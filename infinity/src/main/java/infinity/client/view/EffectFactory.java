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
package infinity.client.view;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import infinity.client.states.ModelViewState;

/**
 *
 * @author Asser
 */
public class EffectFactory {

    // private ModelViewState state;
    private AssetManager assets;
    // private EntityData ed;
    // private final float time = 0;
    // private final int state2 = 0;
    private final Node explosionEffect = new Node("explosionFX");
    private ParticleEmitter flame, flash, spark, roundspark, smoketrail, debris, shockwave;

    private static final int COUNT_FACTOR = 1;
    private static final float COUNT_FACTOR_F = 1f;

    private static final boolean POINT_SPRITE = true;
    private static final ParticleMesh.Type EMITTER_TYPE = POINT_SPRITE ? ParticleMesh.Type.Point
            : ParticleMesh.Type.Triangle;

    public void setState(final ModelViewState state) {

        // this.state = state;
        assets = state.getApplication().getAssetManager();
        // ed =
        // state.getApplication().getStateManager().getState(ConnectionState.class).getEntityData();

        createFlame();
        createFlash();
        createSpark();
        createRoundSpark();
        createSmokeTrail();
        createDebris();
        createShockwave();
        explosionEffect.setLocalScale(0.5f);
        state.getApplication().getRenderManager().preloadScene(explosionEffect);

    }

    private void createFlame() {
        flame = new ParticleEmitter("Flame", EMITTER_TYPE, 32 * COUNT_FACTOR);
        flame.setSelectRandomImage(true);
        flame.setStartColor(new ColorRGBA(1f, 0.4f, 0.05f, 1f / COUNT_FACTOR_F));
        flame.setEndColor(new ColorRGBA(.4f, .22f, .12f, 0f));
        flame.setStartSize(1.3f);
        flame.setEndSize(2f);
        flame.setShape(new EmitterSphereShape(Vector3f.ZERO, 1f));
        flame.setParticlesPerSec(0);
        flame.setGravity(0, -5, 0);
        flame.setLowLife(.4f);
        flame.setHighLife(.5f);
        flame.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 7, 0));
        flame.getParticleInfluencer().setVelocityVariation(1f);
        flame.setImagesX(2);
        flame.setImagesY(2);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/flame.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        flame.setMaterial(mat);
        explosionEffect.attachChild(flame);
    }

    private void createFlash() {
        flash = new ParticleEmitter("Flash", EMITTER_TYPE, 24 * COUNT_FACTOR);
        flash.setSelectRandomImage(true);
        flash.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, 1f / COUNT_FACTOR_F));
        flash.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        flash.setStartSize(.1f);
        flash.setEndSize(3.0f);
        flash.setShape(new EmitterSphereShape(Vector3f.ZERO, .05f));
        flash.setParticlesPerSec(0);
        flash.setGravity(0, 0, 0);
        flash.setLowLife(.2f);
        flash.setHighLife(.2f);
        flash.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 5f, 0));
        flash.getParticleInfluencer().setVelocityVariation(1);
        flash.setImagesX(2);
        flash.setImagesY(2);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/flash.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        flash.setMaterial(mat);
        explosionEffect.attachChild(flash);
    }

    private void createRoundSpark() {
        roundspark = new ParticleEmitter("RoundSpark", EMITTER_TYPE, 20 * COUNT_FACTOR);
        roundspark.setStartColor(new ColorRGBA(1f, 0.29f, 0.34f, (float) (1.0 / COUNT_FACTOR_F)));
        roundspark.setEndColor(new ColorRGBA(0, 0, 0, 0.5f / COUNT_FACTOR_F));
        roundspark.setStartSize(1.2f);
        roundspark.setEndSize(1.8f);
        roundspark.setShape(new EmitterSphereShape(Vector3f.ZERO, 2f));
        roundspark.setParticlesPerSec(0);
        roundspark.setGravity(0, -.5f, 0);
        roundspark.setLowLife(1.8f);
        roundspark.setHighLife(2f);
        roundspark.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 3, 0));
        roundspark.getParticleInfluencer().setVelocityVariation(.5f);
        roundspark.setImagesX(1);
        roundspark.setImagesY(1);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/roundspark.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        roundspark.setMaterial(mat);
        explosionEffect.attachChild(roundspark);
    }

    private void createSpark() {
        spark = new ParticleEmitter("Spark", ParticleMesh.Type.Triangle, 30 * COUNT_FACTOR);
        spark.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, 1.0f / COUNT_FACTOR_F));
        spark.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        spark.setStartSize(.5f);
        spark.setEndSize(.5f);
        spark.setFacingVelocity(true);
        spark.setParticlesPerSec(0);
        spark.setGravity(0, 5, 0);
        spark.setLowLife(1.1f);
        spark.setHighLife(1.5f);
        spark.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 20, 0));
        spark.getParticleInfluencer().setVelocityVariation(1);
        spark.setImagesX(1);
        spark.setImagesY(1);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/spark.png"));
        spark.setMaterial(mat);
        explosionEffect.attachChild(spark);
    }

    private void createSmokeTrail() {
        smoketrail = new ParticleEmitter("SmokeTrail", ParticleMesh.Type.Triangle, 22 * COUNT_FACTOR);
        smoketrail.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, 1.0f / COUNT_FACTOR_F));
        smoketrail.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        smoketrail.setStartSize(.2f);
        smoketrail.setEndSize(1f);

