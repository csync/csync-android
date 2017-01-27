/*
 * Copyright IBM Corporation 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.csync;

import android.content.Context;
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.internals.AdvanceManager;
import com.ibm.csync.internals.DBManager;
import com.ibm.csync.internals.MemoryDBManager;
import com.ibm.csync.internals.RetryWithExponentialDelay;
import com.ibm.csync.internals.RvtsPrime;
import com.ibm.csync.internals.SubStateManager;
import com.ibm.csync.internals.response.Happy;
import com.ibm.csync.internals.sqlite.SqliteDBManager;
import com.ibm.csync.internals.websocket.CSTransport;
import com.ibm.csync.internals.websocket.OkHttpWebSocketConnection;
import com.ibm.csync.internals.websocket.WebSocketConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.Schedulers;

public class CSApp {
  private Builder builder;
  private CSAuthData authorizedUser;
  private final Scheduler scheduler;
  private CSTransport transport;
  private DBManager db;
  private AdvanceManager advanceManager;
  private RvtsPrime rvtsPrime;
  private AtomicBoolean isClosed = new AtomicBoolean(true);

  private CSApp(Builder builder) {
    this.builder = builder;
    this.scheduler = builder.scheduler;

    if (builder.inMemoryDB) {
      this.db = new MemoryDBManager();
    } else {
      this.db = new SqliteDBManager(builder.context);
    }
    this.rvtsPrime = new RvtsPrime();
  }

  private static boolean resolveDataOrder(CSValue csValue, Map<CSKey, Long> timeStamps) {
    synchronized (timeStamps) {
      Long latestVts = timeStamps.get(csValue.key());
      return latestVts == null || latestVts < csValue.vts();
    }
  }

  public Observable<CSAuthData> authData() {
    return Observable.defer(new Func0<Observable<CSAuthData>>() {
      @Override public Observable<CSAuthData> call() {
        return Observable.just(authorizedUser);
      }
    });
  }

  public Observable<CSAuthData> authenticate(String authProvider, String token) {
    if (authorizedUser == null) {
      return connect(authProvider, token);
    }
    return Observable.just(authorizedUser);
  }

  public void unauthenticate() {
    db.closeDB();
    transport.disconnect();
    transport = null;
    advanceManager = null;
    isClosed.compareAndSet(false, true);
  }

  private Observable<CSAuthData> connect(String authProvider, String token) {
    Request request = OkHttpWebSocketConnection
        .buildConnectRequest(builder.host, builder.port, builder.useSSL, authProvider, token);

    OkHttpClient client = new OkHttpClient
        .Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build();

    WebSocketConnection connection
        = new OkHttpWebSocketConnection(request, client);

    transport = new CSTransport(connection);

    advanceManager = new AdvanceManager(transport, db, scheduler, rvtsPrime);

    //Observable.from(db.getDirtyData())
    //    .subscribeOn(Schedulers.newThread())
    //    .subscribe(dirtyData -> {
    //      CSValue csValue = dirtyData.csValue;
    //      write(csValue.getKey(), csValue.data, csValue.acl);
    //    });
    isClosed.compareAndSet(true, false);

    //TODO: implement handshake.
    authorizedUser = new CSAuthData(UUID.randomUUID().toString(), authProvider, token, System.currentTimeMillis());
    return Observable.just(authorizedUser);
  }

  /**
   * Listens to any changes on the given CSKey which the user is entitled to with their current
   * ACLs.
   *
   * @param csKey The CSKey to listen to; Read the CSKey class documentation for further information
   * on the wildcards available in CSKey.
   * @return Emits all CSValues for the given CSKey and current user's ACLs.
   */
  public synchronized Observable<CSValue> listen(CSKey csKey) {
    if (isClosed.get()) {
      return Observable.error(new CSyncClosedException());
    }

    //Map of maximum vts for a CSKey delivered to this listener
    final Map<CSKey, Long> timeStamps = new HashMap<>();
    final SubStateManager subStateManager = new SubStateManager(transport, csKey);

    return transport.liveValues(csKey)
        .mergeWith(advanceManager.advanceValues(csKey))
        .mergeWith(db.cachedValues(csKey, Long.MAX_VALUE))
        .filter(new Func1<CSValue, Boolean>() {
          @Override public Boolean call(CSValue csValue) {
            return resolveDataOrder(csValue, timeStamps);
          }
        })
        .doOnNext(new Action1<CSValue>() {
          @Override public void call(CSValue csValue) {
            timeStamps.put(csValue.key(), csValue.vts());
            if (csValue.vts() > rvtsPrime.getRvtsPrime()) {
              rvtsPrime.setRvtsPrime(csValue.vts());
            }
          }
        })
        .doOnNext(new Action1<CSValue>() {
          @Override public void call(CSValue csValue) {
            db.addData(csValue);
          }
        })
        .doOnSubscribe(new Action0() {
          @Override public void call() {
            subStateManager.sub();
          }
        })
        .doOnUnsubscribe(new Action0() {
          @Override public void call() {
            subStateManager.unsub();
          }
        });
  }

  /**
   * write() writes the data to the specified CSKey with the specified ACL. Write() will overwrite
   * the data at the specified key if the key already exists. Otherwise, if the key does not exist
   * write() will create a new key-value pair at the specified CSKey.
   *
   * @param key the write location for the specified data. NOTE: write() with wildcards is not
   * supported.
   * @param data is the data to write
   * @param acl the desired acl for the data at the specified key.
   * @return succesfulWrite [true] the server acknowledges a successful write. In the event there is
   * a conflict resolution (two users write at the same time); the server will return a true result
   * to both users even though only one user's write was the final successful entry.
   */
  public Observable<Boolean> write(CSKey key, String data, CSAcl acl) {
    if (isClosed.get()) {
      return Observable.error(new CSyncClosedException());
    }

    CSValue newCSValue = new CSValue(key, data, 0, 0, acl, false);

    final long id = db.addDirty(newCSValue);

    return Observable.just(newCSValue)
        .flatMap(new Func1<CSValue, Observable<Happy>>() {
          @Override public Observable<Happy> call(CSValue csValue) {
            return transport.write(csValue.key(), csValue.data(), csValue.acl());
          }
        })
        .retryWhen(new RetryWithExponentialDelay(3, 1000))
        .doOnNext(new Action1<Happy>() {
          @Override public void call(Happy happy) {
            db.deleteDirty(id);
          }
        })
        .map(new Func1<Happy, Boolean>() {
          @Override public Boolean call(Happy happy) {
            return happy.code == 0;
          }
        });
  }

  /**
   * delete() deletes the key and value at the specified key.
   *
   * NOTE: Wildcards in the CSKey are NOT supported for delete.
   *
   * @param csKey the key which will be deleted upon completion of this method.
   * @return isDeleted [true] the specified CSKey was successfully deleted from the server.
   */
  public Observable<Boolean> delete(CSKey csKey) {
    if (isClosed.get()) {
      return Observable.error(new CSyncClosedException());
    }

    return Observable.just(csKey)
        .flatMap(new Func1<CSKey, Observable<Happy>>() {
          @Override public Observable<Happy> call(CSKey csKey) {
            return transport.delete(csKey);
          }
        })
        .retry()
        .map(new Func1<Happy, Boolean>() {
          @Override public Boolean call(Happy happy) {
            return happy.code == 0;
          }
        })
        .subscribeOn(scheduler);
  }

  /**
   * Builder constructs a CSApp with the required parameters of the Builder constructer and any
   * optional parameters provided in the Builder's supporting methods. The build() method completes
   * the construction and returns a CSApp.
   */
  public static class Builder {
    //required
    private final String host;
    private final int port;

    //optional
    private Context context;
    private boolean useSSL = true;
    private boolean inMemoryDB = true;
    private int THREAD_POOL_SZ = 4;
    private ThreadFactory threadFactory = new RxThreadFactory("CSync Thread - ");
    private Scheduler scheduler = Schedulers.from(new ScheduledThreadPoolExecutor(THREAD_POOL_SZ, threadFactory));

    /**
     * Builder constructs a CSApp using build(). The required parameters are in the constructor of
     * Builder and any optional parameters are specified in supporting Builder methods.
     *
     * @param host the hostname (url) for the desired CSApp server endpoint.
     * @param port the port number for the desired CSApp server endpoint.
     */
    public Builder(String host, int port) {
      this.host = host;
      this.port = port;
    }

    /**
     * useSSL() forces a secure connection. By default useSSL is true and if a user wishes to use an
     * unsecure connection pass false as the parameter to useSSL().
     *
     * @param useSSL true the connection is forced secure, false the connection is unsecure
     */
    public Builder useSSL(boolean useSSL) {
      this.useSSL = useSSL;
      return this;
    }

    /**
     * Specify the scheduler for CSApp to use for background work (e.g. syncing)
     *
     * @param executorService the executorService CSApp will use to sync and cache data.
     */
    public Builder scheduler(ScheduledExecutorService executorService) {
      this.scheduler = Schedulers.from(executorService);
      return this;
    }

    /**
     * cache creates a SQLiteDatabase for caching on the user's device. Caching aids in offline
     *    access.
     *
     * @param context This context will be used to create a SQLite database for caching data used
     *    during offline access.
     */
    public Builder cache(Context context) {
      this.inMemoryDB = false;
      this.context = context;
      return this;
    }

    /**
     * build() is the final method in the Builder chain and will return a CSApp with the parameters
     * specified in the Builder constructor and supporting Builder methods.
     */
    public CSApp build() {
      return new CSApp(this);
    }
  }
}
