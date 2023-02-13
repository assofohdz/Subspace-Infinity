/*
 * Copyright (c) 2018, Asser Fahrenholz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package infinity.modules.prizeTester;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.mathd.Vec3d;
import infinity.sim.AccessLevel;
import infinity.sim.AccountManager;
import infinity.sim.AdaptiveLoader;
import infinity.sim.ArenaManager;
import infinity.sim.BaseGameModule;
import infinity.sim.ChatHostedPoster;
import infinity.sim.CommandTriConsumer;
import infinity.sim.GameEntities;
import infinity.sim.PhysicsManager;
import infinity.sim.TimeManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for testing the prize spawner.
 *
 * @author Asser
 */
public class prizeTester extends BaseGameModule {

  static Logger log = LoggerFactory.getLogger(prizeTester.class);
  private final Pattern prizeTesterCommand = Pattern.compile("\\~prizeTester\\s(\\w+)");
  private EntityData ed;
  private Ini settings;

  public prizeTester(
      final ChatHostedPoster chp,
      final AccountManager am,
      final AdaptiveLoader loader,
      final ArenaManager arenas,
      final TimeManager time,
      final PhysicsManager physics) {
    super(chp, am, loader, arenas, time, physics);
  }

  @Override
  protected void initialize() {
    ed = getSystem(EntityData.class);
    settings = new Ini();
    try {
      InputStream is =
          prizeTester.class.getResourceAsStream(this.getClass().getSimpleName() + ".ini");
      settings = new Ini(is);
    } catch (final IOException ex) {
      java.util.logging.Logger.getLogger(prizeTester.class.getName()).log(Level.SEVERE, null, ex);
    }

    GameEntities.createWeightedPrizeSpawner(
        ed,
        EntityId.NULL_ID,
        getPhysicsManager().getPhysics(),
        getTimeManager().getTime(),
        new Vec3d(0, 1, 0),
        1000,
        true,
        10);
  }

  @Override
  protected void terminate() {
    // TODO Auto-generated method stub
  }

  @Override
  public void start() {
    // EventBus.addListener(this, ShipEvent.shipDestroyed, ShipEvent.shipSpawned);
    //
    getChp()
        .registerPatternTriConsumer(
            prizeTesterCommand,
            "The command to make this prizeTester do stuff is ~prizeTester <command>, "
                + "where <command> is the command you want to execute",
            new CommandTriConsumer<>(AccessLevel.PLAYER_LEVEL, this::messageHandler));

    // startGame();
  }

  @Override
  public void stop() {
    // EventBus.removeListener(this, ShipEvent.shipDestroyed,
    // ShipEvent.shipSpawned);
    // endGame();
  }

  /**
   * Handle the message events.
   *
   * @param id The entity id of the sender
   * @param s The message to handle
   */
  public void messageHandler(final EntityId id, EntityId id2, final String s) {
    log.info("Received command" + s);
  }
}
