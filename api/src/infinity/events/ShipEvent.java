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
package infinity.events;

import com.simsilica.es.EntityId;
import com.simsilica.event.EventType;

/**
 * Game-related events. Mostly done for interfacing better with the bots of
 * SubSpace Continuum
 *
 * @author Asser
 */
public class ShipEvent {

    public static EventType<ShipEvent> shipDestroyed = EventType.create("ShipDestroyed", ShipEvent.class);
    public static EventType<ShipEvent> shipSpawned = EventType.create("ShipSpawned", ShipEvent.class);
    public static EventType<ShipEvent> weaponFiring = EventType.create("WeaponFiring", ShipEvent.class);
    public static EventType<ShipEvent> weaponFired = EventType.create("WeaponFired", ShipEvent.class);
    public static EventType<ShipEvent> shipChangeAllowed = EventType.create("ShipChangeAllowed", ShipEvent.class);
    public static EventType<ShipEvent> shipChangeDenied = EventType.create("ShipChangeDenied", ShipEvent.class);

    private final EntityId shipId;

    public ShipEvent(final EntityId shipId) {
        this.shipId = shipId;
    }

    public EntityId getShipId() {
        return shipId;
    }

}