//        smoketrail.setShape(new EmitterSphereShape(Vector3f.ZERO, 1f));
        smoketrail.setFacingVelocity(true);
        smoketrail.setParticlesPerSec(0);
        smoketrail.setGravity(0, 1, 0);
        smoketrail.setLowLife(.4f);
        smoketrail.setHighLife(.5f);
        smoketrail.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 12, 0));
        smoketrail.getParticleInfluencer().setVelocityVariation(1);
        smoketrail.setImagesX(1);
        smoketrail.setImagesY(3);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/smoketrail.png"));
        smoketrail.setMaterial(mat);
        explosionEffect.attachChild(smoketrail);
    }

    private void createDebris() {
        debris = new ParticleEmitter("Debris", ParticleMesh.Type.Triangle, 15 * COUNT_FACTOR);
        debris.setSelectRandomImage(true);
        debris.setRandomAngle(true);
        debris.setRotateSpeed(FastMath.TWO_PI * 4);
        debris.setStartColor(new ColorRGBA(1f, 0.59f, 0.28f, 1.0f / COUNT_FACTOR_F));
        debris.setEndColor(new ColorRGBA(.5f, 0.5f, 0.5f, 0f));
        debris.setStartSize(.2f);
        debris.setEndSize(.2f);

//        debris.setShape(new EmitterSphereShape(Vector3f.ZERO, .05f));
        debris.setParticlesPerSec(0);
        debris.setGravity(0, 12f, 0);
        debris.setLowLife(1.4f);
        debris.setHighLife(1.5f);
        debris.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 15, 0));
        debris.getParticleInfluencer().setVelocityVariation(.60f);
        debris.setImagesX(3);
        debris.setImagesY(3);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/Debris.png"));
        debris.setMaterial(mat);
        explosionEffect.attachChild(debris);
    }

    private void createShockwave() {
        shockwave = new ParticleEmitter("Shockwave", ParticleMesh.Type.Triangle, 1 * COUNT_FACTOR);
//        shockwave.setRandomAngle(true);
        shockwave.setFaceNormal(Vector3f.UNIT_Y);
        shockwave.setStartColor(new ColorRGBA(.48f, 0.17f, 0.01f, .8f / COUNT_FACTOR_F));
        shockwave.setEndColor(new ColorRGBA(.48f, 0.17f, 0.01f, 0f));

        shockwave.setStartSize(0f);
        shockwave.setEndSize(7f);

        shockwave.setParticlesPerSec(0);
        shockwave.setGravity(0, 0, 0);
        shockwave.setLowLife(0.5f);
        shockwave.setHighLife(0.5f);
        shockwave.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0, 0));
        shockwave.getParticleInfluencer().setVelocityVariation(0f);
        shockwave.setImagesX(1);
        shockwave.setImagesY(1);
        final Material mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Explosion/shockwave.png"));
        shockwave.setMaterial(mat);
        explosionEffect.attachChild(shockwave);
    }

    public Spatial createExplosion() {
        return explosionEffect.deepClone();
    }

//    @Override
//    public void simpleUpdate(float tpf) {
//        time += tpf / speed;
//        if (time > 1f && state2 == 0) {
//            flash.emitAllParticles();
//            spark.emitAllParticles();
//            smoketrail.emitAllParticles();
//            debris.emitAllParticles();
//            shockwave.emitAllParticles();
//            state2++;
//        }
//        if (time > 1f + .05f / speed && state2 == 1) {
//            flame.emitAllParticles();
//            roundspark.emitAllParticles();
//            state2++;
//        }
//
//        // rewind the effect
//        if (time > 5 / speed && state2 == 2) {
//            state2 = 0;
//            time = 0;
//
//            flash.killAllParticles();
//            spark.killAllParticles();
//            smoketrail.killAllParticles();
//            debris.killAllParticles();
//            flame.killAllParticles();
//            roundspark.killAllParticles();
//            shockwave.killAllParticles();
//        }
//    }
}