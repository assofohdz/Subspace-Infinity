/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package infinity.systems;

import com.jme3.math.FastMath;
import com.simsilica.es.*;
import com.simsilica.ext.mphys.MPhysSystem;
import com.simsilica.mathd.Quatd;
import com.simsilica.mathd.Vec3d;
import com.simsilica.mblock.phys.MBlockShape;
import com.simsilica.mphys.*;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import infinity.es.*;
import infinity.es.ship.actions.Burst;
import infinity.es.ship.actions.Thor;
import infinity.es.ship.weapons.*;
import infinity.sim.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * @author AFahrenholz
 */
public class WeaponsSystem extends AbstractGameSystem implements ContactListener {

  public static final byte GUN = 0x0;
  public static final byte BOMB = 0x1;
  public static final byte GRAVBOMB = 0x2;
  public static final byte MINE = 0x3;
  public static final byte BURST = 0x4;
  public static final byte THOR = 0x5;
  static Logger log = LoggerFactory.getLogger(WeaponsSystem.class);
  private final LinkedHashSet<Attack> sessionAttackCreations = new LinkedHashSet<>();
  private EntityData ed;
  // private BinIndex binIndex;
  // private BinEntityManager binEntityManager;
  private MPhysSystem<MBlockShape> physics;
  private PhysicsSpace<EntityId, MBlockShape> space;
  private EntitySet thors, mines, gravityBombs, bursts, bombs, guns;

  private SimTime time;
  private EnergySystem health;
  // private SettingsSystem settings;

  protected MPhysSystem<MBlockShape> getPhysicsSystem() {
    final MPhysSystem<?> s = getSystem(MPhysSystem.class);
    @SuppressWarnings("unchecked")
    final MPhysSystem<MBlockShape> result = (MPhysSystem<MBlockShape>) s;
    return result;
  }

  @Override
  protected void initialize() {
    ed = getSystem(EntityData.class);
    if (ed == null) {
      throw new RuntimeException(getClass().getName() + " system requires an EntityData object.");
    }
    physics = getPhysicsSystem();
    if (physics == null) {
      throw new RuntimeException(getClass().getName() + " system requires the MPhysSystem system.");
    }

    space = physics.getPhysicsSpace();
    // binIndex = space.getBinIndex();
    // binEntityManager = physics.getBinEntityManager();

    health = getSystem(EnergySystem.class);

    guns = ed.getEntities(Gun.class, GunFireDelay.class, GunCost.class);

    bombs = ed.getEntities(Bomb.class, BombFireDelay.class, BombCost.class);

    bursts = ed.getEntities(Burst.class);

    gravityBombs =
        ed.getEntities(GravityBomb.class, GravityBombFireDelay.class, GravityBombCost.class);

    mines = ed.getEntities(Mine.class, MineFireDelay.class, MineCost.class);

    thors = ed.getEntities(Thor.class);

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

    thors.release();
    thors = null;

    getSystem(ContactSystem.class).removeListener(this);
  }

  @Override
  public void update(final SimTime tpf) {
    time = tpf;

    // Update who has what ship weapons
    guns.applyChanges();

    bombs.applyChanges();

    gravityBombs.applyChanges();

    mines.applyChanges();

    bursts.applyChanges();

    thors.applyChanges();

    /*
     * Default pattern to let multiple sessions call methods and then process them
     * one by one
     */
    final Iterator<Attack> iterator = sessionAttackCreations.iterator();
    while (iterator.hasNext()) {
      final Attack a = iterator.next();

      attack(a.getOwner(), a.getWeaponType());

      iterator.remove();
    }
    /*
     * for (Attack attack : sessionAttackCreations) { this.attack(attack.getOwner(),
     * attack.getWeaponType()); } sessionAttackCreations.clear();
     */
  }

  /**
   * A request to attack with a weapon
   *
   * @param requestor the requesting entity
   * @param flag the weapon type to attack with
   */
  private void attack(final EntityId requestor, final byte flag) {
    switch (flag) {
      case WeaponsSystem.BOMB:
        entityAttackBomb(requestor);
        break;
      case WeaponsSystem.GUN:
        entityAttackGuns(requestor);
        break;
      case WeaponsSystem.BURST:
        entityBurst(requestor);
        break;
      case WeaponsSystem.GRAVBOMB:
        entityAttackGravityBomb(requestor);
        break;
      case WeaponsSystem.MINE:
        entityPlaceMine(requestor);
        break;
      case WeaponsSystem.THOR:
        entityAttackThor(requestor);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported weapontype " + flag + " in attack");
    }
  }

