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

package infinity.systems;

import com.simsilica.es.ComponentFilter;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.ext.mphys.ShapeInfo;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import infinity.es.ShapeNames;
import infinity.es.arena.ArenaId;
import infinity.server.AssetLoaderService;
import infinity.settings.IniLoader;
import infinity.settings.SSSLoader;
import infinity.settings.SettingListener;
import infinity.sim.CoreGameConstants;
import infinity.sim.util.InfinityRunTimeException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This state loads the settings for all arenas and notifies all listeners of the new settings.
 * Listeners are meant to keep a local copy of the settings so as not to reference this state every
 * time they have use for a setting
 *
 * <p>Every setting will be stored per ship in the entitydata, but this state will load the settings
 * and can update ship settings when requested (for example when a ship enters a new arena)
 *
 * @author Asser Fahrenholz
 */
public class SettingsSystem extends AbstractGameSystem {

  private static final String BULLET_GROUP = "Bullet";
  private static final String BOMB_GROUP = "Bomb";
  private static final String MISC_GROUP = "Misc";
  private static final String TEAM_GROUP = "Team";
  private static final String REPEL_GROUP = "Repel";
  private static final String MINE_GROUP = "Mine";
  private static final String BURST_GROUP = "Burst";
  private static final String FLAG_GROUP = "Flag";
  private static final String ROCKET_GROUP = "Rocket";
  private static final String SHRAPNEL_GROUP = "Shrapnel";
  private static final String WORMHOLE_GROUP = "Wormhole";
  private static final String KILL_GROUP = "Kill";
  private static final String PRIZE_GROUP = "Prize";
  private static final String LATENCY_GROUP = "Latency";
  private static final String DOOR_GROUP = "Door";
  private static final String TOGGLE_GROUP = "Toggle";
  private static final String BRICK_GROUP = "Brick";
  private static final String RADAR_GROUP = "Radar";
  private static final String SOCCER_GROUP = "Soccer";
  private static final String UNUSED_GROUP = "Unused";
  private static final String MESSAGE_GROUP = "Message";
  private static final String CHAT_GROUP = "Chat";
  private static final String ZONE_FOLDER = "zone";
  private static final String ARENA_FOLDER = "arenas";
  private static final String DEFAULT_ARENA_FOLDER = "(default)";
  private static final String ARENA_CONFIG_FILE = "arena.conf";
  static Logger log = LoggerFactory.getLogger(SettingsSystem.class);
  private final HashMap<String, Ini> arenaSettingsMap = new HashMap<>();
  private final List<SimpleEntry<String, String>> intSettings =
      Arrays.asList(
          new SimpleEntry<>(BULLET_GROUP, "BulletDamageLevel"),
          new SimpleEntry<>(BOMB_GROUP, "BombDamageLevel"),
          new SimpleEntry<>(BULLET_GROUP, "BulletAliveTime"),
          new SimpleEntry<>(BOMB_GROUP, "BombAliveTime"),
          new SimpleEntry<>(MISC_GROUP, "DecoyAliveTime"),
          new SimpleEntry<>(MISC_GROUP, "SafetyLimit"),
          new SimpleEntry<>(MISC_GROUP, "FrequencyShift"),
          new SimpleEntry<>(TEAM_GROUP, "MaxFrequency"),
          new SimpleEntry<>(REPEL_GROUP, "RepelSpeed"),
          new SimpleEntry<>(MINE_GROUP, "MineAliveTime"),
          new SimpleEntry<>(BURST_GROUP, "BurstDamageLevel"),
          new SimpleEntry<>(BULLET_GROUP, "BulletDamageUpgrade"),
          new SimpleEntry<>(FLAG_GROUP, "FlagDropDelay"),
          new SimpleEntry<>(FLAG_GROUP, "EnterGameFlaggingDelay"),
          new SimpleEntry<>(ROCKET_GROUP, "RocketThrust"),
          new SimpleEntry<>(ROCKET_GROUP, "RocketSpeed"),
          new SimpleEntry<>(SHRAPNEL_GROUP, "InactiveShrapDamage"),
          new SimpleEntry<>(WORMHOLE_GROUP, "SwitchTime"),
          new SimpleEntry<>(MISC_GROUP, "ActivateAppShutdownTime"),
          new SimpleEntry<>(SHRAPNEL_GROUP, "ShrapnelSpeed"));
  private final List<SimpleEntry<String, String>> shortSettings =
      Arrays.asList(
          new SimpleEntry<>(LATENCY_GROUP, "SendRoutePercent"),
          new SimpleEntry<>(BOMB_GROUP, "BombExplodeDelay"),
          new SimpleEntry<>(MISC_GROUP, "SendPositionDelay"),
          new SimpleEntry<>(BOMB_GROUP, "BombExplodePixels"),
          new SimpleEntry<>(PRIZE_GROUP, "DeathPrizeTime"),
          new SimpleEntry<>(BOMB_GROUP, "JitterTime"),
          new SimpleEntry<>(KILL_GROUP, "EnterDelay"),
          new SimpleEntry<>(PRIZE_GROUP, "EngineShutdownTime"),
          new SimpleEntry<>(BOMB_GROUP, "ProximityDistance"),
          new SimpleEntry<>(KILL_GROUP, "BountyIncreaseForKill"),
          new SimpleEntry<>(MISC_GROUP, "BounceFactor"),
          new SimpleEntry<>(RADAR_GROUP, "MapZoomFactor"),
          new SimpleEntry<>(KILL_GROUP, "MaxBonus"),
          new SimpleEntry<>(KILL_GROUP, "MaxPenalty"),
          new SimpleEntry<>(KILL_GROUP, "RewardBase"),
          new SimpleEntry<>(REPEL_GROUP, "RepelTime"),
          new SimpleEntry<>(REPEL_GROUP, "RepelDistance"),
          new SimpleEntry<>(MISC_GROUP, "TickerDelay"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerOnRadar"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerKillMultiplier"),
          new SimpleEntry<>(PRIZE_GROUP, "PrizeFactor"),
          new SimpleEntry<>(PRIZE_GROUP, "PrizeDelay"),
          new SimpleEntry<>(PRIZE_GROUP, "MinimumVirtual"),
          new SimpleEntry<>(PRIZE_GROUP, "UpgradeVirtual"),
          new SimpleEntry<>(PRIZE_GROUP, "PrizeMaxExist"),
          new SimpleEntry<>(PRIZE_GROUP, "PrizeMinExist"),
          new SimpleEntry<>(PRIZE_GROUP, "PrizeNegativeFactor"),
          new SimpleEntry<>(DOOR_GROUP, "DoorDelay"),
          new SimpleEntry<>(TOGGLE_GROUP, "AntiWarpPixels"),
          new SimpleEntry<>(DOOR_GROUP, "DoorMode"),
          new SimpleEntry<>(FLAG_GROUP, "FlagBlankDelay"),
          new SimpleEntry<>(FLAG_GROUP, "NoDataFlagDropDelay"),
          new SimpleEntry<>(PRIZE_GROUP, "MultiPrizeCount"),
          new SimpleEntry<>(BRICK_GROUP, "BrickTime"),
          new SimpleEntry<>(MISC_GROUP, "WarpRadiusLimit"),
          new SimpleEntry<>(BOMB_GROUP, "EBombShutdownTime"),
          new SimpleEntry<>(BOMB_GROUP, "EBombDamagePercent"),
          new SimpleEntry<>(RADAR_GROUP, "RadarNeutralSize"),
          new SimpleEntry<>(MISC_GROUP, "WarpPointDelay"),
          new SimpleEntry<>(MISC_GROUP, "NearDeathLevel"),
          new SimpleEntry<>(BOMB_GROUP, "BBombDamagePercent"),
          new SimpleEntry<>(SHRAPNEL_GROUP, "ShrapnelDamagePercent"),
          new SimpleEntry<>(LATENCY_GROUP, "ClientSlowPacketTime"),
          new SimpleEntry<>(FLAG_GROUP, "FlagDropResetReward"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerFireCostPercent"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerDamagePercent"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerBombFireDelay"),
          new SimpleEntry<>(SOCCER_GROUP, "PassDelay"),
          new SimpleEntry<>(SOCCER_GROUP, "BallBlankDelay"),
          new SimpleEntry<>(LATENCY_GROUP, "S2CNoDataKickoutDelay"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerThrustAdjustment"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerSpeedAdjustment"),
          new SimpleEntry<>(LATENCY_GROUP, "ClientSlowPacketSampleSize"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused5"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused4"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused3"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused2"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused1"));
  private final List<SimpleEntry<String, String>> byteSettings =
      Arrays.asList(
          new SimpleEntry<>(SHRAPNEL_GROUP, "Random"),
          new SimpleEntry<>(SOCCER_GROUP, "BallBounce"),
          new SimpleEntry<>(SOCCER_GROUP, "AllowBombs"),
          new SimpleEntry<>(SOCCER_GROUP, "AllowGuns"),
          new SimpleEntry<>(SOCCER_GROUP, "Mode"),
          new SimpleEntry<>(TEAM_GROUP, "MaxPerTeam"),
          new SimpleEntry<>(TEAM_GROUP, "MaxPerPrivateTeam"),
          new SimpleEntry<>(MINE_GROUP, "TeamMaxMines"),
          new SimpleEntry<>(WORMHOLE_GROUP, "GravityBombs"),
          new SimpleEntry<>(BOMB_GROUP, "BombSafety"),
          new SimpleEntry<>(CHAT_GROUP, "MessageReliable"),
          new SimpleEntry<>(PRIZE_GROUP, "TakePrizeReliable"),
          new SimpleEntry<>(MESSAGE_GROUP, "AllowAudioMessages"),
          new SimpleEntry<>(PRIZE_GROUP, "PrizeHideCount"),
          new SimpleEntry<>(MISC_GROUP, "ExtraPositionData"),
          new SimpleEntry<>(MISC_GROUP, "SlowFrameCheck"),
          new SimpleEntry<>(FLAG_GROUP, "CarryFlags"),
          new SimpleEntry<>(MISC_GROUP, "AllowSavedShips"),
          new SimpleEntry<>(RADAR_GROUP, "RadarMode"),
          new SimpleEntry<>(MISC_GROUP, "VictoryMusic"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerGunUpgrade"),
          new SimpleEntry<>(FLAG_GROUP, "FlaggerBombUpgrade"),
          new SimpleEntry<>(SOCCER_GROUP, "UseFlagger"),
          new SimpleEntry<>(SOCCER_GROUP, "BallLocation"),
          new SimpleEntry<>(MISC_GROUP, "AntiWarpSettleDelay"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused7"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused6"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused5"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused4"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused3"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused2"),
          new SimpleEntry<>(UNUSED_GROUP, "Unused1"));
  private final List<String> shipByteSettings =
      Arrays.asList(
          "TurretLimit",
          "BurstShrapnel",
          "MaxMines",
          "RepelMax",
          "BurstMax",
          "DecoyMax",
          "ThorMax",
          "BrickMax",
          "RocketMax",
          "PortalMax",
          "InitialRepel",
          "InitialBurst",
          "InitialBrick",
          "InitialRocket",
          "InitialThor",
          "InitialDecoy",
          "InitialPortal",
          "BombBounceCount");
  private final List<String> shipShortSettings =
      Arrays.asList(
          "Gravity",
          "GravityTopSpeed",
          "BulletFireEnergy",
          "MultiFireEnergy",
          "BombFireEnergy",
          "BombFireEnergyUpgrade",
          "LandmineFireEnergy",
          "LandmineFireEnergyUpgrade",
          "BulletSpeed",
          "BombSpeed",
          "___MiscBitfield___",
          "MultiFireAngle",
          "CloakEnergy",
          "StealthEnergy",
          "AntiWarpEnergy",
          "XRadarEnergy",
          "MaximumRotation",
          "MaximumThrust",
          "MaximumSpeed",
          "MaximumRecharge",
          "MaximumEnergy",
          "InitialRotation",
          "InitialThrust",
          "InitialSpeed",
          "InitialRecharge",
          "InitialEnergy",
          "UpgradeRotation",
          "UpgradeThrust",
          "UpgradeSpeed",
          "UpgradeRecharge",
          "UpgradeEnergy",
          "AfterburnerEnergy",
          "BombThrust",
          "BurstSpeed",
          "TurretThrustPenalty",
          "TurretSpeedPenalty",
          "BulletFireDelay",
          "MultiFireDelay",
          "BombFireDelay",
          "LandmineFireDelay",
          "RocketTime",
          "InitialBounty",
          "DamageFactor",
          "PrizeShareLimit",
          "AttachBounty",
          "SoccerThrowTime",
          "SoccerBallFriction",
          "SoccerBallProximity",
          "SoccerBallSpeed");
  private final List<String> shipIntSettings = Arrays.asList("SuperTime", "ShieldsTime");
  ArrayList<SettingListener> listeners = new ArrayList<>();
  private AssetLoaderService assetLoader;
  private EntityData ed;
  private double timeSinceLastSettingsUpdate_ms = 0;


  public void addListener(final SettingListener listener) {
    listeners.add(listener);
  }

  public void removeListener(final SettingListener listener) {
    listeners.remove(listener);
  }

  @Override
  protected void initialize() {

    this.assetLoader = getSystem(AssetLoaderService.class);

    ed = getSystem(EntityData.class);

    assetLoader.registerLoader(IniLoader.class, "ini");
    assetLoader.registerLoader(IniLoader.class, "cfg");
    assetLoader.registerLoader(IniLoader.class, "conf");

    assetLoader.registerLoader(SSSLoader.class, "sss");
    assetLoader.registerLoader(SSSLoader.class, "set");

  }

  @Override
  protected void terminate() {
    // Nothing to do
  }

  @Override
  public void update(final SimTime tpf) {

  }

  @Override
  public void start() {
    // Nothing to do here
  }

  @Override
  public void stop() {
    // Nothing to do here
  }

  private void updateShipSettings(EntityId entityId, Ini settings) {
    ShapeInfo shapeInfo = ed.getComponent(entityId, ShapeInfo.class);

    if (shapeInfo == null) {
      throw new InfinityRunTimeException("Entity " + entityId + " has no ShapeInfo component");
    }

    String section = "";
    switch (shapeInfo.getShapeName(ed)) {
      case ShapeNames.SHIP_JAVELIN:
        section = "Javelin";
        break;
      case ShapeNames.SHIP_WARBIRD:
        section = "Warbird";
        break;
      case ShapeNames.SHIP_LEVI:
        section = "Leviathan";
        break;
      case ShapeNames.SHIP_TERRIER:
        section = "Terrier";
        break;
      case ShapeNames.SHIP_LANCASTER:
        section = "Lancaster";
        break;
      case ShapeNames.SHIP_SHARK:
        section = "Shark";
        break;
      case ShapeNames.SHIP_SPIDER:
        section = "Spider";
        break;
      case ShapeNames.SHIP_WEASEL:
        section = "Weasel";
        break;
      default:
        // If the entity is not a ship, do nothing
        return;
    }

    Section sec = settings.get(section);
  }

  /**
   * Called when some setting change and listeners has to update their local copy of the settings.
   *
   * @param arenaId the arena to lookup settings for
   * @param section the section of the setting
   * @param setting the setting to retrieve
   */
  private void settingChanged(final ArenaId arenaId, final String section, final String setting) {
    for (final SettingListener listener : listeners) {
      listener.arenaSettingsChange(arenaId, section, setting);
    }
  }

  /**
   * This method is called to load a specific arena. It will load the config-file corresponding to
   * that arena (for now its just one file containing all the config).
   *
   * @param requester The entity that requested the settings
   * @param map The id of the map to load settings for
   */
  public void loadSettings(EntityId requester, String map) {

    // Load the settings from file
    // Example: /arenas/trench/trench.conf
    // (we already load the folder "zone" as resource folder, so not including it in the call here
    Ini settings =
        (Ini) assetLoader.loadAsset("/" + ARENA_FOLDER + "/" + map + "/" + ARENA_CONFIG_FILE);

    if (settings == null) {
      log.warn("Settings file not found for map {}, loading default map instead", map);
      if (arenaSettingsMap.get(DEFAULT_ARENA_FOLDER) != null) {
        arenaSettingsMap.put(map, arenaSettingsMap.get(DEFAULT_ARENA_FOLDER));
      } else {
        Ini defaultSettings =
            (Ini)
                assetLoader.loadAsset(
                    "/" + ARENA_FOLDER + "/" + DEFAULT_ARENA_FOLDER + "/" + ARENA_CONFIG_FILE);

        arenaSettingsMap.put(map, defaultSettings);
      }
    } else {
      arenaSettingsMap.put(map, settings);
    }
  }

  /**
   * Method to load settings if we only know the arena entity id and not the specific map name.
   *
   * @param requester The entity that requested the settings
   * @param arenaEntityId The arena entity id that holds informatio on the map
   */
  public void loadSettings(EntityId requester, EntityId arenaEntityId) {
    Entity arena = ed.getEntity(arenaEntityId);
    if (arena == null) {
      throw new InfinityRunTimeException("Arena entity " + arenaEntityId + " does not exist");
    }

    ArenaId arenaId = arena.get(ArenaId.class);
    if (arenaId == null) {
      throw new InfinityRunTimeException(
          "Arena entity " + arenaEntityId + " has no ArenaId component");
    }

    // Load the settings from file
    loadSettings(requester, arenaId.getArena());
  }

  public Ini getIni(String map) {
    return arenaSettingsMap.get(map);
  }

  /*
   * Notes:SettingName:::Name of the Game the settings "create"
   * Notes:Maker:::Creator of the settings Notes:CoMaker:::Original and/or helper
   * of the zone Notes:MapName:::Map name(s) used with settings Notes:Mapper:::Map
   * Maker's name Notes:Note1:::Any other misc notes you wish to add
   * Notes:Note2:::Any other misc notes you wish to add Notes:Note3:::Any other
   * misc notes you wish to add Notes:Note4:::Any other misc notes you wish to add
   * Notes:Note5:::Any other misc notes you wish to add
   *
   * Bomb:BombDamageLevel:::Amount of damage a bomb causes at its center point
   * (for all bomb levels) Bomb:BombAliveTime:::Time bomb is alive (in hundredths
   * of a second) Bomb:BombExplodeDelay:::How long after the proximity sensor is
   * triggered before bomb explodes. (note: it explodes immediately if ship moves
   * away from it after triggering it) Bomb:BombExplodePixels:::Blast radius in
   * pixels for an L1 bomb (L2 bombs double this, L3 bombs triple this)
   * Bomb:ProximityDistance:::Radius of proximity trigger in tiles. Each bomb
   * level adds 1 to this amount. Bomb:JitterTime:::How long the screen jitters
   * from a bomb hit. (in hundredths of a second) Bomb:BombSafety:0:1:Whether
   * proximity bombs have a firing safety (0=no 1=yes). If enemy ship is within
   * proximity radius, will it allow you to fire. Bomb:EBombShutdownTime:::Maximum
   * time recharge is stopped on players hit with an EMP bomb.
   * Bomb:EBombDamagePercent:::Percentage of normal damage applied to an EMP bomb
   * 0=0% 1000=100% 2000=200% Bomb:BBombDamagePercent:::Percentage of normal
   * damage applied to a bouncing bomb 0=0% 1000=100% 2000=200%
   *
   * Brick:BrickTime:::How long bricks last (in hundredths of a second)
   * Brick:BrickSpan:::How many tiles bricks are able to span
   *
   * Bullet:BulletDamageLevel:::Maximum amount of damage that a L1 bullet will
   * cause. Formula; damage = squareroot(rand# * (max damage^2 + 1))
   * Bullet:BulletDamageUpgrade:::Amount of extra damage each bullet level will
   * cause Bullet:BulletAliveTime:::How long bullets live before disappearing (in
   * hundredths of a second) Bullet:ExactDamage:0:1:If damage is to be random or
   * not (1=exact, 0=random)
   *
   * Burst:BurstDamageLevel:::Maximum amount of damage caused by a single burst
   * bullet.
   *
   * Cost:PurchaseAnytime:0:1:Where prizes can be purchased 0 = safe zone, 1 =
   * anywhere Cost:Recharge:::Cost (in points) to purchase this prize
   * Cost:Energy:::Cost (in points) to purchase this prize Cost:Rotation:::Cost
   * (in points) to purchase this prize Cost:Stealth:::Cost (in points) to
   * purchase this prize Cost:Cloak:::Cost (in points) to purchase this prize
   * Cost:XRadar:::Cost (in points) to purchase this prize Cost:Gun:::Cost (in
   * points) to purchase this prize Cost:Bomb:::Cost (in points) to purchase this
   * prize Cost:Bounce:::Cost (in points) to purchase this prize
   * Cost:Thrust:::Cost (in points) to purchase this prize Cost:Speed:::Cost (in
   * points) to purchase this prize Cost:MultiFire:::Cost (in points) to purchase
   * this prize Cost:Prox:::Cost (in points) to purchase this prize
   * Cost:Super:::Cost (in points) to purchase this prize Cost:Shield:::Cost (in
   * points) to purchase this prize Cost:Shrap:::Cost (in points) to purchase this
   * prize Cost:AntiWarp:::Cost (in points) to purchase this prize
   * Cost:Repel:::Cost (in points) to purchase this prize Cost:Burst:::Cost (in
   * points) to purchase this prize Cost:Decoy:::Cost (in points) to purchase this
   * prize Cost:Thor:::Cost (in points) to purchase this prize Cost:Brick:::Cost
   * (in points) to purchase this prize Cost:Rocket:::Cost (in points) to purchase
   * this prize Cost:Portal:::Cost (in points) to purchase this prize
   *
   * Custom:SaveStatsTime:100000:100000000:How often a custom arena saves its
   * scores to the hard drive (in case something goes wrong)
   *
   * Door:DoorDelay:::How often doors attempt to switch their state.
   * Door:DoorMode:::Door mode (-2=all doors completely random, -1=weighted random
   * (some doors open more often than others), 0-255=fixed doors (1 bit of byte
   * for each door specifying whether it is open or not)
   *
   * Flag:FlaggerOnRadar:::Whether the flaggers appear on radar in red 0=no 1=yes
   * Flag:FlaggerKillMultiplier:::Number of times more points are given to a
   * flagger (1 = double points, 2 = triple points)
   * Flag:FlaggerGunUpgrade:0:1:Whether the flaggers get a gun upgrade 0=no 1=yes
   * Flag:FlaggerBombUpgrade:0:1:Whether the flaggers get a bomb upgrade 0=no
   * 1=yes Flag:FlaggerFireCostPercent:::Percentage of normal weapon firing cost
   * for flaggers 0=Super 1000=100% 2000=200%
   * Flag:FlaggerDamagePercent:::Percentage of normal damage received by flaggers
   * 0=Invincible 1000=100% 2000=200% Flag:FlaggerSpeedAdjustment:::Amount of
   * speed adjustment player carrying flag gets (negative numbers mean slower)
   * Flag:FlaggerThrustAdjustment:::Amount of thrust adjustment player carrying
   * flag gets (negative numbers mean less thrust)
   * Flag:FlaggerBombFireDelay:::Delay given to flaggers for firing bombs (0=ships
   * normal firing rate -- note: please do not set this number less than 20)
   * Flag:CarryFlags:0:2:Whether the flags can be picked up and carried (0=no,
   * 1=yes, 2=yes-one at a time) Flag:FlagDropDelay:::Time before flag is dropped
   * by carrier (0=never) Flag:FlagDropResetReward:::Minimum kill reward that a
   * player must get in order to have his flag drop timer reset.
   * Flag:EnterGameFlaggingDelay:::Time a new player must wait before they are
   * allowed to see flags Flag:FlagBlankDelay:::Amount of time that a user can get
   * no data from server before flags are hidden from view for 10 seconds.
   * Flag:NoDataFlagDropDelay:::Amount of time that a user can get no data from
   * server before flags he is carrying are dropped. Flag:FlagMode:0:2:Style of
   * flag game (0=dropped flags are un-owned, carry all flags to win)(1=dropped
   * flags are owned, own all flags to win)(2=Turf style flag game)
   * Flag:FlagResetDelay:::Amount of time before an un-won flag game is reset (in
   * hundredths of a second) Advisable to be over 1000000
   * Flag:MaxFlags:0:32:Maximum number of flags in the arena. (0=no flag game)
   * Flag:RandomFlags:0:1:Whether the actual number of flags is randomly picked up
   * to MaxFlags (0=no 1=yes) Flag:FlagReward:::Number of points given for a flag
   * victory. Formula = (playersInGame * playersInGame * FlagReward / 1000). (0=no
   * flag victory is EVER declared) Flag:FlagRewardMode:0:1:How flag reward points
   * are divided up (0 = each team member gets rewardPoints) (1 = each team member
   * gets (rewardPoints * numberOfTeamMembers / maximumAllowedPerTeam))
   * Flag:FlagTerritoryRadius:::When flagger drops flags, this is how spread out
   * they are (distance from drop-centroid in tiles). (note: 0 = special value
   * meaning hide flags in center area of board as normal)
   * Flag:FlagTerritoryRadiusCentroid:::When flagger drops flags, this is how far
   * the drop-centroid is randomly adjusted from the actual drop location) (note:
   * 1024 = hide anywhere on level) Flag:FriendlyTransfer:0:1:Whether the flaggers
   * can transfer flags to other teammates (0=no 1=yes)
   *
   * Kill:MaxBonus:::Let's ignore these for now. Or let's not. :) This is if you
   * have flags, can add more points per a kill. Founded by MGB
   * Kill:MaxPenalty:::Let's ignore these for now. Or let's not. :) This is if you
   * have flags, can take away points per a kill. Founded by MGB
   * Kill:RewardBase:::Let's ignore these for now. Or let's not. :) This is shown
   * added to a person's bty, but isn't added from points for a kill. Founded by
   * MGB Kill:BountyIncreaseForKill:::Number of points added to players bounty
   * each time he kills an opponent. Kill:EnterDelay:::How long after a player
   * dies before he can re-enter the game. Kill:KillPointsPerFlag:::Number of
   * bonus points given to a player based on the number of flags his team has (as
   * is done in Turf zone now) Kill:KillPointsMinimumBounty:::Bounty of target
   * must be over this value to get any KillPointsPerFlag bonus points.
   * Kill:DebtKills:::Number of kills a player must get after dying or
   * resetting-ship before he starts getting points for kills. (0 = Normal)
   * Kill:NoRewardKillDelay:::If you kill the same guy twice within this amount of
   * time, you get no points for the second kill. (in hundredths of a second)
   * Kill:BountyRewardPercent:::Percentage of your own bounty added to your reward
   * when you kill somebody else. Kill:FixedKillReward:::Fixed number of points
   * given for any kill (regardless of bounty) (-1 = use bounty as always)
   * Kill:JackpotBountyPercent:::Percentage of kill value added to Jackpot 0=No
   * Jackpot Game 1000=100% 2000=200%
   *
   * King:DeathCount:::Number of deaths a player is allowed until his crown is
   * removed King:ExpireTime:::Initial time given to each player at beginning of
   * 'King of the Hill' round King:RewardFactor:::Number of points given to winner
   * of 'King of the Hill' round (uses FlagReward formula)
   * King:NonCrownAdjustTime:::Amount of time added for killing a player without a
   * crown King:NonCrownMinimumBounty:::Minimum amount of bounty a player must
   * have in order to receive the extra time. King:CrownRecoverKills:::Number of
   * crown kills a non-crown player must get in order to get their crown back.
   *
   * Latency:SendRoutePercent:300:800:Percentage of the ping time that is spent on
   * the ClientToServer portion of the ping. (used in more accurately syncronizing
   * clocks) Latency:KickOutDelay:100:2000:Amount of time the server can receive
   * no data from the player before the player is kicked.
   * Latency:NoFlagDelay:100:1000:Amount of time before the server can receive no
   * data from the player before it denies them the ability to pick up flags.
   * Latency:NoFlagPenalty:300:32000:Amount of time user is penalized when they
   * exceed the NoFlagDelay. Latency:SlowPacketKickoutPercent:0:1000:Percentage of
   * C2S slow packets before a player is kicked.
   * Latency:SlowPacketTime:20:200:Amount of latency C2S that constitutes a slow
   * packet. Latency:SlowPacketSampleSize:50:1000:Number of packets to sample C2S
   * before checking for kickout.
   * Latency:ClientSlowPacketKickoutPercent:0:1000:Percentage of S2C slow packets
   * before a player is kicked. Latency:ClientSlowPacketTime:20:200:Amount of
   * latency S2C that constitutes a slow packet.
   * Latency:ClientSlowPacketSampleSize:50:1000:Number of packets to sample S2C
   * before checking for kickout. Latency:MaxLatencyForWeapons:20:200:Maximum C2S
   * latency to allow before server disables weapons.
   * Latency:MaxLatencyForPrizes:50:800:Maximum amount of time that can pass
   * before a shared prize packet is ignored.
   * Latency:MaxLatencyForKickOut:40:200:Maximum latency that allowed before
   * kickout. Latency:LatencyKickOutTime:300:3000:Amount of time that
   * MaxLatencyForKickOut must be bad before the kickout occurs.
   * Latency:S2CNoDataKickoutDelay:100:32000:Amount of time a user can receive no
   * data from server before connection is terminated.
   * Latency:CutbackWatermark:500:32000:Amount of data the server is allowed to
   * send the user per second before it starts trying to cutback by skipping
   * non-critical packets. Latency:C2SNoDataAction:::0=Use Normal Method (Above
   * settings) 1=Display Sysop Warning, 2=Spec Player, 3=Warning&Spec, 4=Kick,
   * 5=Warning&Kick, 6=Kick&Spec, 7=Warning&Kick&Spec Latency:C2SNoDataTime:::If
   * above setting (C2SNoDataAction) is set, this is the delay (in 1/100 of a
   * second [100 = 1 second, 60000=1 minute]) that a player's ping can be maxed at
   * during a position packet before action takes place
   * Latency:NegativeClientSlowPacketTime:::Packets with future timestamp farther
   * in future than this variable are considered as slow packets (0=disabled).
   * Feature is still experimental.
   *
   * Message:MessageReliable:0:1:Whether messages are sent reliably.
   * Message:AllowAudioMessages:0:1:Whether players can send audio messages (0=no
   * 1=yes) Message:BongAllowed:0:1:Whether players can play bong sounds (0=no
   * 1=yes) Message:QuickMessageLimit:::Maximum number of messages that can be
   * sent in a row before player is kicked.
   * Message:MessageTeamReliable:0:1:Whether team messages are sent reliably.
   * Message:MessageDistance:::Don't think this is used anymore....
   *
   * Mine:MineAliveTime:0:60000:Time that mines are active (in hundredths of a
   * second) Mine:TeamMaxMines:0:32000:Maximum number of mines allowed to be
   * placed by an entire team
   *
   * Misc:FrequencyShipTypes:0:1:Whether ship type is based on frequency player is
   * on or not (0=no 1=yes) Misc:WarpPointDelay:::How long a Portal point is
   * active. Misc:DecoyAliveTime:::Time a decoy is alive (in hundredths of a
   * second) Misc:BounceFactor:::How bouncy the walls are (16=no-speed-loss)
   * Misc:SafetyLimit:::Amount of time that can be spent in the safe zone. (90000
   * = 15 mins) Misc:TickerDelay:::Amount of time between ticker help messages.
   * Misc:WarpRadiusLimit:::When ships are randomly placed in the arena, this
   * parameter will limit how far from the center of the arena they can be placed
   * (1024=anywhere) Misc:ActivateAppShutdownTime:::Amount of time a ship is
   * shutdown after application is reactivated (ie. when they come back from
   * windows mode) Misc:NearDeathLevel:::Amount of energy that constitutes a
   * near-death experience (ships bounty will be decreased by 1 when this occurs
   * -- used for dueling zone) Misc:VictoryMusic:0:1:Whether the zone plays
   * victory music or not. Misc:BannerPoints:::Number of points require to display
   * a banner Misc:MaxLossesToPlay:::Number of deaths before a player is forced
   * into spectator mode (0=never) Misc:SpectatorQuiet:0:1:Whether spectators can
   * talk to active players (1=no 0=yes) Misc:MaxPlaying:::Maximum number of
   * players that can be playing at one time (does not count spectators, 0=limited
   * only by maximum allowed in arena (not a user setting))
   * Misc:TimedGame:::Amount of time in a timed game (like speed zone) (0=not a
   * timed game) Misc:ResetScoreOnFrequencyChange:0:1:Whether a players score
   * should be reset when they change frequencies (used primarily for timed games
   * like soccer) Misc:SendPositionDelay:0:20:Amount of time between position
   * packets sent by client. Misc:SlowFrameCheck:0:1:Whether to check for slow
   * frames on the client (possible cheat technique) (flawed on some machines, do
   * not use) Misc:SlowFrameRate:0:35:Check whether client has too slow frame rate
   * to play (0=disabled, 1-35=Frame Rate limit, if < than this, kicked)
   * Misc:AllowSavedShips:0:1:Whether saved ships are allowed (do not allow saved
   * ship in zones where sub-arenas may have differing parameters) 1 = Savedfrom
   * last arena/lagout, 0 = New Ship when entering arena/zone
   * Misc:FrequencyShift:0:10000:Amount of random frequency shift applied to
   * sounds in the game. Misc:ExtraPositionData:0:1:Whether regular players
   * receive sysop data about a ship (leave this at zero)
   * Misc:SheepMessage:::String that Appears when a player types ?sheep
   * Misc:MaxPlayers:::This is max amount of players in this arena. Will override
   * the .ini max arena setting Misc:GreetMessage:::This message, if set to
   * anything, will be sent to the player who just enter arena/zone
   * Misc:PeriodicMessage0:::Info about this will be in explained through all 0-4
   * of these. Read next one down... To set it blank, set it to: 0
   * Misc:PeriodicMessage1:::Must be set to this format: (Time for repeating)
   * (Delay after arena is created) (Text) *Read Next!
   * Misc:PeriodicMessage2:::Time for repeating will be after every X mins
   * (60=1hour, 1440=1day), will display this message. *READ NEXT!
   * Misc:PeriodicMessage3:::Delay will be after first person enters arena, it
   * will then start this many mins after they enter. Good so they don't get one
   * instantly for no reason. *READ NEXT! Misc:PeriodicMessage4:::Text will be the
   * text. If text starts with a * (ie: *Hello), will act as a *zone message. If
   * no * (ie: Hello), will act as a *arena command. Misc:MaxXRes:::Max X res
   * limit, 0 = no limit Misc:MaxYRes:::Max Y res limit, 0 = no limit
   * Misc:ContinuumOnly:::If set to 0, anyone can play. If set to 1, only
   * continuum uses can play in ship, while all VIE clients will be locked in
   * spec. Misc:LevelFiles:::List of .lvz files, seperated by commas, that will be
   * downloaded via client and used in this arena. A + in front of .lvz file will
   * make it optional Misc:MinUsage:::Min total usage hours required to play in
   * this arena. Misc:StartInSpec:::If set to 1, all users entering arena start in
   * spec. Otherwise enter arena as normal. Misc:MaxTimerDrift:::Percentage how
   * much client timer is allowed to differ from server timer.
   * Misc:DisableScreenshot:::If set to 0, anyone can take screenshots. If set to
   * 1, only spectators can. Misc:AntiWarpSettleDelay:::Time (in 1/100 of a
   * second) after someone warps/portals/attaches that they cannot
   * warp/portal/attach again for (like a temp antiwarp enabled for them only)
   * Misc:SaveSpawnScore:::If set to 1, will save spawn scores to arenaname.scr.
   * If set to 0, or blank, will not create useless .scr files.
   *
   * Owner:UserId:::User ID number for Users name Owner:Name:::Owners Username
   *
   * PacketLoss:C2SKickOutPercent:::ClientToServer packetloss percentage before
   * being kicked (this is percentage that make it 800 = 80% good or allow 20%
   * packetloss) PacketLoss:S2CKickOutPercent:::ServerToClient packetloss
   * percentage before being kicked (this is percentage that make it 800 = 80%
   * good or allow 20% packetloss) PacketLoss:SpectatorPercentAdjust:::Amount of
   * extra packetloss a spectator is allowed to have.
   * PacketLoss:PacketLossDisableWeapons:0:1:Whether the server disables weapons
   * for high packetloss or not (1=yes 0=no)
   *
   * Periodic:RewardDelay:0:720000:Time interval between each periodic reward
   * (0=no periodic reward) Periodic:RewardMinimumPlayers:0:255:Number of players
   * that must be in the arena before periodic rewards will occur
   * Periodic:RewardPoints:-500:1000:Number of points given out to team members
   * (per flag owned). (Negative numbers = flagCount * playersInArena)
   *
   * Prize:MultiPrizeCount:::Number of random 'Greens' given with a 'MultiPrize'
   * Prize:PrizeFactor:::Number of prizes hidden is based on number of players in
   * game. This number adjusts the formula, higher numbers mean more prizes.
   * (*Note: 10000 is max, 10 greens per person) Prize:PrizeDelay:::How often
   * prizes are regenerated (in hundredths of a second)
   * Prize:PrizeHideCount:::Number of prizes that are regenerated every
   * PrizeDelay. Prize:MinimumVirtual:::Distance from center of arena that
   * prizes/flags/soccer-balls will generate Prize:UpgradeVirtual:::Amount of
   * additional distance added to MinimumVirtual for each player that is in the
   * game. Prize:PrizeMaxExist:::Maximum amount of time that a hidden prize will
   * remain on screen. (actual time is random) Prize:PrizeMinExist:::Minimum
   * amount of time that a hidden prize will remain on screen. (actual time is
   * random) Prize:PrizeNegativeFactor:::Odds of getting a negative prize. (1 =
   * every prize, 32000 = extremely rare) Prize:DeathPrizeTime:::How long the
   * prize exists that appears after killing somebody.
   * Prize:EngineShutdownTime:::Time the player is affected by an 'Engine
   * Shutdown' Prize (in hundredth of a second)
   * Prize:TakePrizeReliable:0:1:Whether prize packets are sent reliably (C2S)
   * Prize:S2CTakePrizeReliable:0:1:Whether prize packets are sent reliably (S2C)
   *
   * PrizeWeight:Recharge:::Likelyhood of 'Full Charge' prize appearing (NOTE! This is FULL CHARGE, not Recharge!! stupid vie)
   * PrizeWeight:QuickCharge:::Likelyhood of 'Recharge' prize appearing
   * PrizeWeight:Energy:::Likelyhood of 'Energy Upgrade' prize appearing
   * PrizeWeight:Rotation:::Likelyhood of 'Rotation' prize appearing
   * PrizeWeight:Stealth:::Likelyhood of 'Stealth' prize appearing
   * PrizeWeight:Cloak:::Likelyhood of 'Cloak' prize appearing
   * PrizeWeight:AntiWarp:::Likelyhood of 'AntiWarp' prize appearing
   * PrizeWeight:XRadar:::Likelyhood of 'XRadar' prize appearing
   * PrizeWeight:Warp:::Likelyhood of 'Warp' prize appearing
   * PrizeWeight:Gun:::Likelyhood of 'Gun Upgrade' prize appearing
   * PrizeWeight:Bomb:::Likelyhood of 'Bomb Upgrade' prize appearing
   * PrizeWeight:BouncingBullets:::Likelyhood of 'Bouncing Bullets' prize appearing
   * PrizeWeight:Thruster:::Likelyhood of 'Thruster' prize appearing
   * PrizeWeight:TopSpeed:::Likelyhood of 'Speed' prize appearing
   * PrizeWeight:MultiFire:::Likelyhood of 'MultiFire' prize appearing
   * PrizeWeight:Proximity:::Likelyhood of 'Proximity Bomb' prize appearing
   * PrizeWeight:Glue:::Likelyhood of 'Engine Shutdown' prize appearing
   * PrizeWeight:AllWeapons:::Likelyhood of 'Super!' prize appearing
   * PrizeWeight:Shields:::Likelyhood of 'Shields' prize appearing
   * PrizeWeight:Shrapnel:::Likelyhood of 'Shrapnel Upgrade' prize appearing
   * PrizeWeight:Repel:::Likelyhood of 'Repel' prize appearing
   * PrizeWeight:Burst:::Likelyhood of 'Burst' prize appearing
   * PrizeWeight:Decoy:::Likelyhood of 'Decoy' prize appearing
   * PrizeWeight:Thor:::Likelyhood of 'Thor' prize appearing
   * PrizeWeight:Portal:::Likelyhood of 'Portal' prize appearing
   * PrizeWeight:Brick:::Likelyhood of 'Brick' prize appearing
   * PrizeWeight:Rocket:::Likelyhood of 'Rocket' prize appearing
   * PrizeWeight:MultiPrize:::Likelyhood of 'Multi-Prize' prize appearing
   *
   * Radar:RadarMode:0:4:Radar mode (0=normal, 1=half/half, 2=quarters,
   * 3=half/half-see team mates, 4=quarters-see team mates)
   * Radar:RadarNeutralSize:0:1024:Size of area between blinded radar zones (in
   * pixels) Radar:MapZoomFactor:8:1000:A number representing how far you can see
   * on radar.
   *
   * Repel:RepelSpeed:::Speed at which players are repelled Repel:RepelTime:::Time
   * players are affected by the repel (in hundredths of a second)
   * Repel:RepelDistance:::Number of pixels from the player that are affected by a
   * repel.
   *
   * Rocket:RocketThrust:::Thrust value given while a rocket is active.
   * Rocket:RocketSpeed:::Speed value given while a rocket is active.
   *
   * Routing:RadarFavor:1:7:Number of packets somebody on radar receives (1 =
   * every packet, 3 = every fourth packet, 7 = every eighth packet)
   * Routing:CloseEnoughBulletAdjust:0:512:Distance off edge of screen in pixels
   * that bullet packets will always forward to player.
   * Routing:CloseEnoughBombAdjust:0:4096:Distance off edge of radar in pixels
   * that bomb packets will always forward to player. (in direction bomb is
   * heading only) Routing:DeathDistance:1000:16384:Distance death messages are
   * forwarded. Routing:DoubleSendPercent:500:900:Percentage packetloss at which
   * server starts double sending weapon packets.
   * Routing:WallResendCount:0:3:Number of times a create wall packet is sent
   * unreliably (in additional to the reliable send) Routing:QueuePositions:::Set
   * to 1 to use following 4 settings: Routing:PosSendRadar:::How long radar
   * packets are queued, default 100 ms Routing:PosSendEdge:::How long packets on
   * screen edge are queued, default 30 ms Routing:PosSendClose:::How long close
   * packets are queue, default 20 ms Routing:ClosePosPixels:::How near are
   * packets considered close, default 250 pixels
   *
   * Security:S2CKickOutPercentWeapons:::The percent kickout for not getting
   * weapon packets. Security:SecurityKickOff:0:1:Whether players doing security
   * violations get kicked off or not. Security:SuicideLimit:::Maximum number of
   * suicides before player is kicked (no longer used since there are no
   * suicides???) Security:MaxShipTypeSwitchCount:::Number of times a player can
   * change ship type without being removed from the arena
   * Security:PacketModificationMax:::Maximum number of modified packets allowed
   * before a security violation is triggered.
   * Security:MaxDeathWithoutFiring:::Number of times a player can die without
   * firing before being removed from the arena.
   *
   * Shrapnel:ShrapnelSpeed:::Speed that shrapnel travels
   * Shrapnel:InactiveShrapDamage:::Amount of damage shrapnel causes in it's first
   * 1/4 second of life. Shrapnel:ShrapnelDamagePercent:::Percentage of normal
   * damage applied to shrapnel (relative to bullets of same level) 0=0% 1000=100%
   * 2000=200% Shrapnel:Random:0:1:Whether shrapnel spreads in circular or random
   * patterns 0=circular 1=random
   *
   * Spawn:Team0-X:::If set to a value, this is the center point where Freq 0 will
   * start Spawn:Team0-Y:::If set to a value, this is the center point where Freq
   * 0 will start Spawn:Team0-Radius:::How large of a circle from center point can
   * they warp (in Tiles) Spawn:Team1-X:::If set to a value, this is the center
   * point where Freq 1 will start Spawn:Team1-Y:::If set to a value, this is the
   * center point where Freq 1 will start Spawn:Team1-Radius:::How large of a
   * circle from center point can they warp (in Tiles) Spawn:Team2-X:::If set to a
   * value, this is the center point where Freq 2 will start *NOTE: If not set,
   * but 0 and 1 set, will loop between 0 and 1 Spawn:Team2-Y:::If set to a value,
   * this is the center point where Freq 2 will start *NOTE: If not set, but 0 and
   * 1 set, will loop between 0 and 1 Spawn:Team2-Radius:::How large of a circle
   * from center point can they warp (in Tiles) Spawn:Team3-X:::If set to a value,
   * this is the center point where Freq 3 will start *NOTE: Repeats, Freq 4 will
   * use Team0's, Freq 5 use Team1's, etc Spawn:Team3-Y:::If set to a value, this
   * is the center point where Freq 3 will start *NOTE: Repeats, Freq 4 will use
   * Team0's, Freq 5 use Team1's, etc Spawn:Team3-Radius:::How large of a circle
   * from center point can they warp (in Tiles)
   *
   * Soccer:BallBounce:0:1:Whether the ball bounces off walls (0=ball go through
   * walls, 1=ball bounces off walls) Soccer:AllowBombs:0:1:Whether the ball
   * carrier can fire his bombs (0=no 1=yes) Soccer:AllowGuns:0:1:Whether the ball
   * carrier can fire his guns (0=no 1=yes) Soccer:PassDelay:0:10000:How long
   * after the ball is fired before anybody can pick it up (in hundredths of a
   * second) Soccer:Mode:0:6:Goal configuration (0=any goal,
   * 1=left-half/right-half, 2=top-half/bottom-half, 3=quadrants-defend-one-goal,
   * 4=quadrants-defend-three-goals, 5=sides-defend-one-goal,
   * 6=sides-defend-three-goals) Soccer:BallCount:::Number of soccer balls in the
   * arena (0=soccer game off) Soccer:SendTime:::How often the balls position is
   * updated (note: set larger if you have more soccer balls to prevent too much
   * modem traffic) Soccer:Reward:::Negative numbers equal absolute points given,
   * positive numbers use FlagReward formula. Soccer:CapturePoints:::If positive,
   * these points are distributed to each goal/team. When you make a goal, the
   * points get transferred to your goal/team. In timed games, team with most
   * points in their goal wins. If one team gets all the points, then they win as
   * well. If negative, teams are given 1 point for each goal, first team to reach
   * -CapturePoints points wins the game. Soccer:UseFlagger:0:1:If player with
   * soccer ball should use the Flag:Flagger* ship adjustments or not (0=no,
   * 1=yes) Soccer:BallLocation:0:1:Whether the balls location is displayed at all
   * times or not (0=not, 1=yes) Soccer:BallBlankDelay:::Amount of time a player
   * can receive no data from server and still pick up the soccer ball.
   * Soccer:CatchMinimum:::Minimun goals needed to win Soccer:CatchPoints:::Max
   * goals needed to win Soccer:WinBy:::Have to beat other team by this many goals
   * Soccer:DisableWallPass:::Set to 1 to disable passing of ball through a wall
   * Soccer:DisableBallKilling:::Set to 1 to disable people dieing in safety with
   * the ball
   *
   * Team:MaxFrequency:::Maximum number of frequencies allowed in arena (5 would
   * allow frequencies 0,1,2,3,4) Team:MaxPerTeam:::Maximum number of players on a
   * non-private frequency Team:MaxPerPrivateTeam:::Maximum number of players on a
   * private frequency (0=same as MaxPerTeam) Team:DesiredTeams:::Number of teams
   * the server creates when adding new players before it starts adding new
   * players to existing teams. Team:ForceEvenTeams:::Whether people are allowed
   * to change teams if it would make the teams uneven. 0 = no restrictions, 1-10
   * = allowed variance Team:SpectatorFrequency:::Frequency reserved for
   * spectators (does not have to be within MaxFrequency limit)
   *
   * Territory:RewardDelay:::Time interval between each territory reward (0=no
   * territory reward) Territory:RewardBaseFlags:::Minimum number of flags
   * required to receive the territory reward
   * Territory:RewardMinimumPlayers:::Minimum number of players required in game
   * to receive the territory reward Territory:RewardPoints:::Amount of points
   * given out to the players at end of each time interval (formula is
   * complicated)
   *
   * Toggle:AntiWarpPixels:::Distance Anti-Warp affects other players (in pixels)
   * (note: enemy must also be on radar)
   *
   * Wormhole:GravityBombs:0:1:Whether a wormhole affects bombs (0=no 1=yes)
   * Wormhole:SwitchTime:::How often the wormhole switches its destination.
   *
   * All:InitialRotation:::Initial rotation rate of the ship (0 = can't rotate, 400 = full rotation in 1 second)
   * All:InitialThrust:::Initial thrust of ship (0 = none) All:InitialSpeed:::Initial speed of ship (0 = can't move)
   * All:InitialRecharge:::Initial recharge rate, or how quickly this ship recharges its energy.
   * All:InitialEnergy:::Initial amount of energy that the ship can have.
   * All:MaximumRotation:::Maximum rotation rate of the ship (0 = can't rotate, 400 = full rotation in 1 second)
   * All:MaximumThrust:::Maximum thrust of ship (0 = none)
   * All:MaximumSpeed:::Maximum speed of ship (0 = can't move)
   * All:MaximumRecharge:::Maximum recharge rate, or how quickly this ship recharges its energy.
   * All:MaximumEnergy:::Maximum amount of energy that the ship can have.
   * All:UpgradeRotation:::Amount added per 'Rotation' Prize
   * All:UpgradeThrust:::Amount added per 'Thruster' Prize
   * All:UpgradeSpeed:::Amount added per 'Speed' Prize
   * All:UpgradeRecharge:::Amount added per 'Recharge Rate' Prize
   * All:UpgradeEnergy:::Amount added per 'Energy Upgrade' Prize
   *
   * All:CloakStatus:0:2:Whether ships are allowed to receive 'Cloak' 0=no 1=yes
   * 2=yes/start-with All:StealthStatus:0:2:Whether ships are allowed to receive
   * 'Stealth' 0=no 1=yes 2=yes/start-with All:XRadarStatus:0:2:Whether ships are
   * allowed to receive 'X-Radar' 0=no 1=yes 2=yes/start-with
   * All:AntiWarpStatus:0:2:Whether ships are allowed to receive 'Anti-Warp' 0=no
   * 1=yes 2=yes/start-with All:CloakEnergy:0:32000:Amount of energy required to
   * have 'Cloak' activated (thousanths per hundredth of a second)
   * All:StealthEnergy:0:32000:Amount of energy required to have 'Stealth'
   * activated (thousanths per hundredth of a second)
   * All:XRadarEnergy:0:32000:Amount of energy required to have 'X-Radar'
   * activated (thousanths per hundredth of a second)
   * All:AntiWarpEnergy:0:32000:Amount of energy required to have 'Anti-Warp'
   * activated (thousanths per hundredth of a second)
   *
   * All:InitialRepel:::Initial number of Repels given to ships when they start
   * All:InitialBurst:::Initial number of Bursts given to ships when they start
   * All:InitialBrick:::Initial number of Bricks given to ships when they start
   * All:InitialRocket:::Initial number of Rockets given to ships when they start
   * All:InitialThor:::Initial number of Thor's Hammers given to ships when they
   * start All:InitialDecoy:::Initial number of Decoys given to ships when they
   * start All:InitialPortal:::Initial number of Portals given to ships when they
   * start All:InitialGuns:0:3:Initial level a ship's guns fire 0=no guns
   * All:InitialBombs:0:3:Initial level a ship's bombs fire 0=no bombs
   * All:RepelMax:::Maximum number of Repels allowed in ships
   * All:BurstMax:::Maximum number of Bursts allowed in ships
   * All:DecoyMax:::Maximum number of Decoys allowed in ships
   * All:RocketMax:::Maximum number of Rockets allowed in ships
   * All:ThorMax:::Maximum number of Thor's Hammers allowed in ships
   * All:BrickMax:::Maximum number of Bricks allowed in ships
   * All:PortalMax:::Maximum number of Portals allowed in ships
   * All:MaxGuns:0:3:Maximum level a ship's guns can fire 0=no guns
   * All:MaxBombs:0:3:Maximum level a ship's bombs can fire 0=no bombs
   *
   * All:BulletFireEnergy:::Amount of energy it takes a ship to fire a single L1
   * bullet All:BulletSpeed:::How fast bullets travel All:BulletFireDelay:::delay
   * that ship waits after a bullet is fired until another weapon may be fired (in
   * hundredths of a second) All:MultiFireEnergy:::Amount of energy it takes a
   * ship to fire multifire L1 bullets All:MultiFireDelay:::delay that ship waits
   * after a multifire bullet is fired until another weapon may be fired (in
   * hundredths of a second) All:MultiFireAngle:::Angle spread between multi-fire
   * bullets and standard forward firing bullets. (111 = 1 degree, 1000 = 1
   * ship-rotation-point) All:DoubleBarrel:0:1:Whether ships fire with double
   * barrel bullets 0=no 1=yes
   *
   * All:BombFireEnergy:::Amount of energy it takes a ship to fire a single bomb
   * All:BombFireEnergyUpgrade:::Extra amount of energy it takes a ship to fire an
   * upgraded bomb. ie. L2 = BombFireEnergy+BombFireEnergyUpgrade
   * All:BombThrust:::Amount of back-thrust you receive when firing a bomb.
   * All:BombBounceCount:::Number of times a ship's bombs bounce before they
   * explode on impact All:BombSpeed:::How fast bombs travel
   * All:BombFireDelay:::delay that ship waits after a bomb is fired until another
   * weapon may be fired (in hundredths of a second) All:EmpBomb:0:1:Whether ships
   * fire EMP bombs 0=no 1=yes All:SeeBombLevel:0:4:If ship can see bombs on radar
   * (0=Disabled, 1=All, 2=L2 and up, 3=L3 and up, 4=L4 bombs only)
   *
   * All:MaxMines:::Maximum number of mines allowed in ships
   * All:SeeMines:0:1:Whether ships see mines on radar 0=no 1=yes
   * All:LandmineFireEnergy:::Amount of energy it takes a ship to place a single
   * L1 mine All:LandmineFireEnergyUpgrade:::Extra amount of energy it takes to
   * place an upgraded landmine. ie. L2 =
   * LandmineFireEnergy+LandmineFireEnergyUpgrade All:LandmineFireDelay:::delay
   * that ship waits after a mine is fired until another weapon may be fired (in
   * hundredths of a second)
   *
   * All:ShrapnelMax:0:31:Maximum amount of shrapnel released from a ship's bomb
   * All:ShrapnelRate:0:31:Amount of additional shrapnel gained by a 'Shrapnel
   * Upgrade' prize. All:BurstSpeed:::How fast the burst shrapnel is for this
   * ship. All:BurstShrapnel:::Number of bullets released when a 'Burst' is
   * activated
   *
   * All:TurretThrustPenalty:::Amount the ship's thrust is decreased with a turret
   * riding All:TurretSpeedPenalty:::Amount the ship's speed is decreased with a
   * turret riding All:TurretLimit:::Number of turrets allowed on a ship.
   * All:RocketTime:::How long a Rocket lasts (in hundredths of a second)
   * All:InitialBounty:::Number of 'Greens' given to ships when they start
   * All:AttachBounty:::Bounty required by ships to attach as a turret
   * All:AfterburnerEnergy:::Amount of energy required to have 'Afterburners'
   * activated. All:DisableFastShooting:0:1:If firing bullets, bombs, or thors is
   * disabled after using afterburners (1=enabled)
   *
   * All:Radius:::The ship's radius from center to outside, in pixels. Standard
   * value is 14 pixels. All:DamageFactor:::How likely a the ship is to take
   * damamage (ie. lose a prize) (0=special-case-never, 1=extremely likely,
   * 5000=almost never) All:PrizeShareLimit:::Maximum bounty that ships receive
   * Team Prizes All:SuperTime:1::How long Super lasts on the ship (in hundredths
   * of a second) All:ShieldsTime:1::How long Shields lasts on the ship (in
   * hundredths of a second) All:Gravity:::Uses this formula, where R = raduis
   * (tiles) and g = this setting; R = 1.325 * (g ^ 0.507) IE: If set to 500, then
   * your ship will start to get pulled in by the wormhole once you come within 31
   * tiles of it All:GravityTopSpeed:::Ship are allowed to move faster than their
   * maximum speed while effected by a wormhole. This determines how much faster
   * they can go (0 = no extra speed)
   *
   * All:SoccerBallFriction:::Amount the friction on the soccer ball (how quickly
   * it slows down -- higher numbers mean faster slowdown)
   * All:SoccerBallProximity:::How close the player must be in order to pick up
   * ball (in pixels) All:SoccerBallSpeed:::Initial speed given to the ball when
   * fired by the carrier. All:SoccerThrowTime:::Time player has to carry soccer
   * ball (in hundredths of a second)
   *
   * Spectator:HideFlags:0:1:If flags are to be shown to specs when they are
   * dropped (1=can't see them) Spectator:NoXRadar:0:1:If specs are allowed to
   * have X (0=yes, 1=no)
   *
   * +Maker:Maker:::Editing was done by Mine GO BOOM with the help of the letter
   * K. Version 1.34.14 For more help, visit http://www.shanky.com/server/
   *
   */
  /*
   * @Override public void write(JmeExporter ex) throws IOException {
   * OutputCapsule capsule = ex.getCapsule(this); //capsule.write(someIntValue,
   * "someIntValue", 1); //capsule.write(someFloatValue, "someFloatValue", 0f);
   * //capsule.write(someJmeObject, "someJmeObject", new Material()); }
   *
   * @Override public void read(JmeImporter im) throws IOException { InputCapsule
   * capsule = im.getCapsule(this); //someIntValue =
   * capsule.readInt("someIntValue", 1); //someFloatValue =
   * capsule.readFloat("someFloatValue", 0f); //someJmeObject =
   * capsule.readSavable("someJmeObject", new Material()); } }
   */
}
