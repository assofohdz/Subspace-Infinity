/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package infinity.systems;

import com.jme3.math.FastMath;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.common.Decay;
import com.simsilica.ext.mphys.MPhysSystem;
import com.simsilica.ext.mphys.ShapeInfo;
import com.simsilica.mathd.Quatd;
import com.simsilica.mathd.Vec3d;
import com.simsilica.mblock.phys.MBlockShape;
import com.simsilica.mphys.AbstractBody;
import com.simsilica.mphys.Contact;
import com.simsilica.mphys.ContactListener;
import com.simsilica.mphys.PhysicsSpace;
import com.simsilica.mphys.RigidBody;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import infinity.es.Damage;
import infinity.es.Frequency;
import infinity.es.GravityWell;
import infinity.es.ShapeNames;
import infinity.es.ship.Energy;
import infinity.es.ship.actions.Burst;
import infinity.es.ship.actions.Thor;
import infinity.es.ship.weapons.BombCost;
import infinity.es.ship.weapons.BombCurrentLevel;
import infinity.es.ship.weapons.BombFireDelay;
import infinity.es.ship.weapons.Bounce;
import infinity.es.ship.weapons.GravityBomb;
import infinity.es.ship.weapons.GravityBombCost;
import infinity.es.ship.weapons.GravityBombFireDelay;
import infinity.es.ship.weapons.GunCost;
import infinity.es.ship.weapons.GunCurrentLevel;
import infinity.es.ship.weapons.GunFireDelay;
import infinity.es.ship.weapons.MineCost;
import infinity.es.ship.weapons.MineCurrentLevel;
import infinity.es.ship.weapons.MineFireDelay;
import infinity.sim.CoreGameConstants;
import infinity.sim.CorePhysicsConstants;
import infinity.sim.CoreViewConstants;
import infinity.sim.GameEntities;
import infinity.sim.GameSounds;
import infinity.sim.util.InfinityRunTimeException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This system handles the creation of projectiles and the application of damage to entities.
 *
 * @author AFahrenholz
 */