  /**
   * Checks that an entity can attack with Thors
   *
   * @param requestor requesting entity
   */
  private void entityAttackThor(final EntityId requestor) {
    // Check authorization and cooldown
    if (!thors.containsId(requestor)) {
      return;
    }
    final Thor shipThors = thors.getEntity(requestor).get(Thor.class);

    /*
     * Health check disabled because Thors are free to use //Check health if
     * (!health.hasHealth(requestor) || health.getHealth(requestor) <
     * shipGuns.getCost()) { return; } //Deduct health
     * health.createHealthChange(requestor, -1 * shipGuns.getCost());
     */
    // Perform attack
    final AttackInfo info = getAttackInfo(requestor, WeaponsSystem.THOR);

    // Todo: Get the setting for thor-damage from a centralized system
    attackThor(info, new Damage(20), requestor);

    // Set new cooldown
    // No cooldown on thors
    // ed.setComponent(requestor, new ThorFireDelay(shipThors.getCooldown()));
    // Reduce count of thors in inventory:
    if (shipThors.getCount() == 1) {
      ed.removeComponent(requestor, Thor.class);
    } else {
      ed.setComponent(requestor, new Thor(shipThors.getCount() - 1));
    }
  }

  // TODO: Get the damage from some setting instead of hard coded
  /**
   * Checks that an entity can attack with Bullets
   *
   * @param requestor requesting entity
   */
  private void entityAttackGuns(final EntityId requestor) {
    final Entity entity = guns.getEntity(requestor);
    // Entity doesnt have guns
    if (entity == null) {
      return;
    }
    final Gun shipGuns = entity.get(Gun.class);
    final GunCost shipGunCost = entity.get(GunCost.class);
    final GunFireDelay shipGunCooldown = entity.get(GunFireDelay.class);

    // Check authorization and check cooldown
    if (!guns.containsId(requestor) || shipGunCooldown.getPercent() < 1.0) {
      return;
    }

    // Check health
    if (!health.hasEnergy(requestor) || health.getHealth(requestor) < shipGunCost.getCost()) {
      return;
    }
    // Deduct health
    health.createHealthChange(requestor, -1 * shipGunCost.getCost());

    // Perform attack
    final AttackInfo info = getAttackInfo(requestor, WeaponsSystem.GUN);

    attackGuns(info, shipGuns.getLevel(), new Damage(-20), requestor);

    // Set new cooldown
    ed.setComponent(requestor, shipGunCooldown.copy());
  }

  /**
   * Checks that an entity can attack with Bombs
   *
   * @param requestor requesting entity
   */
  private void entityAttackBomb(final EntityId requestor) {
    final Entity entity = bombs.getEntity(requestor);
    final Bomb shipBombs = entity.get(Bomb.class);
    final BombFireDelay shipBombCooldown = entity.get(BombFireDelay.class);
    final BombCost shipBombCost = entity.get(BombCost.class);

    // Check authorization
    if (!bombs.containsId(requestor) || shipBombCooldown.getPercent() < 1.0) {
      return;
    }

    // Check health
    if (!health.hasEnergy(requestor) || health.getHealth(requestor) < shipBombCost.getCost()) {
      return;
    }
    // Deduct health
    health.createHealthChange(requestor, -1 * shipBombCost.getCost());

    // Perform attack
    final AttackInfo info = getAttackInfo(requestor, WeaponsSystem.BOMB);

    attackBomb(info, shipBombs.getLevel(), new Damage(-20), requestor);

    // Set new cooldown
    ed.setComponent(requestor, shipBombCooldown.copy());
  }

  /**
   * Checks that an entity can place Mines
   *
   * @param requestor requesting entity
   */
  private void entityPlaceMine(final EntityId requestor) {
    final Entity entity = mines.getEntity(requestor);
    final Mine shipMines = entity.get(Mine.class);
    final MineCost shipMineCost = entity.get(MineCost.class);
    final MineFireDelay shipMineCooldown = entity.get(MineFireDelay.class);

    // Check authorization and cooldown
    if (!mines.containsId(requestor) || shipMineCooldown.getPercent() < 1.0) {
      return;
    }

    // Check health
    if (!health.hasEnergy(requestor) || health.getHealth(requestor) < shipMineCost.getCost()) {
      return;
    }
    // Deduct health
    health.createHealthChange(requestor, -1 * shipMineCost.getCost());

    // Perform attack
    final AttackInfo info = getAttackInfo(requestor, WeaponsSystem.MINE);

    attackBomb(info, shipMines.getLevel(), new Damage(-20), requestor);

    // Set new cooldown
    ed.setComponent(requestor, shipMineCooldown.copy());
  }

