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
package infinity.sim;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.Name;
import com.simsilica.es.common.Decay;
import com.simsilica.ext.mphys.Gravity;
import com.simsilica.ext.mphys.Impulse;
import com.simsilica.ext.mphys.Mass;
import com.simsilica.ext.mphys.ShapeInfo;
import com.simsilica.ext.mphys.SpawnPosition;
import com.simsilica.mathd.Vec3d;
import com.simsilica.mphys.PhysicsSpace;

import infinity.es.ArenaId;
import infinity.es.AudioTypes;
import infinity.es.Bounty;
import infinity.es.Buff;
import infinity.es.CollisionCategory;
import infinity.es.Delay;
import infinity.es.Flag;
import infinity.es.Frequency;
import infinity.es.Gold;
import infinity.es.GravityWell;
import infinity.es.HealthChange;
import infinity.es.Meta;
import infinity.es.Parent;
import infinity.es.PrizeType;
import infinity.es.ShapeNames;
import infinity.es.Spawner;
import infinity.es.TileType;
import infinity.es.TileTypes;
import infinity.es.WarpTouch;
import infinity.es.WeaponTypes;
import infinity.es.ship.Energy;
import infinity.es.ship.EnergyMax;
import infinity.es.ship.Recharge;
import infinity.es.ship.actions.Burst;
import infinity.es.ship.actions.BurstMax;
import infinity.es.ship.actions.Repel;
import infinity.es.ship.actions.RepelMax;
import infinity.es.ship.actions.Thor;
import infinity.es.ship.actions.ThorMax;
import infinity.es.ship.weapons.Bomb;
import infinity.es.ship.weapons.BombCost;
import infinity.es.ship.weapons.BombFireDelay;
import infinity.es.ship.weapons.BombLevelEnum;
import infinity.es.ship.weapons.GravityBomb;
import infinity.es.ship.weapons.GravityBombCost;
import infinity.es.ship.weapons.GravityBombFireDelay;
import infinity.es.ship.weapons.Gun;
import infinity.es.ship.weapons.GunCost;
import infinity.es.ship.weapons.GunFireDelay;
import infinity.es.ship.weapons.GunLevelEnum;
import infinity.es.ship.weapons.Mine;
import infinity.es.ship.weapons.MineCost;
import infinity.es.ship.weapons.MineFireDelay;
import infinity.es.ship.weapons.MineMax;

/**
 * Utility methods for creating the common game entities used by the simulation.
 * In cases where a game entity may have multiple specific components or
 * dependencies used to create it, it can be more convenient to have a
 * centralized factory method. Especially if those objects are widely used. For
 * entities with only a few components or that are created by one system and
 * only consumed by one other, then this is not necessarily true.
 */
public class GameEntities {

    // TODO: All constants should come through the parameters - for now, they come
    // from the constants
    // TODO: All parameters should be dumb types and should be the basis of the
    // complex types used in the backend
    public static EntityId createGravSphere(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos,
            @SuppressWarnings("unused") final double radius) {
        final EntityId result = ed.createEntity();
        ed.setComponents(result, ShapeInfo.create("gravitysphere", 1, ed), new SpawnPosition(phys.getGrid(), pos));

        ed.setComponent(result, new Meta(createdTime));
        return result;
    }

    public static EntityId createDelayedBomb(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime, final Vec3d pos, final Vec3d linearVelocity, final long decayMillis,
            final long scheduledMillis, final HashSet<EntityComponent> delayedComponents, final BombLevelEnum level) {

        final EntityId lastDelayedBomb = GameEntities.createBomb(ed, owner, phys, createdTime, pos, linearVelocity,
                decayMillis, level);

        ed.setComponents(lastDelayedBomb, new Delay(scheduledMillis, delayedComponents, Delay.SET));
        ed.setComponents(lastDelayedBomb, WeaponTypes.gravityBomb(ed));

        return lastDelayedBomb;
    }

