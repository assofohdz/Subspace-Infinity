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

package infinity.es;

import com.simsilica.es.EntityComponent;

/**
 * This component enables an entity that spawns different types of entities.
 *
 * @author Asser
 */
public class Spawner implements EntityComponent {

  private boolean weighted;
  private int maxCount;
  private SpawnType type;
  // Add option to have a spawn interval between spawning, in milliseconds
  private double spawnInterval;
  // add option to spawn on the radius or in the circle
  private boolean spawnOnRing;
  public Spawner(
      final int maxCount,
      final double spawnInterval,
      final boolean spawnAllOver,
      final SpawnType type,
      final boolean weighted) {
    this.maxCount = maxCount;
    this.type = type;
    this.spawnInterval = spawnInterval;
    this.spawnOnRing = spawnAllOver;
    this.weighted = weighted;
  }

  public Spawner() {}

  public Spawner(final SpawnType type) {
    this.type = type;
  }

  public boolean isWeighted() {
    return weighted;
  }

  public boolean spawnOnRing() {
    return spawnOnRing;
  }

  public double getSpawnInterval() {
    return spawnInterval;
  }

  public int getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(final int maxCount) {
    this.maxCount = maxCount;
  }

  public SpawnType getType() {
    return type;
  }

  public void setType(final SpawnType type) {
    this.type = type;
  }

  public enum SpawnType {
    Players,
    Prizes
  }
}
