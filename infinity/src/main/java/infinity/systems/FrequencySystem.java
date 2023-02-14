package infinity.systems;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.mphys.AbstractBody;
import com.simsilica.mphys.Contact;
import com.simsilica.mphys.ContactListener;
import com.simsilica.mphys.PhysicsSpace;
import com.simsilica.mphys.RigidBody;
import com.simsilica.mphys.StaticBody;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import infinity.es.Flag;
import infinity.es.Frequency;
import infinity.server.chat.InfinityChatHostedService;
import infinity.sim.AccessLevel;
import infinity.sim.CommandTriConsumer;
import infinity.sim.GameSounds;
import java.util.regex.Pattern;

/**
 * A system that handles the frequency of the flags and players.
 *
 * @author AFahrenholz
 */
public class FrequencySystem extends AbstractGameSystem implements ContactListener {

  private final Pattern freuencyChange = Pattern.compile("\\=(\\d+)");
  private EntityData ed;
  private PhysicsSpace phys;
  private EntitySet freqencies;
  private EntitySet flags;
  private InfinityChatHostedService chat;
  private SimTime time;

  @Override
  protected void initialize() {
    ed = getSystem(EntityData.class, true);
    phys = getSystem(PhysicsSpace.class, true);

    freqencies = ed.getEntities(Frequency.class);
    flags = ed.getEntities(Flag.class);

    this.chat = getSystem(InfinityChatHostedService.class);
    // Register consuming methods for patterns
    chat.registerPatternTriConsumer(
        freuencyChange,
        "The command to load a new map is ~loadArena <mapName>, where <mapName> is the "
            + "name of the map you want to load",
        new CommandTriConsumer<>(AccessLevel.PLAYER_LEVEL, this::changeFrequency));

    // Register this as a contact listener with the ContactSystem
    getSystem(ContactSystem.class, true).addListener(this);
  }

  /**
   * Changes the frequency of the player's avatar
   *
   * @param entityId The id of the player
   * @param freq The new frequency
   */
  private void changeFrequency(EntityId entityId, EntityId avatarEntityId, String freq) {
    ed.setComponent(avatarEntityId, new Frequency(Integer.parseInt(freq)));
  }

  @Override
  protected void terminate() {
    //Remove this as a contact listener with the ContactSystem
    getSystem(ContactSystem.class, true).removeListener(this);
  }

  @Override
  public void newContact(Contact contact) {
    RigidBody body1 = contact.body1;
    AbstractBody body2 = contact.body2;

    // For now, all flags are static and cannot be picked up, but can change frequencies
    if (body2 instanceof StaticBody) {
      EntityId ship = (EntityId) body1.id;
      EntityId flag = (EntityId) body2.id;

      // Check if entity one is a ship and has a frequency and if entity two is flag with a
      // different frequency
      if (freqencies.containsId(ship) && flags.containsId(flag)) {
        int shipFreq = freqencies.getEntity(ship).get(Frequency.class).getFrequency();
        // Check if flag has a frequency
        if (freqencies.containsId(flag)) {
          int flagFreq = freqencies.getEntity(flag).get(Frequency.class).getFrequency();
          if (shipFreq != flagFreq) {
            // Set the flag to the frequency of the ship
            ed.setComponent(
                flag,
                new Frequency(freqencies.getEntity(ship).get(Frequency.class).getFrequency()));
            GameSounds.createFlagSound(ed, EntityId.NULL_ID, phys, time.getTime(), body2.position);
          }
        } else {
          // Set the flag to the frequency of the ship
          ed.setComponent(
              flag, new Frequency(freqencies.getEntity(ship).get(Frequency.class).getFrequency()));
          GameSounds.createFlagSound(ed, EntityId.NULL_ID, phys, time.getTime(), body2.position);
        }
        contact.disable();
      }
    }
  }

  @Override
  public void update(SimTime time) {
    freqencies.applyChanges();
    flags.applyChanges();

    this.time = time;
  }
}