    public static EntityId createBomb(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime, final Vec3d pos, final Vec3d linearVelocity, final long decayMillis,
            final BombLevelEnum level) {
        final EntityId lastBomb = ed.createEntity();

        ed.setComponents(lastBomb, ShapeInfo.create("bomb_l" + level.level, 0.5, ed),
                new SpawnPosition(phys.getGrid(), pos), new Mass(5),
                new Decay(createdTime, createdTime + TimeUnit.NANOSECONDS.convert(decayMillis, TimeUnit.MILLISECONDS)),
                WeaponTypes.bomb(ed), new Impulse(linearVelocity),
                new CollisionCategory(CollisionFilters.FILTER_CATEGORY_DYNAMIC_PROJECTILES), new Parent(owner));

        // new PointLightComponent(level.lightColor, level.lightRadius,
        // CorePhysicsConstants.SHIPLIGHTOFFSET));
        ed.setComponent(lastBomb, new Meta(createdTime));
        return lastBomb;
    }

    public static EntityId createBullet(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime, final Vec3d pos, final Vec3d linearVelocity, final long decayMillis,
            @SuppressWarnings("unused") final GunLevelEnum level, final String shapeName) {
        final EntityId lastBullet = ed.createEntity();

        ed.setComponents(lastBullet, ShapeInfo.create(shapeName, 0.125, ed), new SpawnPosition(phys.getGrid(), pos),
                new Mass(1),
                new Decay(createdTime, createdTime + TimeUnit.NANOSECONDS.convert(decayMillis, TimeUnit.MILLISECONDS)),
                WeaponTypes.bullet(ed), new Impulse(linearVelocity),
                new CollisionCategory(CollisionFilters.FILTER_CATEGORY_DYNAMIC_PROJECTILES), new Parent(owner));

        ed.setComponent(lastBullet, new Meta(createdTime));

        return lastBullet;
    }

    public static EntityId createArena(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final String arenaId,
            @SuppressWarnings("unused") final Vec3d pos) {
        final EntityId lastArena = ed.createEntity();

        ed.setComponents(lastArena, ShapeInfo.create(ShapeNames.ARENA, 1, ed),
                new SpawnPosition(phys.getGrid(), new Vec3d()), new ArenaId(arenaId));
        ed.setComponent(lastArena, new Meta(createdTime));

        return lastArena;
    }

    /*
     * public static EntityId createMapTile(String tileSet, short tileIndex, Vec3d
     * pos, Convex c, double invMass, String tileType, EntityData ed, Ini settings,
     * long createdTime, PhysicsSpace phys) { EntityId lastTileInfo =
     * ed.createEntity();
     *
     * ed.setComponents(lastTileInfo, TileType.create(tileType, tileSet, tileIndex,
     * ed), ViewTypes.mapTile(ed), new SpawnPosition(phys.getGrid(), pos),
     * PhysicsMassTypes.infinite(ed), PhysicsShapes.mapTile(c));
     * ed.setComponent(lastTileInfo, new Meta(createdTime));
     *
     * return lastTileInfo; }
     */
    // Explosion is for now only visual, so only object type and position
    public static EntityId createExplosion2(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos, final long decayMillis) {
        final EntityId lastExplosion = ed.createEntity();

        // Explosion is a ghost
        ed.setComponents(lastExplosion, ShapeInfo.create(ShapeNames.EXPLOSION2, 0, ed),
                new SpawnPosition(phys.getGrid(), pos),
                new Decay(createdTime, createdTime + TimeUnit.NANOSECONDS.convert(decayMillis, TimeUnit.MILLISECONDS)));
        ed.setComponent(lastExplosion, new Meta(createdTime));

        return lastExplosion;
    }

    public static EntityId createWormhole(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos, final double gravityRadius,
            @SuppressWarnings("unused") final double targetAreaRadius, final double force, final String gravityType,
            final Vec3d warpTargetLocation) {
        final EntityId lastWormhole = ed.createEntity();

        // Wormhome is also a ghost
        ed.setComponents(lastWormhole, ShapeInfo.create(ShapeNames.WORMHOLE, 0, ed),
                new SpawnPosition(phys.getGrid(), pos), new GravityWell(gravityRadius, force, gravityType),
                new WarpTouch(warpTargetLocation));
        ed.setComponent(lastWormhole, new Meta(createdTime));

        return lastWormhole;
    }

