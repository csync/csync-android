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

package com.ibm.csync.internals.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.csync.CSAuthData;
import com.ibm.csync.CSKey;
import com.ibm.csync.CSValue;
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.internals.request.Advance;
import com.ibm.csync.internals.request.CSRequest;
import com.ibm.csync.internals.request.Fetch;
import com.ibm.csync.internals.request.Pub;
import com.ibm.csync.internals.request.RequestEnvelope;
import com.ibm.csync.internals.request.Sub;
import com.ibm.csync.internals.request.Unsub;
import com.ibm.csync.internals.response.AdvanceResponse;
import com.ibm.csync.internals.response.CSValueDeserializer;
import com.ibm.csync.internals.response.FetchResponse;
import com.ibm.csync.internals.response.Happy;
import com.ibm.csync.internals.response.ResponseEnvelope;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/*
 * CSRequest  -- contains the details of the request
 *      example: { "path" : [ "root", "a", "b" ], "data" : "abc" }
 * CSRequest.ResponseEnvelope -- contains the request and some CSValue about it
 *      example:
 *          { "kind" : "write",
 *            "closure" : { "id" : 100 },
 *            "payload" : { "path" : [ "root", "a", "b" ], "data" : "abc" }
 *          }
 * CSResponse  -- contains the details of the response
 *      example: { "code" : 4, "msg" : "path not found" }
 * CSResponse.ResponseEnvelope -- contains the response and some CSValue about it
 *     example:
 *         { "kind" : "sad",
 *           "closure" : { "id" : 100 },
 *           "payload" : { "code" : 4, "msg" : "path not found" }
 *         }
 * Command -- contains the request and tells us if a response is needed and how to handle it
 *      needsResponse() : true / false
 *      handleResponse(status, CSResponse.ResponseEnvelope)
 *  Result -- contains the original command, the response envelope, and the Status
 */

/*
 * The low-level pickles interface
 *
 * Exposes its functionality using 3 streams
 *
 * Observable<Command> toServer -- commands to pass to server
 * Observer<Result> resultsToClient -- results to pass to client
 * Observer<CSValue> dataToClient -- CSValue to pass to client
 *
 * CSTransport observes the toServer stream and relays its commands
 * to the server. If the WebSocket connection is down, the commands
 * will be queued internally and a background thread tries to drain
 * the queue when the connection is established.
 *
 * Requests that need acknowledgement are kept awaiting responses.
 *
 * A timeout mechanism (not implemented yet) will be used but
 * no attempt is made to guarantee command delivery at this level.
 * Errors and timeouts are reported to the upper level and it's up
 * to them to retry if appropriate.
 *
 * Messages arriving from the server are parsed and segregated into
 * either response or CSValue messages.
 *
 * CSResponse messages are matched with their pending requests in order
 * to produce Result objects that are fed on the resultsToClient stream
 *
 * CSValue messages are fed to the dataToClient stream without further
 * processing.
 */
public class CSTransport {
  public static final int MESSAGE_VERSION = 15;
  private static final AtomicLong nextId = new AtomicLong(0);
  private static Gson gson;
  private WebSocketConnection socketConnection;

  public CSTransport(WebSocketConnection webSocketConnection) {
    this.socketConnection = webSocketConnection;

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(CSValue.class, new CSValueDeserializer());
    gson = gsonBuilder.create();
  }

  public void disconnect() {
    socketConnection.disconnect();
  }