  /**
   * Checks that an entity can attack with Bursts
   *
   * @param requestor requesting entity
   */
  private void entityBurst(final EntityId requestor) {

    // Check authorization
    if (!bursts.containsId(requestor)) {
      return;
    }
    final Burst shipBursts = bursts.getEntity(requestor).get(Burst.class);

    // No health check for these
    // Perform attack
    Quatd orientation = new Quatd();

    final float angle = (360 / CoreGameConstants.BURSTPROJECTILECOUNT) * FastMath.DEG_TO_RAD;

    final AttackInfo infoOrig = getAttackInfo(requestor, WeaponsSystem.BURST);
    for (int i = 0; i < CoreGameConstants.BURSTPROJECTILECOUNT; i++) {
      final AttackInfo info = infoOrig.clone();
      orientation = orientation.fromAngles(0, angle * i, 0);

      // log.info("Rotating (from original) degrees: "+rotation *
      // FastMath.RAD_TO_DEG);
      // Quaternion newOrientation =
      // info.getOrientation().toQuaternion().fromAngleAxis(rotation,
      // Vector3f.UNIT_Z);
      Vec3d newVelocity = info.getAttackVelocity();

      // Rotate:
      newVelocity = orientation.mult(newVelocity);
      // log.info("Attack velocity: "+newVelocity.toString());

      // log.info("rotated velocity: "+newVelocity.toString());
      // info.setOrientation(new Quatd(newOrientation));
      info.setAttackVelocity(newVelocity);

      attackBurst(info, new Damage(-30), requestor);
    }

    // Reduce count of bursts in inventory:
    if (shipBursts.getCount() == 1) {
      ed.removeComponent(requestor, Burst.class);
    } else {
      ed.setComponent(requestor, new Burst(shipBursts.getCount() - 1));
    }
  }

  /**
   * Checks that an entity can attack with Gravity Bombs
   *
   * @param requestor requesting entity
   */
  private void entityAttackGravityBomb(final EntityId requestor) {
    final Entity entity = gravityBombs.getEntity(requestor);

    final GravityBombFireDelay shipGravBombCooldown = entity.get(GravityBombFireDelay.class);
    final GravityBombCost shipGravBombCost = entity.get(GravityBombCost.class);

    // Check authorization
    if (!gravityBombs.containsId(requestor) || shipGravBombCooldown.getPercent() < 1.0) {
      return;
    }

    final GravityBomb shipGravityBombs = entity.get(GravityBomb.class);

    // Check health
    if (!health.hasEnergy(requestor) || health.getHealth(requestor) < shipGravBombCost.getCost()) {
      return;
    }
    // Deduct health
    health.createHealthChange(requestor, -1 * shipGravBombCost.getCost());

    // Perform attack
    final AttackInfo info = getAttackInfo(requestor, WeaponsSystem.GRAVBOMB);

    attackGravBomb(info, shipGravityBombs.getLevel(), new Damage(-20), requestor);

    // Set new cooldown
    ed.setComponent(requestor, shipGravBombCooldown.copy());
  }

  /**
   * Creates a bomb entity
   *
   * @param info the attack information
   * @param level the bomb level
   * @param damage the damage of the bomb
   */
  private void attackBomb(
      final AttackInfo info, final BombLevelEnum level, final Damage damage, final EntityId owner) {
    final EntityId projectile =
        GameEntities.createBomb(
            ed,
            owner,
            space,
            time.getTime(),
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.BULLETDECAY,
            level);
    ed.setComponent(projectile, damage);
    GameSounds.createBombSound(ed, owner, space, time.getTime(), info.getLocation(), level);
  }

  /**
   * Creates one or more burst entity
   *
   * @param info the attack information
   * @param owner the entity that fired this burst
   * @param damage the damage of the burst projectiles
   */
  private void attackBurst(final AttackInfo info, final Damage damage, final EntityId owner) {
    EntityId projectile;
    projectile =
        GameEntities.createBurst(
            ed,
            owner,
            space,
            time.getTime(),
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.BULLETDECAY);
    ed.setComponent(projectile, damage);
    GameSounds.createBurstSound(ed, owner, space, time.getTime(), info.getLocation());
  }

  /**
   * Creates a bullet entity
   *
   * @param info the attack information
   * @param level the bullet level
   * @param damage the damage of the bullet
   */
  private void attackGuns(
      final AttackInfo info, final GunLevelEnum level, final Damage damage, final EntityId owner) {

    final String shapeName = "bullet_l" + level.level;

    EntityId projectile;
    projectile =
        GameEntities.createBullet(
            ed,
            owner,
            space,
            time.getTime(),
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.BULLETDECAY,
            level,
            shapeName);
    ed.setComponent(projectile, damage);
    GameSounds.createBulletSound(ed, owner, space, time.getTime(), info.getLocation(), level);
  }