    /*
     * public static EntityId createAttack(EntityId owner, String attackType,
     * EntityData ed, Ini settings, long createdTime, PhysicsSpace phys) { EntityId
     * lastAttack = ed.createEntity(); ed.setComponents(lastAttack, new
     * Attack(owner), WeaponType.create(attackType, ed), new Damage(-20));
     *
     * return lastAttack; }
     *
     * public static EntityId createForce(EntityId owner, Force force, Vec3d
     * forceWorldCoords, EntityData ed, Ini settings, long createdTime, PhysicsSpace
     * phys) { EntityId lastForce = ed.createEntity(); ed.setComponents(lastForce,
     * new PhysicsForce(owner, force, forceWorldCoords)); ed.setComponent(lastForce,
     * new Meta(createdTime));
     *
     * return lastForce; }
     */
    public static EntityId createOver5(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos, final double force,
            final double gravityRadius, final String gravityType) {
        final EntityId lastOver5 = ed.createEntity();

        ed.setComponents(lastOver5, ShapeInfo.create(ShapeNames.OVER5, CorePhysicsConstants.OVER5SIZERADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos), new GravityWell(gravityRadius, force, gravityType));
        ed.setComponent(lastOver5, new Meta(createdTime));

        return lastOver5;
    }

    /**
     * Small asteroid with animation
     *
     * @param ed the entitydata set to create the entity in
     * @return the entityid of the created entity
     */
    public static EntityId createOver1(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos,
            @SuppressWarnings("unused") final int mass) {
        final EntityId lastOver1 = ed.createEntity();

        ed.setComponents(lastOver1, ShapeInfo.create(ShapeNames.OVER1, CorePhysicsConstants.OVER1SIZERADIUS, ed),
                // new Mass(mass),
                new SpawnPosition(phys.getGrid(), pos));
        ed.setComponent(lastOver1, new Meta(createdTime));

        return lastOver1;
    }

    /**
     * Medium asteroid with animation
     *
     * @param location the Vec3d position of the asteroid
     * @param ed       the entitydata set to create the entity in
     * @param settings the settings to load this medium asteroid with
     * @return the entityid of the created entity
     */
    public static EntityId createOver2(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos) {
        final EntityId lastOver2 = ed.createEntity();

        ed.setComponents(lastOver2, ShapeInfo.create(ShapeNames.OVER2, CorePhysicsConstants.OVER2SIZERADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos));
        ed.setComponent(lastOver2, new Meta(createdTime));

        return lastOver2;
    }

    public static EntityId createWarpEffect(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos, final long decayMillis) {
        final EntityId lastWarpTo = ed.createEntity();

        // Warp is a ghost
        ed.setComponents(lastWarpTo, ShapeInfo.create(ShapeNames.WARP, 0, ed), new SpawnPosition(phys.getGrid(), pos),
                new Decay(createdTime, createdTime + TimeUnit.NANOSECONDS.convert(decayMillis, TimeUnit.MILLISECONDS)));
        ed.setComponent(lastWarpTo, new Meta(createdTime));

        return lastWarpTo;
    }

    public static EntityId createCaptureTheFlag(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos) {
        final EntityId lastFlag = ed.createEntity();

        ed.setComponents(lastFlag, ShapeInfo.create(ShapeNames.FLAG_OURS, CorePhysicsConstants.FLAGSIZERADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos), new Flag(), new Frequency(0));
        ed.setComponent(lastFlag, new Meta(createdTime));

        return lastFlag;
    }

    public static EntityId createHealthBuff(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            @SuppressWarnings("unused") final PhysicsSpace<?, ?> phys, final long createdTime, final int healthChange,
            final EntityId target) {

        final EntityId lastHealthBuff = ed.createEntity();

        ed.setComponents(lastHealthBuff, new HealthChange(healthChange), // apply the damage
                new Buff(target, 0)); // apply right away
        ed.setComponent(lastHealthBuff, new Meta(createdTime));

        return lastHealthBuff;
    }

    public static EntityId createLight(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos) {

        final EntityId lastLight = ed.createEntity();

        ed.setComponents(lastLight, new SpawnPosition(phys.getGrid(), pos));
        ed.setComponent(lastLight, new Meta(createdTime));

        return lastLight;
    }

