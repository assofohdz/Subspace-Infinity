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

package infinity.client;

import java.io.IOException;

import com.simsilica.bpos.net.SharedObjectUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.service.ClientService;
import com.jme3.network.service.rmi.RmiClientService;
import com.jme3.network.service.rpc.RpcClientService;

import com.simsilica.es.EntityData;
import com.simsilica.es.client.EntityDataClientService;
import com.simsilica.ethereal.EtherealClient;
import com.simsilica.ethereal.TimeSource;
import com.simsilica.mworld.net.client.WorldClientService;

import infinity.InfinityConstants;
import infinity.client.chat.ChatClientService;

/**
 * @author Paul Speed
 */
public class GameClient {

  static Logger log = LoggerFactory.getLogger(GameClient.class);

  private final Client client;
  private final EntityData ed;

  public GameClient(final String host, final int port) throws IOException {
    log.info("Connecting to:" + host + " " + port);
    client =
        Network.connectToServer(
            InfinityConstants.NAME, InfinityConstants.PROTOCOL_VERSION, host, port);

    // client.addMessageListener(new MessageDebugger());

    log.info("Adding services...");
    client
        .getServices()
        .addServices(
            new RpcClientService(),
            new RmiClientService(),
            new AccountClientService(),
            new GameSessionClientService(),
            new EntityDataClientService(InfinityConstants.ES_CHANNEL),
            new ChatClientService(InfinityConstants.CHAT_CHANNEL),
            new WorldClientService(InfinityConstants.TERRAIN_CHANNEL),
            new EtherealClient(
                InfinityConstants.OBJECT_PROTOCOL,
                InfinityConstants.ZONE_GRID,
                InfinityConstants.ZONE_RADIUS),
            new SharedObjectUpdater());

    // Can grab this even before started but you won't be able to retrieve
    // entities until the connection has been fully setup.
    ed = client.getServices().getService(EntityDataClientService.class).getEntityData();
  }

  public TimeSource getTimeSource() {
    return client.getServices().getService(EtherealClient.class).getTimeSource();
  }

  public Client getClient() {
    return client;
  }

  public EntityData getEntityData() {
    return ed;
  }

  public void start() {
    log.info("start()");
    client.start();
  }

  public void addService(final ClientService service) {
    client.getServices().addService(service);
  }

  public void removeService(final ClientService service) {
    client.getServices().removeService(service);
  }

  public <T extends ClientService> T getService(final Class<T> type) {
    return client.getServices().getService(type);
  }

  public void close() {
    log.info("close()");
    if (client.isStarted()) {
      client.close();
    }
  }

  @SuppressWarnings("unused")
  private class MessageDebugger implements MessageListener<Client> {
    Logger debugLog = LoggerFactory.getLogger("diagnostics.MessageDebugger");
    private boolean objectStateStarted = false;

    public MessageDebugger() {}

    @Override
    public void messageReceived(final Client source, final Message m) {
      if (m instanceof com.simsilica.ethereal.net.ObjectStateMessage) {
        if (!objectStateStarted) {
          debugLog.info("Object state updates started.");
          objectStateStarted = true;
        }
      } else if (debugLog.isInfoEnabled()) {
        debugLog.info("Received:" + m);
      }
    }
  }
}
