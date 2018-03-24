/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package example.es.ship.weapons;

import com.simsilica.es.EntityComponent;

/**
 * A component that allows an entity to perform attacks with guns
 * @author Asser
 */
public class Guns implements EntityComponent{
    
    long cooldown;
    int cost;
    int level;

    public Guns(long cooldown, int cost, int level) {
        this.cooldown = cooldown;
        this.cost = cost;
        this.level = level;
    }

    public long getCooldown() {
        return cooldown;
    }

    public int getCost() {
        return cost;
    }

    public int getLevel() {
        return level;
    }
}