    // TODO: All constants should come through the parameters - for now, they come
    // from the constants
    // TODO: All parameters should be dumb types and should be the basis of the
    // complex types used in the backend
    public static EntityId createWarbird(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime) {
        final EntityId result = ed.createEntity();
        final Name name = ed.getComponent(owner, Name.class);
        ed.setComponent(result, name);

        ed.setComponents(result, ShapeInfo.create(ShapeNames.SHIP_WARBIRD, CorePhysicsConstants.SHIPSIZERADIUS, ed));

        // ViewTypes.ship_warbird(ed),
        // PhysicsMassTypes.normal(ed),
        // PhysicsShapes.ship());
        ed.setComponent(result, new SpawnPosition(phys.getGrid(), new Vec3d(20, 0.5, 20)));
        ed.setComponent(result, new Mass(1));
        ed.setComponent(result, Gravity.ZERO);

        ed.setComponent(result, new Frequency(1));
        ed.setComponent(result, new Gold(0));

        ed.setComponent(result, new Energy(CoreGameConstants.SHIPHEALTH));
        ed.setComponent(result, new EnergyMax(CoreGameConstants.SHIPHEALTH * 2));
        ed.setComponent(result, new Recharge(100));

        // Add bombs:
        ed.setComponent(result, new Bomb(BombLevelEnum.BOMB_1));
        ed.setComponent(result, new BombCost(2));
        ed.setComponent(result, new BombFireDelay(500));

        // Add burst:
        ed.setComponent(result, new Burst(5));
        ed.setComponent(result, new BurstMax(5));

        // Add guns:
        ed.setComponent(result, new Gun(GunLevelEnum.LEVEL_1));
        ed.setComponent(result, new GunCost(10));
        ed.setComponent(result, new GunFireDelay(250));

        // Add gravity bombs
        ed.setComponent(result, new GravityBomb(BombLevelEnum.BOMB_1));
        ed.setComponent(result, new GravityBombCost(10));
        ed.setComponent(result, new GravityBombFireDelay(1000));

        // Add mines
        ed.setComponent(result, new Mine(BombLevelEnum.BOMB_1));
        ed.setComponent(result, new MineCost(50));
        ed.setComponent(result, new MineFireDelay(500));
        ed.setComponent(result, new MineMax(4));

        // Add thors
        ed.setComponent(result, new Thor(2));
        ed.setComponent(result, new ThorMax(2));

        // Add repels
        ed.setComponent(result, new Repel(10));
        ed.setComponent(result, new RepelMax(20));

        ed.setComponent(result, new CollisionCategory(CollisionFilters.FILTER_CATEGORY_DYNAMIC_PLAYERS));

        // ed.setComponent(result, new PointLightComponent(ColorRGBA.White,
        // CoreViewConstants.SHIPLIGHTRADIUS, CorePhysicsConstants.SHIPLIGHTOFFSET));
        ed.setComponent(result, new Meta(createdTime));
        return result;
    }

    public static EntityId createPrize(final EntityData ed, final PhysicsSpace<?, ?> phys, final long createdTime,
            final Vec3d pos, final String prizeType) {
        final EntityId result = ed.createEntity();

        ed.setComponents(result, ShapeInfo.create(ShapeNames.PRIZE, CorePhysicsConstants.PRIZESIZERADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos), new Bounty(CoreGameConstants.BOUNTYVALUE),
                PrizeType.create(prizeType, ed), new Decay(createdTime, createdTime
                        + TimeUnit.NANOSECONDS.convert(CoreGameConstants.PRIZEDECAY, TimeUnit.MILLISECONDS)));

        ed.setComponent(result, new Meta(createdTime));
        return result;
    }

    public static EntityId createPrizeSpawner(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final Vec3d pos,
            @SuppressWarnings("unused") final double radius) {
        final EntityId result = ed.createEntity();
        ed.setComponents(result,
                // Possible to add model if we want the players to be able to see the spawner
                new SpawnPosition(phys.getGrid(), pos),
                new Spawner(CoreGameConstants.PRIZEMAXCOUNT, Spawner.SpawnType.Prizes));
        ed.setComponent(result, new Meta(createdTime));
        return result;
    }

