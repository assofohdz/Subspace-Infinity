/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package infinity.server.chat;

import com.jme3.network.HostedConnection;
import com.jme3.network.MessageConnection;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import com.simsilica.es.EntityId;
import infinity.net.chat.ChatSession;
import infinity.net.chat.ChatSessionListener;
import infinity.server.AccountHostedService;
import infinity.server.GameSessionHostedService;
import infinity.sim.ChatHostedPoster;
import infinity.sim.CommandBiFunction;
import infinity.sim.CommandFunction;
import infinity.sim.CommandTriFunction;
import infinity.sim.MessageTypes;
import infinity.sim.TriFunction;
import infinity.sim.util.InfinityRunTimeException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HostedService providing a chat server for connected players. Some time during player connection
 * setup, the game must start hosting and provide the player name in order for the client to
 * participate.
 *
 * @author Paul Speed
 */
public class InfinityChatHostedService extends AbstractHostedConnectionService
    implements ChatHostedPoster {

  private static final String PREPEND_CHAT = "chat> ";
  private static final String SYSTEM_MESSAGE_SENDER = "System";
  private static final String NOT_SUPPORTED_YET = "Not supported yet.";
  private static final String ATTRIBUTE_SESSION = "chat.session";
  static Logger log = LoggerFactory.getLogger(InfinityChatHostedService.class);
  private final int channel;
  private final List<ChatSessionImpl> players = new CopyOnWriteArrayList<>();
  // TriConsumers need the player entityId and the avatar entityId
  private final ConcurrentHashMap<Pattern, CommandTriFunction<EntityId, EntityId, Matcher, String>>
      patternTriConsumer;
  // BiConsumers only need the player entityId
  private final ConcurrentHashMap<Pattern, CommandBiFunction<EntityId, Matcher, String>>
      patternBiConsumer;
  private RmiHostedService rmiService;

  /**
   * Creates a new chat service that will use the default reliable channel for reliable
   * communication.
   */
  public InfinityChatHostedService() {
    this(MessageConnection.CHANNEL_DEFAULT_RELIABLE);
  }

  /** Creates a new chat service that will use the specified channel for reliable communication. */
  public InfinityChatHostedService(final int channel) {
    this.channel = channel;
    patternTriConsumer = new ConcurrentHashMap<>();
    patternBiConsumer = new ConcurrentHashMap<>();
    setAutoHost(false);
  }

  protected ChatSessionImpl getChatSession(final HostedConnection conn) {
    return conn.getAttribute(ATTRIBUTE_SESSION);
  }

  @Override
  protected void onInitialize(final HostedServiceManager s) {

    // Grab the RMI service so we can easily use it later
    rmiService = getService(RmiHostedService.class);
    if (rmiService == null) {
      throw new InfinityRunTimeException("ChatHostedService requires an RMI service.");
    }
  }

  /**
   * Starts hosting the chat services on the specified connection using a specified player name.
   * This causes the player to 'enter' the chat room and will then be able to send/receive messages.
   */
  public void startHostingOnConnection(final HostedConnection conn, final String playerName) {
    log.debug("startHostingOnConnection(" + conn + ")");

    final ChatSessionImpl session = new ChatSessionImpl(conn, playerName);
    conn.setAttribute(ATTRIBUTE_SESSION, session);

    // Expose the session as an RMI resource to the client
    final RmiRegistry rmi = rmiService.getRmiRegistry(conn);
    rmi.share((byte) channel, session, ChatSession.class);

    players.add(session);

    // Send the enter event to other players
    for (final ChatSessionImpl chatter : players) {
      if (chatter == session) {
        // Don't send our enter event to ourselves
        continue;
      }
      chatter.playerJoined(conn.getId(), playerName);
    }
    log.info(PREPEND_CHAT + playerName + " joined.");
  }

  /** Starts hosting the chat services on the specified connection using a generated player name. */
  @Override
  public void startHostingOnConnection(final HostedConnection conn) {
    startHostingOnConnection(conn, "Client:" + conn.getId());
  }

  @Override
  public void stopHostingOnConnection(final HostedConnection conn) {
    log.debug("stopHostingOnConnection(" + conn + ")");
    final ChatSessionImpl player = getChatSession(conn);
    if (player != null) {

      // Then we are still hosting on the connection... it's
      // possible that stopHostingOnConnection() is called more than
      // once for a particular connection since some other game code
      // may call it and it will also be called during connection shutdown.
      conn.setAttribute(ATTRIBUTE_SESSION, null);

      // Remove player session from the active sessions list
      players.remove(player);

      // Send the leave event to other players
      for (final ChatSessionImpl chatter : players) {
        if (chatter == player) {
          // Don't send our enter event to ourselves
          continue;
        }
        chatter.playerLeft(player.conn.getId(), player.name);
      }
      log.info(PREPEND_CHAT + player.name + " left.");
    }
  }

  protected void postMessage(final ChatSessionImpl from, final String message) {
    final EntityId fromEntity = AccountHostedService.getPlayerEntity(from.getConn());
    final EntityId fromAvatar = GameSessionHostedService.getAvatarEntity(from.getConn());
    boolean matched = false;

    for (Iterator<Pattern> iterator = patternTriConsumer.keySet().iterator();
        iterator.hasNext(); ) {
      Pattern pattern = iterator.next();
      final Matcher matcher = pattern.matcher(message);
      if (matcher.matches()) {
        matched = true;
        final CommandTriFunction<EntityId, EntityId, Matcher, String> cc =
            patternTriConsumer.get(pattern);
        // TODO: Implement account service to manage security levels
        // if (getService(AccountHostedService.class).isAtLeastAtAccessLevel(fromEntity,
        // cc.getAccessLevelRequired())) {
        TriFunction<EntityId, EntityId, Matcher, String> function = cc.getFunction();
        String response = function.apply(fromEntity, fromAvatar, matcher);
        from.newMessage(from.conn.getId(), from.name, response);
        // }
      }
    }
    if (matched) {
      return;
    }

    // Go through patterns with no arguments
    for (Iterator<Pattern> iterator = patternBiConsumer.keySet().iterator(); iterator.hasNext(); ) {
      Pattern pattern = iterator.next();
      final Matcher matcher = pattern.matcher(message);
      if (matcher.matches()) {
        matched = true;
        // final EntityId fromAvatar = GameSessionHostedService.getAvatarEntity(from.getConn());
        BiFunction<EntityId, Matcher, String> function =
            patternBiConsumer.get(pattern).getConsumer();

        String response = function.apply(fromEntity, matcher);
        from.newMessage(from.conn.getId(), from.name, response);
      }
    }

    if (matched) {
      return;
    }

    log.info(PREPEND_CHAT + from.name + " said:" + message);
    for (final ChatSessionImpl chatter : players) {
      chatter.newMessage(from.conn.getId(), from.name, message);
    }
  }
  // This method doesn't match patterns. It is only called from other modules, not from player
  // clients. Could potentially allow matching to allow modules to chain commands to other modules

  @Override
  public void postPublicMessage(final String from, final int messageType, final String message) {
    log.info(PREPEND_CHAT + from + " said:" + message);
    for (final ChatSessionImpl chatter : players) {
      chatter.newMessage(0, from, message);
    }
  }

  /**
   * @param pattern the pattern to match
   * @param description the help description of the pattern
   * @param c a consumer taking the message and the sender entity id
   */
  @Override
  public void registerPatternTriConsumer(
      final Pattern pattern, final String description, final CommandTriFunction c) {
    // TODO: For now, only one consumer per pattern (we could potentially have
    // multiple)
    patternTriConsumer.put(pattern, c);

    // TODO: Post message only to those who have the proper access level
    postPublicMessage(SYSTEM_MESSAGE_SENDER, MessageTypes.MESSAGE, description);
  }

  /**
   * Registers a pattern to be consumed by a function that takes two arguments. The first argument
   * is the entity id of the sender, and the second argument is the matcher for the pattern.
   *
   * @param pattern the pattern to match
   * @param description the help description of the pattern
   * @param c the function that will be run when the pattern is matched
   */
  public void registerPatternBiConsumer(Pattern pattern, String description, CommandBiFunction c) {
    patternBiConsumer.put(pattern, c);

    postPublicMessage(SYSTEM_MESSAGE_SENDER, MessageTypes.MESSAGE, description);
  }

  /**
   * Removes a pattern to be consumed.
   *
   * @param pattern the pattern to remove comsumption of
   */
  @Override
  public void removePatternConsumer(final Pattern pattern) {
    patternTriConsumer.remove(pattern);
  }

  @Override
  public void postPrivateMessage(
      final String from,
      final int messageType,
      final EntityId targetEntityId,
      final String message) {
    throw new UnsupportedOperationException(
        NOT_SUPPORTED_YET); // To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postTeamMessage(
      final String from, final int messageType, final int targetFrequency, final String message) {
    throw new UnsupportedOperationException(
        NOT_SUPPORTED_YET);
  }

  @Override
  public void registerCommandConsumer(
      final String cmd, final String helptext, final CommandFunction c) {
    // TODO: Put together the pattern that will match, depending on the sender and
    // the command

    // TODO: Post message only to those who have the proper access level
    postPublicMessage(SYSTEM_MESSAGE_SENDER, MessageTypes.MESSAGE, helptext);
  }

  @Override
  public void removeCommandConsumer(final String cmd) {
    throw new UnsupportedOperationException(
        NOT_SUPPORTED_YET);
  }

  /**
   * The connection-specific 'host' for the ChatSession. For convenience this also implements the
   * ChatSessionListener. Since the methods don't collide at all it's convenient for our other code
   * not to have to worry about the internal delegate.
   */
  private class ChatSessionImpl implements ChatSession, ChatSessionListener {

    private final HostedConnection conn;
    private final String name;
    private ChatSessionListener callback;

    public ChatSessionImpl(final HostedConnection conn, final String name) {
      this.conn = conn;
      this.name = name;

      // Note: at this point we won't be able to look up the callback
      // because we haven't received the client's RMI shared objects yet.
    }

    protected HostedConnection getConn() {
      return conn;
    }

    protected ChatSessionListener getCallback() {
      if (callback == null) {
        final RmiRegistry rmi = rmiService.getRmiRegistry(conn);
        callback = rmi.getRemoteObject(ChatSessionListener.class);
        if (callback == null) {
          throw new InfinityRunTimeException(
              "Unable to locate client callback for ChatSessionListener");
        }
      }
      return callback;
    }

    @Override
    public void sendMessage(final String message) {
      postMessage(this, message);
    }

    @Override
    public List<String> getPlayerNames() {
      final List<String> results = new ArrayList<>();
      for (final ChatSessionImpl chatter : players) {
        results.add(chatter.name);
      }
      return results;
    }

    @Override
    public void playerJoined(final int clientId, final String playerName) {
      getCallback().playerJoined(clientId, playerName);
    }

    @Override
    public void newMessage(final int clientId, final String playerName, final String message) {
      getCallback().newMessage(clientId, playerName, message);
    }

    @Override
    public void playerLeft(final int clientId, final String playerName) {
      getCallback().playerLeft(clientId, playerName);
    }
  }
}
