/*
 * $Id$
 *
 * Copyright (c) 2018, Simsilica, LLC
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

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.plugins.AWTLoader;
import com.simsilica.builder.BuilderState;
import com.simsilica.es.EntityId;
import com.simsilica.ethereal.TimeSource;
import com.simsilica.mworld.view.ProgressState;
import com.simsilica.state.BlackboardState;
import com.simsilica.state.CompositeAppState;
import com.simsilica.state.DebugHudState;
import com.simsilica.state.GameSystemsState;
import infinity.HelpState;
import infinity.HostState;
import infinity.InfinityConstants;
import infinity.SettingsState;
import infinity.TimeState;
import infinity.ai.MobDebugState;
import infinity.client.audio.AudioState;
import infinity.client.audio.SIAudioFactory;
import infinity.client.states.HudLabelState;
import infinity.client.states.InfinityCameraState;
import infinity.client.states.LightState;
import infinity.client.states.LocalViewState;
import infinity.client.states.MapState;
import infinity.client.states.ModelViewState;
import infinity.client.states.PhysicsDebugState;
import infinity.client.states.SpaceGridState;
import infinity.client.view.SkyState;

/**
 * The main game session state.  This is the state that is active
 * when the player is in a game session.  It is responsible for
 * managing the various sub-states that are active during a game
 * session.
 *
 * @author Asser Fahrenholz
 */
public class GameSessionState extends CompositeAppState {

  private EntityId avatarEntityId;

  /**
   * Creates a new GameSessionState.
   */
  public GameSessionState() {
    super(
        new GameSystemsState(),
        new AvatarMovementState(),
        new TimeState(), // Has to be before any visuals that might need it.
        new SkyState(),
        new BuilderState(4, 4),
        new LocalViewState(),
        new ModelViewState(),
        new AudioState(new SIAudioFactory()),
        new SpaceGridState(InfinityConstants.GRID_CELL_SIZE, 2, new ColorRGBA(0.8f, 1f, 1f, 0.5f)),
        new LightState(),
        new ProgressState(),
        new BlackboardState(),
        new DebugHudState()
        );

    addChild(new HelpState(), true);
    //addChild(new SettingsState(), true);
    addChild(new ChatState(), true);
    addChild(new MapState(), true);
    addChild(new HudLabelState(), true);
  }

  @Override
  protected void initialize(final Application app) {
    avatarEntityId =
        getState(ConnectionState.class).getService(GameSessionClientService.class).getAvatar();
    // See if this is local host mode. This stuff should maybe be moved
    // to its own debug manager state.
    final HostState host = getState(HostState.class);
    if (host != null) {
      addChild(new PhysicsDebugState(host), true);
    }
    if (host != null) {
      addChild(new MobDebugState(host), true);
    }

    TimeSource timeSource = getState(ConnectionState.class).getRemoteTimeSource();

    getState(TimeState.class).setTimeSource(timeSource);

    InfinityCameraState cameraState = new InfinityCameraState(avatarEntityId, timeSource);
    addChild(cameraState);


    getApplication().getAssetManager().registerLoader(AWTLoader.class, "bm2");
  }

  @Override
  protected void cleanup(final Application app) {
    // Auto-generated method stub
  }

  @Override
  protected void onEnable() {
    // Auto-generated method stub
  }

  @Override
  public void update(float tpf) {
    // Auto-generated method stub
  }

  @Override
  protected void onDisable() {
    // Auto-generated method stub
  }

  //A method to get the avatar entity id
  public EntityId getAvatarEntityId() {
    return avatarEntityId;
  }
}