    public static EntityId createBurst(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime, final Vec3d pos, @SuppressWarnings("unused") final Vec3d linearVelocity,
            final long decayMillis) {
        final EntityId lastBomb = ed.createEntity();

        ed.setComponents(lastBomb,
                // ViewTypes.burst(ed),
                ShapeInfo.create(ShapeNames.BURST, CorePhysicsConstants.BURSTSIZERADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos),
                // new PhysicsVelocity(new Vec3d(linearVelocity.x, linearVelocity.y)),
                new Decay(createdTime, createdTime + TimeUnit.NANOSECONDS.convert(decayMillis, TimeUnit.MILLISECONDS)),
                WeaponTypes.burst(ed),
                // PhysicsMassTypes.normal_bullet(ed),
                // PhysicsShapes.burst(),
                new Parent(owner)
        // new PointLightComponent(level.lightColor, level.lightRadius));
        );
        ed.setComponent(lastBomb, new Meta(createdTime));
        return lastBomb;
    }

    public static EntityId createMapTile(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final String tileSet, final short tileIndex,
            final Vec3d pos, final String tileType) {
        final EntityId lastTileInfo = ed.createEntity();

        ed.setComponents(lastTileInfo, TileType.create(tileType, tileSet, tileIndex, ed),
                // TODO: Register map tiles with a block shape factory instead of default sphere
                // factory
                new Mass(0), ShapeInfo.create(ShapeNames.MAPTILE, CorePhysicsConstants.MAPTILEWIDTH, ed),
                new SpawnPosition(phys.getGrid(), pos));

        ed.setComponent(lastTileInfo, new Meta(createdTime));

        return lastTileInfo;
    }

    // This is called by the server when it has calculcated the correct tileIndex
    // number
    public static EntityId updateWangBlobEntity(final EntityData ed, @SuppressWarnings("unused") final EntityId owner,
            final PhysicsSpace<?, ?> phys, final long createdTime, final EntityId entity, final String tileSet,
            final short tileIndex, final Vec3d pos) {

        // TODO: Update the shapename to something from ShapeNames
        ed.setComponents(entity, TileTypes.wangblob(tileSet, tileIndex, ed), ShapeInfo.create("wangblob", 1, ed),
                new SpawnPosition(phys.getGrid(), pos));

        ed.setComponent(entity, new Meta(createdTime));
        return entity;
    }

    /*
     * public static EntityId createForce(EntityId owner, Force force, Vec3d
     * forceWorldCoords, EntityData ed, long createdTime, PhysicsSpace phys) {
     * EntityId lastForce = ed.createEntity(); ed.setComponents(lastForce, new
     * PhysicsForce(owner, force, forceWorldCoords));
     *
     * ed.setComponent(lastForce, new Meta(createdTime)); return lastForce; }
     */
    public static EntityId createRepel(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime, final Vec3d pos) {
        final EntityId lastWarpTo = ed.createEntity();

        ed.setComponents(lastWarpTo, ShapeInfo.create(ShapeNames.REPEL, CorePhysicsConstants.REPELRADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos),
                new Decay(createdTime,
                        createdTime
                                + TimeUnit.NANOSECONDS.convert(CoreViewConstants.REPELDECAY, TimeUnit.MILLISECONDS)),
                new Parent(owner), AudioTypes.repel(ed));

        ed.setComponent(lastWarpTo, new Meta(createdTime));
        return lastWarpTo;
    }

    public static EntityId createThor(final EntityData ed, final EntityId owner, final PhysicsSpace<?, ?> phys,
            final long createdTime, final Vec3d pos, @SuppressWarnings("unused") final Vec3d attackVelocity,
            final long thorDecay) {
        final EntityId lastBomb = ed.createEntity();

        ed.setComponents(lastBomb, ShapeInfo.create(ShapeNames.THOR, CorePhysicsConstants.THORSIZERADIUS, ed),
                new SpawnPosition(phys.getGrid(), pos),
                new Decay(createdTime, createdTime + TimeUnit.NANOSECONDS.convert(thorDecay, TimeUnit.MILLISECONDS)),
                WeaponTypes.thor(ed), new Parent(owner));
        ed.setComponent(lastBomb, new Meta(createdTime));

        return lastBomb;
    }
}
