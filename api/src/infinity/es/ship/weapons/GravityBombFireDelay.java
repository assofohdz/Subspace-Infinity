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
package infinity.es.ship.weapons;

import com.simsilica.es.EntityComponent;

/**
 *
 * @author Asser
 */
public class GravityBombFireDelay implements EntityComponent {

    private final long start;
    private final long delta;

    public GravityBombFireDelay() {
        start = System.nanoTime();
        delta = 1000000 * 10;
    }

    public GravityBombFireDelay(final long deltaMillis) {
        start = System.nanoTime();
        delta = deltaMillis * 1000000;
    }

    public double getPercent() {
        final long time = System.nanoTime();
        return (double) (time - start) / delta;
    }

    /**
     * Create a new copy of this class witht the same delay
     *
     * @return new BombFireDelay instance
     */
    public GravityBombFireDelay copy() {
        return new GravityBombFireDelay(delta / 1000000);
    }

    @Override
    public String toString() {
        return "GravityBombsCooldown[" + (delta / 1000000.0) + " ms]";
    }
}