  public Observable<CSValue> liveValues(final CSKey csKey) {
    return socketConnection.messages()
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("data");
          }
        })
        .map(new Func1<ResponseEnvelope, CSValue>() {
          @Override public CSValue call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, CSValue.class);
          }
        })
        .filter(new Func1<CSValue, Boolean>() {
          @Override public Boolean call(CSValue csValue) {
            return csValue.key().matches(csKey);
          }
        });
  }

  public Observable<ResponseEnvelope> send(final CSRequest request) {
    final long closure = nextId.incrementAndGet();
    RequestEnvelope envelope = request.toEnvelope(closure);
    final String requestString = gson.toJson(envelope);

    socketConnection
        .sendMessage(requestString)
        .subscribeOn(Schedulers.io())
        .subscribe(new Action1<Boolean>() {
          @Override public void call(Boolean aBoolean) {
            System.out.println("[send] [" + aBoolean + "] " + requestString);
          }
        });

    return socketConnection
        .messages()
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.closure != null;
          }
        })
        .first(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.closure == closure;
          }
        });
  }

  public Observable<CSAuthData> authData() {
    return socketConnection.messages()
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("connectResponse");
          }
        })
        .map(new Func1<ResponseEnvelope, CSAuthData>() {
          @Override public CSAuthData call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, CSAuthData.class);
          }
        })
        .first();
  }

  public Observable<Happy> write(final CSKey csKey, final String data, final CSAcl acl) {
    final CSRequest request =
        new Pub(System.currentTimeMillis(), csKey.toArray(), data, false, acl.rawAcl());

    return send(request)
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("happy");
          }
        })
        .map(new Func1<ResponseEnvelope, Happy>() {
          @Override public Happy call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, Happy.class);
          }
        });
  }

  public Observable<Happy> listen(final CSKey csKey) {
    final CSRequest request = new Sub(csKey.toArray());

    return send(request)
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("happy");
          }
        })
        .map(new Func1<ResponseEnvelope, Happy>() {
          @Override public Happy call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, Happy.class);
          }
        });
  }

  public Observable<Happy> unlisten(final CSKey csKey) {
    final CSRequest request = new Unsub(csKey.toArray());

    return send(request)
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("happy");
          }
        })
        .map(new Func1<ResponseEnvelope, Happy>() {
          @Override public Happy call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, Happy.class);
          }
        });
  }

  public Observable<AdvanceResponse> advance(final CSKey csKey, final long rvts) {
    final CSRequest request = new Advance(csKey.toArray(), rvts);

    return send(request)
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("advanceResponse");
          }
        })
        .map(new Func1<ResponseEnvelope, AdvanceResponse>() {
          @Override public AdvanceResponse call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, AdvanceResponse.class);
          }
        });
  }

    public Observable<FetchResponse> fetch(final List<Long> rvts) {
        final CSRequest request = new Fetch(rvts);

        return send(request)
                .filter(new Func1<ResponseEnvelope, Boolean>() {
                    @Override public Boolean call(ResponseEnvelope responseEnvelope) {
                        return responseEnvelope.kind.equals("fetchResponse");
                    }
                })
                .map(new Func1<ResponseEnvelope, FetchResponse>() {
                    @Override public FetchResponse call(ResponseEnvelope responseEnvelope) {
                        return gson.fromJson(responseEnvelope.payload, FetchResponse.class);
                    }
                });
    }

  public Observable<Happy> delete(final CSKey csKey) {
    final CSRequest request =
        new Pub(System.currentTimeMillis(), csKey.toArray(), null, true, null);

    return send(request)
        .filter(new Func1<ResponseEnvelope, Boolean>() {
          @Override public Boolean call(ResponseEnvelope responseEnvelope) {
            return responseEnvelope.kind.equals("happy");
          }
        })
        .map(new Func1<ResponseEnvelope, Happy>() {
          @Override public Happy call(ResponseEnvelope responseEnvelope) {
            return gson.fromJson(responseEnvelope.payload, Happy.class);
          }
        });
  }

  public Observable<CSAcl> getAcls() {
    return Observable.just(CSAcl.PRIVATE, CSAcl.PUBLIC_CREATE, CSAcl.PUBLIC_READ,
        CSAcl.PUBLIC_READ_CREATE, CSAcl.PUBLIC_READ_WRITE, CSAcl.PUBLIC_READ_WRITE_CREATE,
        CSAcl.PUBLIC_WRITE, CSAcl.PUBLIC_WRITE_CREATE);
  }
}