  /**
   * Creates a gravity bomb entity
   *
   * @param info the attack information
   * @param level the bomb level
   * @param damage the damage of the bomb
   */
  private void attackGravBomb(
      final AttackInfo info, final BombLevelEnum level, final Damage damage, final EntityId owner) {
    EntityId projectile;
    final HashSet<EntityComponent> delayedComponents = new HashSet<>();
    delayedComponents.add(
        new GravityWell(5, CoreGameConstants.GRAVBOMBWORMHOLEFORCE, GravityWell.PULL)); // Suck
    // everything
    // in
    // delayedComponents.add(new PhysicsVelocity(new Vector2(0, 0))); //Freeze the
    // bomb

    projectile =
        GameEntities.createDelayedBomb(
            ed,
            owner,
            space,
            time.getTime(),
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.GRAVBOMBDECAY,
            CoreGameConstants.GRAVBOMBDELAY,
            delayedComponents,
            level);
    ed.setComponent(projectile, new Damage(damage.getDamage()));

    GameSounds.createSound(
        ed, owner, space, time.getTime(), info.getLocation(), AudioTypes.FIRE_GRAVBOMB);
  }

  /**
   * Creates a thor entity
   *
   * @param info the attack information
   * @param damage the damage of the thor
   */
  private void attackThor(final AttackInfo info, final Damage damage, final EntityId owner) {
    EntityId projectile;

    projectile =
        GameEntities.createThor(
            ed,
            owner,
            space,
            time.getTime(),
            info.getLocation(),
            info.getAttackVelocity(),
            CoreGameConstants.THORDECAY);

    ed.setComponent(projectile, new Damage(damage.getDamage()));

    GameSounds.createSound(
        ed, owner, space, time.getTime(), info.getLocation(), AudioTypes.FIRE_THOR);
  }

  /**
   * Find the velocity and the position of the projectile
   *
   * @param attacker requesting entity
   * @param weaponFlag
   */
  private AttackInfo getAttackInfo(final EntityId attacker, final byte weaponFlag) {
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
    switch (weaponFlag) {
      case WeaponsSystem.MINE:
        projectileVelocity.set(0, 0, 0);
        break;
      default:
        break;
    }

    // Step 4: Find the translation
    final Vec3d shipPosition = new Vec3d(shipBody.position);
    // Default position is at the tip of the ship;
    Vec3d projectilePosition = new Vec3d(0, 0, 0); // CorePhysicsConstants.SHIPSIZERADIUS);
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
    // projectilePosition.addLocal(0, 0, CorePhysicsConstants.SAFETYOFFSET);

    // Rotate the projectile position just as the ship is rotated
    projectilePosition = shipRotation.mult(projectilePosition);
    // Translate by ship position
    projectilePosition = projectilePosition.add(shipPosition);

    return new AttackInfo(projectilePosition, projectileVelocity);
  }

  /**
   * Queue up an attack
   *
   * @param attacker the attacking entity
   * @param flag the weapon of choice
   */
  public void sessionAttack(final EntityId attacker, final byte flag) {
    sessionAttackCreations.add(new Attack(attacker, flag));
  }

  @Override
  public void newContact(Contact contact) {
    log.debug("WeaponsSystem contact detected: " + contact.toString());
    RigidBody body1 = contact.body1;
    AbstractBody body2 = contact.body2;


    //body2 = null means that body1 is hitting the world
    //we first want to test that we are not hitting ourselves, so both are rigidbodies
    if (body2 != null && body2 instanceof RigidBody) {
      EntityId idOne = (EntityId) body1.id;
      EntityId idTwo = (EntityId) body2.id;

      // Only interact with collision if a ship collides with a prize or vice verca
      // We only need to test this way around for ships and prizes since the rigidbody (ship) will
      // always be body1
        /*
      if (prizes.containsId(idTwo) && .containsId(idOne)) {
        log.trace("Entitysets contact resolution found it to be valid");

        PrizeType pt = prizes.getEntity(idTwo).get(PrizeType.class);
        GameSounds.createPrizeSound(ed, ourTime.getTime(), idOne, body1.position, phys);

        this.handlePrizeAcquisition(pt, idOne);
        // Remove prize
        ed.removeEntity(idTwo);
        // Disable contact for further resolution
        contact.disable();
      } else {
        log.trace("Entitysets contact resolution found it to NOT be valid");
      }*/
    }
  }

  /** AttackInfo is where attacks originate (location, orientation, rotation and velocity) */
  private static class AttackInfo {

    private Vec3d location;
    private Vec3d attackVelocity;

    public AttackInfo(final Vec3d location, final Vec3d attackVelocity) {
      this.location = location;
      this.attackVelocity = attackVelocity;
    }

    public Vec3d getLocation() {
      return location;
    }

    @SuppressWarnings("unused")
    public void setLocation(final Vec3d location) {
      this.location = location;
    }

    public Vec3d getAttackVelocity() {
      return attackVelocity;
    }

    public void setAttackVelocity(final Vec3d attackVelocity) {
      this.attackVelocity = attackVelocity;
    }

    @Override
    public AttackInfo clone() {
      return new AttackInfo(location.clone(), attackVelocity.clone());
    }
  }

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
