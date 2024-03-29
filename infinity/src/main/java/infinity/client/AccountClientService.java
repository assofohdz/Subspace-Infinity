/*
 * $Id$
 *
 * Copyright (c) 2017, Simsilica, LLC
 * All rights reserved.
 */

package infinity.client;

import infinity.net.AccountSession;
import infinity.net.AccountSessionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.*;

import com.jme3.network.service.AbstractClientService;
import com.jme3.network.service.ClientServiceManager;
import com.jme3.network.service.rmi.RmiClientService;


/**
 *  Provides super-basic account services like logging in.  This could
 *  be expanded to be more complicated based on a real game's needs.
 *  The basics have been included here as a minimal example that includes
 *  the basic types of communication necessary.
 *
 *  @author    Paul Speed
 */
public class AccountClientService extends AbstractClientService
    implements AccountSession {

  static Logger log = LoggerFactory.getLogger(AccountClientService.class);

  private RmiClientService rmiService;
  private AccountSession delegate;

  private String playerName;

  private AccountSessionCallback sessionCallback = new AccountSessionCallback();
  private List<AccountSessionListener> listeners = new CopyOnWriteArrayList<>();

  public AccountClientService() {
  }

  @Override
  public String getServerInfo() {
    return delegate.getServerInfo();
  }

  @Override
  public void login( String playerName ) {
    this.playerName = playerName;
    delegate.login(playerName);
  }

  /**
   *  Adds a listener that will be notified about account-related events.
   *  Note that these listeners are called on the networking thread and
   *  as such are not suitable for modifying the visualization directly.
   */
  public void addAccountSessionListener( AccountSessionListener l ) {
    listeners.add(l);
  }

  public void removeAccountSessionListener( AccountSessionListener l ) {
    listeners.remove(l);
  }

  @Override
  protected void onInitialize( ClientServiceManager s ) {
    log.debug("onInitialize(" + s + ")");
    this.rmiService = getService(RmiClientService.class);
    if( rmiService == null ) {
      throw new RuntimeException("AccountClientService requires RMI service");
    }
    log.debug("Sharing session callback.");
    rmiService.share(sessionCallback, AccountSessionListener.class);
  }

  /**
   *  Called during connection setup once the server-side services have been initialized
   *  for this connection and any shared objects, etc. should be available.
   */
  @Override
  public void start() {
    log.debug("start()");
    super.start();
    this.delegate = rmiService.getRemoteObject(AccountSession.class);
    log.debug("delegate:" + delegate);
    if( delegate == null ) {
      throw new RuntimeException("No account session found during connection setup");
    }
  }

  /**
   *  Shared with the server over RMI so that it can notify us about account
   *  related stuff.
   */
  private class AccountSessionCallback implements AccountSessionListener {

    @Override
    public void notifyLoginStatus( boolean loggedIn ) {
      log.trace("notifyLoginStatus(" + loggedIn + ")");
      for( AccountSessionListener l : listeners ) {
        l.notifyLoginStatus(loggedIn);
      }
    }

  }
}


