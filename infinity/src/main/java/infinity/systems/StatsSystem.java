/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package infinity.systems;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;

import infinity.server.GameServer;

/**
 *
 * @author asser
 */
public class StatsSystem extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(StatsSystem.class);
    /**
     * the interval between logging stats to console
     */
    public static final int LOGINTERVALMS = 10000;
    private final GameServer server;
    private Timer timer;
    SimTime localTime;

    public StatsSystem(final GameServer server) {
        this.server = server;
    }

    @Override
    protected void initialize() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new Task(server), LOGINTERVALMS, LOGINTERVALMS);
    }

    @Override
    protected void terminate() {
        return;
    }

    @Override
    public void update(final SimTime tpf) {
        localTime = tpf;
    }

    @Override
    public void start() {
        return;
    }

    @Override
    public void stop() {
        return;
    }

    class Task extends TimerTask {

        private final GameServer localServer;

        public Task(final GameServer server) {
            localServer = server;
        }

        int count = 1;

        // run is a abstract method that defines task performed at scheduled time.
        @Override
        public void run() {
            localServer.logStats();
        }
    }

}