public class WeaponsSystem extends AbstractGameSystem
    implements ContactListener<EntityId, MBlockShape> {

  public static final byte GUN = 0x0;
  public static final byte BOMB = 0x1;
  public static final byte GRAVBOMB = 0x2;
  public static final byte MINE = 0x3;
  public static final byte BURST = 0x4;
  static Logger log = LoggerFactory.getLogger(WeaponsSystem.class);
  private final KeySetView<Attack, Boolean> sessionAttackCreations = ConcurrentHashMap.newKeySet();
  private EntityData ed;
  private MPhysSystem<MBlockShape> physics;
  private PhysicsSpace<EntityId, MBlockShape> physicsSpace;
  private EntitySet mines;
  private EntitySet gravityBombs;
  private EntitySet bursts;
  private EntitySet bombs;
  private EntitySet guns;
  private EntitySet frequencies;

  private SimTime time;
  private EnergySystem energySystem;
  private EntitySet damageEntities;
  private EntitySet energyEntities;

  @Override
  protected void initialize() {
    ed = getSystem(EntityData.class);
    if (ed == null) {
      throw new InfinityRunTimeException(
          getClass().getName() + " system requires an EntityData object.");
    }
    physics = getSystem(MPhysSystem.class);
    if (physics == null) {
      throw new InfinityRunTimeException(
          getClass().getName() + " system requires the MPhysSystem system.");
    }

    physicsSpace = physics.getPhysicsSpace();
    energySystem = getSystem(EnergySystem.class);
    guns = ed.getEntities(GunCurrentLevel.class, GunFireDelay.class, GunCost.class);
    bombs = ed.getEntities(BombCurrentLevel.class, BombFireDelay.class, BombCost.class);
    bursts = ed.getEntities(Burst.class);
    gravityBombs =
        ed.getEntities(GravityBomb.class, GravityBombFireDelay.class, GravityBombCost.class);
    mines = ed.getEntities(MineCurrentLevel.class, MineFireDelay.class, MineCost.class);

    damageEntities = ed.getEntities(Damage.class);
    energyEntities = ed.getEntities(Energy.class);

    frequencies = ed.getEntities(Frequency.class);

    getSystem(ContactSystem.class).addListener(this);
  }

  @Override
  protected void terminate() {
    guns.release();
    guns = null;

    bombs.release();
    bombs = null;

    gravityBombs.release();
    gravityBombs = null;

    mines.release();
    mines = null;

    bursts.release();
    bursts = null;

    frequencies.release();
    frequencies = null;

    damageEntities.release();
    damageEntities = null;

    energyEntities.release();
    energyEntities = null;

    getSystem(ContactSystem.class).removeListener(this);
  }

  @Override
  public void update(final SimTime tpf) {
    time = tpf;

    // Update who has
    guns.applyChanges();
    bombs.applyChanges();
    gravityBombs.applyChanges();
    mines.applyChanges();
    bursts.applyChanges();

    energyEntities.applyChanges();
    damageEntities.applyChanges();

    /*
     * Default pattern to let multiple sessions call methods and then process them
     * one by one
     *
     * Not sure if this is needed or there is a queue system already in place by the session
     * framework. 25-02-2023: Maybe the right way is to create "attack entities" that are
     * then handled through an entityset.
     */
    final Iterator<Attack> iterator = sessionAttackCreations.iterator();
    while (iterator.hasNext()) {
      final Attack a = iterator.next();

      Entity requester = ed.getEntity(a.getOwner());

      attack(requester, a.getWeaponType(), time.getTime());

      iterator.remove();
    }
  }

  private boolean canAttackGun(Entity requester) {
    EntityId requesterId = requester.getId();
    if (guns.contains(requester)) {
      final GunFireDelay gfd = ed.getComponent(requesterId, GunFireDelay.class);
      if (gfd.getPercent() < 1) {
        return false;
      }
      final GunCost gc = ed.getComponent(requesterId, GunCost.class);
      return gc.getCost() <= energySystem.getHealth(requesterId);
    }
    return false;
  }

  private boolean canAttackBomb(Entity requester) {
    EntityId requesterId = requester.getId();
    if (bombs.contains(requester)) {
      final BombFireDelay bfd = ed.getComponent(requesterId, BombFireDelay.class);
      if (bfd.getPercent() < 1) {
        return false;
      }
      final BombCost bc = ed.getComponent(requesterId, BombCost.class);
      return bc.getCost() <= energySystem.getHealth(requesterId);
    }
    return false;
  }

  private boolean canAttackGravityBomb(Entity requester) {
    EntityId requesterId = requester.getId();
    if (gravityBombs.contains(requester)) {
      final GravityBombFireDelay bfd = ed.getComponent(requesterId, GravityBombFireDelay.class);
      if (bfd.getPercent() < 1) {
        return false;
      }
      final GravityBombCost bc = ed.getComponent(requesterId, GravityBombCost.class);
      return bc.getCost() <= energySystem.getHealth(requesterId);
    }
    return false;
  }

  private boolean canAttackMine(Entity requester) {
    EntityId requesterId = requester.getId();
    if (mines.contains(requester)) {
      final MineFireDelay bfd = ed.getComponent(requesterId, MineFireDelay.class);
      if (bfd.getPercent() < 1) {
        return false;
      }
      final MineCost bc = ed.getComponent(requesterId, MineCost.class);
      return bc.getCost() <= energySystem.getHealth(requesterId);
    }
    return false;
  }

  private boolean canAttackBurst(Entity requester) {
    return bursts.contains(requester);
  }

  private boolean canAttack(Entity requester, byte weaponType) {
    if (requester == null) {
      return false;
    }
    switch (weaponType) {
      case GUN:
        return canAttackGun(requester);
      case BOMB:
        return canAttackBomb(requester);
      case GRAVBOMB:
        return canAttackGravityBomb(requester);
      case MINE:
        return canAttackMine(requester);
      case BURST:
        return canAttackBurst(requester);
      default:
        return false;
    }
  }

  private boolean setCoolDownGun(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (guns.contains(requester)) {
      final GunFireDelay gfd = ed.getComponent(requesterId, GunFireDelay.class);
      ed.setComponent(requesterId, gfd.copy());
      return true;
    }
    return false;
  }

  private boolean setCoolDownBomb(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (bombs.contains(requester)) {
      final BombFireDelay bfd = ed.getComponent(requesterId, BombFireDelay.class);
      ed.setComponent(requesterId, bfd.copy());
      return true;
    }
    return false;
  }

  private boolean setCoolDownGravityBomb(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (gravityBombs.contains(requester)) {
      final GravityBombFireDelay bfd = ed.getComponent(requesterId, GravityBombFireDelay.class);
      ed.setComponent(requesterId, bfd.copy());
      return true;
    }
    return false;
  }

  private boolean setCoolDownMine(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (mines.contains(requester)) {
      final MineFireDelay bfd = ed.getComponent(requesterId, MineFireDelay.class);
      ed.setComponent(requesterId, bfd.copy());
      return true;
    }
    return false;
  }

  private boolean setCoolDown(final Entity requester, final byte flag) {

    if (requester == null) {
      return false;
    }
    if (flag == GUN) {
      return setCoolDownGun(requester);
    } else if (flag == BOMB) {
      return setCoolDownBomb(requester);
    } else if (flag == GRAVBOMB) {
      return setCoolDownGravityBomb(requester);
    } else if (flag == MINE) {
      return setCoolDownMine(requester);
    } else if (flag == BURST) {
      // No delay on this for now
      return bursts.contains(requester);
    }
    return false;
  }

  private boolean deductCostOfAttackGun(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (guns.contains(requester)) {
      final GunCost gc = ed.getComponent(requesterId, GunCost.class);
      if (gc.getCost() > energySystem.getHealth(requesterId)) {
        return false;
      }
      energySystem.damage(requesterId, gc.getCost());
      return true;
    }
    return false;
  }

  private boolean deductCostOfAttackBomb(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (bombs.contains(requester)) {
      final BombCost bc = ed.getComponent(requesterId, BombCost.class);
      if (bc.getCost() > energySystem.getHealth(requesterId)) {
        return false;
      }
      energySystem.damage(requesterId, bc.getCost());
      return true;
    }
    return false;
  }

  private boolean deductCostOfAttackGravityBomb(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (gravityBombs.contains(requester)) {
      final GravityBombCost bc = ed.getComponent(requesterId, GravityBombCost.class);
      if (bc.getCost() > energySystem.getHealth(requesterId)) {
        return false;
      }
      energySystem.damage(requesterId, bc.getCost());
      return true;
    }
    return false;
  }

  private boolean deductCostOfAttackMine(final Entity requester) {
    EntityId requesterId = requester.getId();
    if (mines.contains(requester)) {
      final MineCost bc = ed.getComponent(requesterId, MineCost.class);
      if (bc.getCost() > energySystem.getHealth(requesterId)) {
        return false;
      }
      energySystem.damage(requesterId, bc.getCost());
      return true;
    }
    return false;
  }

  private boolean deductCostOfAttack(final Entity requester, final byte flag) {
    if (requester == null) {
      return false;
    }
    if (flag == GUN) {
      return deductCostOfAttackGun(requester);
    } else if (flag == BOMB) {
      return deductCostOfAttackBomb(requester);
    } else if (flag == GRAVBOMB) {
      return deductCostOfAttackGravityBomb(requester);
    } else if (flag == MINE) {
      return deductCostOfAttackMine(requester);
    } else if (flag == BURST) {
      // No cost on this for now
      // TODO: Add cost to burst
      return bursts.contains(requester);
    }
    return false;
  }

  private void createProjectileGun(Entity requesterEntity, final long time, AttackPosition info) {
    EntityId requester = requesterEntity.getId();

    GunCurrentLevel gunCurrentLevel = this.guns.getEntity(requester).get(GunCurrentLevel.class);
    final String bulletShape =
        CoreGameConstants.BULLETLEVELPREPENDTEXT + gunCurrentLevel.getLevel().level;

    EntityId gunProjectile;
    gunProjectile =
        GameEntities.createBullet(
            ed,
            requester,
            physicsSpace,
            time,
            info.location,
            info.attackVelocity,
            CoreGameConstants.BULLETDECAY,
            bulletShape);

    ed.setComponent(
        gunProjectile,
        new Damage(
            CoreViewConstants.EXPLOSION0DECAY,
            CoreGameConstants.BULLETDAMAGE,
            ShapeInfo.create(ShapeNames.EXPLODE_0, CoreViewConstants.EXPLOSION0SIZE, ed)));
  }

  private void createProjectileBomb(Entity requesterEntity, long time, AttackPosition info) {
    EntityId requester = requesterEntity.getId();
    BombCurrentLevel bombCurrentLevel = this.bombs.getEntity(requester).get(BombCurrentLevel.class);

    final String bombShape =
        CoreGameConstants.BOMBLEVELPREPENDTEXT + bombCurrentLevel.getLevel().level;

    final EntityId bombProjectile =
        GameEntities.createBomb(
            ed,
            requester,
            physicsSpace,
            time,
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.BULLETDECAY,
            bombShape);
    ed.setComponent(
        bombProjectile,
        new Damage(
            CoreViewConstants.EXPLOSION1DECAY,
            CoreGameConstants.BOMBDAMAGE,
            ShapeInfo.create(ShapeNames.EXPLODE_1, CoreViewConstants.EXPLOSION1SIZE, ed)));
  }

  private void createProjectileGravBomb(Entity requesterEntity, long time, AttackPosition info) {
    EntityId requester = requesterEntity.getId();
    GravityBomb gravityBomb = this.gravityBombs.getEntity(requester).get(GravityBomb.class);
    final GravityBombCost shipGravBombCost = ed.getComponent(requester, GravityBombCost.class);

    EntityId projectile;
    final HashSet<EntityComponent> delayedComponents = new HashSet<>();
    delayedComponents.add(
        new GravityWell(
            5, CoreGameConstants.GRAVBOMBWORMHOLEFORCE, GravityWell.PULL)); // Suck everything in

    projectile =
        GameEntities.createDelayedBomb(
            ed,
            requester,
            physicsSpace,
            time,
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.GRAVBOMBDECAY,
            CoreGameConstants.GRAVBOMBDELAY,
            delayedComponents,
            CoreGameConstants.BOMBLEVELPREPENDTEXT + gravityBomb.getLevel());

    ed.setComponent(
        projectile,
        new Damage(
            CoreViewConstants.EXPLOSION1DECAY,
            CoreGameConstants.GRAVBOMBDAMAGE,
            ShapeInfo.create(ShapeNames.EXPLODE_1, CoreViewConstants.EXPLOSION1SIZE, ed)));
  }

  private void createProjectileBurst(Entity requesterEntity, long time) {
    Quatd orientation = new Quatd();

    final double angle = (360d / CoreGameConstants.BURSTPROJECTILECOUNT) * FastMath.DEG_TO_RAD;

    final AttackPosition infoOrig = getAttackInfo(requesterEntity, WeaponsSystem.BURST);
    for (int i = 0; i < CoreGameConstants.BURSTPROJECTILECOUNT; i++) {
      final AttackPosition info = new AttackPosition(infoOrig);
      orientation = orientation.fromAngles(0, angle * i, 0);

      Vec3d newVelocity = info.getAttackVelocity();

      // Rotate:
      newVelocity = orientation.mult(newVelocity);

      info.setAttackVelocity(newVelocity);

      EntityId projectile;
      projectile =
          GameEntities.createBurst(
              ed,
              requesterEntity.getId(),
              physicsSpace,
              time,
              info.getLocation(),
              info.getAttackVelocity(),
              CoreGameConstants.BULLETDECAY);
      ed.setComponent(
          projectile,
          new Damage(
              CoreViewConstants.EXPLOSION0DECAY,
              20,
              ShapeInfo.create(ShapeNames.EXPLODE_0, CoreViewConstants.EXPLOSION0SIZE, ed)));
    }
  }

  private void createProjectile(
      Entity requesterEntity, final byte flag, long time, AttackPosition info) {
    switch (flag) {
      case GUN:
        createProjectileGun(requesterEntity, time, info);
        break;
      case BOMB:
        createProjectileBomb(requesterEntity, time, info);
        break;
      case GRAVBOMB:
        createProjectileGravBomb(requesterEntity, time, info);
        break;
      case MINE:
        createProjectileMine(requesterEntity, time, info);
        break;
      case BURST:
        createProjectileBurst(requesterEntity, time);
        break;
      default:
        throw new IllegalArgumentException("Unknown flag: " + flag);
    }
  }

  private boolean createProjectileMine(Entity requesterEntity, long time, AttackPosition info) {
    EntityId requester = requesterEntity.getId();
    MineCurrentLevel mineCurrentLevel = this.mines.getEntity(requester).get(MineCurrentLevel.class);
    MineCost mc = this.mines.getEntity(requester).get(MineCost.class);

    final String mineShape =
        CoreGameConstants.MINELEVELPREPENDTEXT + mineCurrentLevel.getLevel().level;

    final EntityId mineProjectile =
        GameEntities.createMine(
            ed,
            requester,
            physicsSpace,
            time,
            info.getLocation(),
            CoreGameConstants.MINEDECAY,
            mineShape);
    ed.setComponent(
        mineProjectile,
        new Damage(
            CoreViewConstants.EXPLOSION1DECAY,
            mc.getCost(),
            ShapeInfo.create(ShapeNames.EXPLODE_1, CoreViewConstants.EXPLOSION1SIZE, ed)));
    return true;
  }

  private boolean createSound(Entity requesterEntity, byte flag, long time, AttackPosition info) {
    EntityId requester = requesterEntity.getId();
    switch (flag) {
      case GUN:
        GunCurrentLevel gunCurrentLevel = this.guns.getEntity(requester).get(GunCurrentLevel.class);
        GameSounds.createBulletSound(
            ed, requester, physicsSpace, time, info.location, gunCurrentLevel.getLevel());
        return true;
      case BOMB:
        BombCurrentLevel bombCurrentLevel =
            this.bombs.getEntity(requester).get(BombCurrentLevel.class);
        GameSounds.createBombSound(
            ed, requester, physicsSpace, time, info.location, bombCurrentLevel.getLevel());
        return true;
      case GRAVBOMB:
        GravityBomb entityGravBomb = this.gravityBombs.getEntity(requester).get(GravityBomb.class);
        GameSounds.createBombSound(
            ed, requester, physicsSpace, time, info.location, entityGravBomb.getLevel());
        return true;
      case MINE:
        MineCurrentLevel mineCurrentLevel =
            this.mines.getEntity(requester).get(MineCurrentLevel.class);
        GameSounds.createMineSound(
            ed, requester, physicsSpace, time, info.location, mineCurrentLevel.getLevel());
        break;
      case BURST:
        break;
      default:
        throw new IllegalArgumentException("Unknown flag: " + flag);
    }
    return false;
  }

  /**
   * A request to attack with a weapon.
   *
   * @param requester the requesting entity
   * @param flag the weapon type to attack with
   */
  private void attack(final Entity requester, final byte flag, long time) {

    boolean canAttack = canAttack(requester, flag);
    if (canAttack) {
      boolean cooldownSet = setCoolDown(requester, flag);
      if (cooldownSet) {
        boolean costDeducted = deductCostOfAttack(requester, flag);
        if (costDeducted) {
          final AttackPosition info = getAttackInfo(requester, flag);
          createProjectile(requester, flag, time, info);
          createSound(requester, flag, time, info);
        }
      }
    }
  }

  /**
   * Find the velocity and the position of the projectile.
   *
   * @param attackerEntity requesting entity
   * @param weaponFlag the weapon type
   */
  private AttackPosition getAttackInfo(final Entity attackerEntity, final byte weaponFlag) {
    EntityId attacker = attackerEntity.getId();
    // Default vector for projectiles (z=forward):
    Vec3d projectileVelocity = new Vec3d(0, 0, 1);

    final RigidBody<?, ?> shipBody = physics.getPhysicsSpace().getBinIndex().getRigidBody(attacker);

    // Step 1: Scale the velocity based on weapon type, weapon level and ship type
    // TODO: Look these settings up in SettingsSystem
    switch (weaponFlag) {
      case WeaponsSystem.GUN:
        projectileVelocity.addLocal(0, 0, 50);
        break;
      case WeaponsSystem.BOMB:
        projectileVelocity.addLocal(0, 0, 25);
        break;
      case WeaponsSystem.GRAVBOMB:
        break;
      case WeaponsSystem.MINE:
        break;
      default:
        throw new AssertionError("Flag :" + weaponFlag + " not recognized");
    }

    // Step 2: Rotate the scaled velocity
    final Quatd shipRotation = new Quatd(shipBody.orientation);
    final Vec3d shipVelocity = shipBody.getLinearVelocity();
    projectileVelocity = shipRotation.mult(projectileVelocity);

    // Step 3: Add ship velocity:
    projectileVelocity.addLocal(shipVelocity);

    // Step 4: Correct mines:
    if (weaponFlag == WeaponsSystem.MINE) {
      projectileVelocity.set(0, 0, 0);
    }

    // Step 4: Find the translation
    final Vec3d shipPosition = new Vec3d(shipBody.position);

    Vec3d projectilePosition = new Vec3d(0, 0, 0);
    // Offset with the radius of the projectile
    switch (weaponFlag) {
      case WeaponsSystem.GUN:
        projectilePosition.addLocal(0, 0, CorePhysicsConstants.BULLETSIZERADIUS);
        break;
      case WeaponsSystem.BOMB:
      case WeaponsSystem.GRAVBOMB:
      case WeaponsSystem.MINE:
        projectilePosition.addLocal(0, 0, CorePhysicsConstants.BOMBSIZERADIUS);
        break;
      default:
        throw new AssertionError();
    }
    // Rotate the projectile position just as the ship is rotated
    projectilePosition = shipRotation.mult(projectilePosition);
    // Translate by ship position
    projectilePosition = projectilePosition.add(shipPosition);

    return new AttackPosition(projectilePosition, projectileVelocity);
  }

  /**
   * This method is called from the gamesession and acts as a queue entry.
   *
   * @param attacker the attacking entity
   * @param flag the weapon of choice
   */
  public void sessionAttack(final EntityId attacker, final byte flag) {
    sessionAttackCreations.add(new Attack(attacker, flag));
  }

  /**
   * The case of our bomb hitting ourselves is handled in the ContactSystem. Here we want to handle
   * the case where an entity that has a Damage component hits an entity that has a Health
   * component. We also want the handle the case where a projectile hits the world.
   *
   * <p>We cannot be sure that the entitysets are updated, so we have to inquire about the entities
   * here and now.
   *
   * @param contact the contact
   */
  @Override
  public void newContact(Contact contact) {
    RigidBody<EntityId, MBlockShape> body1 = contact.body1;
    AbstractBody<EntityId, MBlockShape> body2 = contact.body2;

    EntityId idOne = body1.id;
    Entity entity1 = ed.getEntity(idOne, Damage.class, Bounce.class, Thor.class, Energy.class);

    if (body2 instanceof RigidBody) {
      EntityId idTwo = body2.id;
      Entity entity2 = ed.getEntity(idTwo, Damage.class, Energy.class);

      log.debug("WeaponsSystem contact detected between: {} and {}", body1.id, body2.id);

      Entity damageEntity;
      Entity energyEntity;
      if (entity1.get(Damage.class) != null && entity2.get(Energy.class) != null) {
        damageEntity = entity1;
        energyEntity = entity2;
      } else if (entity2.get(Damage.class) != null && entity1.get(Energy.class) != null) {
        damageEntity = entity2;
        energyEntity = entity1;
      } else {
        return;
      }

      Damage damage = damageEntity.get(Damage.class);

      energySystem.damage(energyEntity.getId(), damage.getIntendedDamage());
      GameEntities.createExplosion(
          ed,
          EntityId.NULL_ID,
          physicsSpace,
          time.getTime(),
          contact.contactPoint,
          damage.getExplosionDecay(),
          damage.getExplosionShape());
      ed.setComponent(damageEntity.getId(), Decay.duration(time.getTime(), 0));
      contact.disable();
    } else if (body2 == null
        && entity1.get(Damage.class) != null
        && entity1.get(Thor.class) == null) {
      // body2 = null means that body1 is hitting the world
      // thors are handled in the action system
      Damage damage = entity1.get(Damage.class);

      Bounce bounce = ed.getComponent(idOne, Bounce.class);
      if (bounce != null) {
        // Retain all energy in the contact
        contact.restitution = 1;
        // Remove all friction so ingoing angle and outgoing angle are the same
        contact.friction = 0;
        if (bounce.getBounces() == 1) {
          ed.removeComponent(idOne, Bounce.class);
        } else {
          ed.setComponent(idOne, bounce.decreaseBounces());
        }
      } else {
        GameEntities.createExplosion(
            ed,
            EntityId.NULL_ID,
            physicsSpace,
            time.getTime(),
            contact.contactPoint,
            damage.getExplosionDecay(),
            damage.getExplosionShape());
        ed.setComponent(idOne, Decay.duration(time.getTime(), 0));
        contact.disable();
      }
    }
  }

  /** A class that holds the position information needed to create an attack. */
  private static class AttackPosition {

    private final Vec3d location;
    private Vec3d attackVelocity;

    public AttackPosition(final Vec3d location, final Vec3d attackVelocity) {
      this.location = location;
      this.attackVelocity = attackVelocity;
    }

    public AttackPosition(AttackPosition source) {
      this.location = source.location;
      this.attackVelocity = source.attackVelocity;
    }

    public Vec3d getLocation() {
      return location;
    }

    public Vec3d getAttackVelocity() {
      return attackVelocity;
    }

    // This is used when creating a burst attack, because we need to calculate, set and use new
    // angles
    public void setAttackVelocity(final Vec3d attackVelocity) {
      this.attackVelocity = attackVelocity;
    }
  }

  /**
   * A class that holds the information needed to create an attack. This is called from the game
   * session.
   */
  public class Attack {

    final EntityId owner;
    final byte flag;

    public Attack(final EntityId owner, final byte flag) {
      this.owner = owner;
      this.flag = flag;
    }

    public EntityId getOwner() {
      return owner;
    }

    public byte getWeaponType() {
      return flag;
    }
  }
}
