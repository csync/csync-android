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

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.csync.internals.response.ResponseEnvelope;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;
import rx.Observable;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

public class OkHttpWebSocketConnection implements WebSocketConnection {
  private Response connectResponse;
  private BehaviorSubject<ResponseEnvelope> messages = BehaviorSubject.create();
  private WebSocketCall call;
  private LockingWebSocket socket;

  //TODO: Can the requests be seen as an Observable<Request>
  public OkHttpWebSocketConnection(Request request, OkHttpClient client) {
    this.call = WebSocketCall.create(client, request);
    start();
  }

  public static Request buildConnectRequest(String host, int port, boolean useSSL,
      String authProvider, String authToken) {
    String secureWebsocket = useSSL ? "wss://" : "ws://";

    StringBuilder s = new StringBuilder("");
    s.append(secureWebsocket);
    s.append(host);
    s.append(":");
    s.append(Integer.toString(port));
    s.append("/connect");
    s.append("?authProvider=");
    s.append(authProvider);
    s.append("&token=");
    s.append(authToken);
    s.append("&sessionId=");
    s.append(UUID.randomUUID().toString());

    return new Request.Builder().url(s.toString()).build();
  }

  public Response getResponse() {
    return connectResponse;
  }

  public Observable<Boolean> sendMessage(String message) {
    return Observable.just(RequestBody.create(WebSocket.TEXT, message))
        .map(new Func1<RequestBody, Boolean>() {
          @Override public Boolean call(RequestBody requestBody) {
            try {
              socket.sendMessage(requestBody);
              return true;
            } catch (IOException e) {
              throw Exceptions.propagate(e);
            }
          }
        });
  }

  @Override public Observable<Boolean> disconnect() {
    try {
      socket.close(0, "User requests disconnect.");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Observable.empty();
  }

  @Override public Observable<ResponseEnvelope> messages() {
    return messages.asObservable();
  }

  private void start() {
    final Gson gson = new Gson();
    final Type responseEnvelopeType = new TypeToken<ResponseEnvelope>() {
    }.getType();
    final CountDownLatch latch = new CountDownLatch(1);

    call.enqueue(new WebSocketListener() {
      @Override public void onOpen(WebSocket webSocket, Response response) {
        connectResponse = response;
        socket = new LockingWebSocket(webSocket);
        latch.countDown();
      }

      @Override public void onFailure(IOException e, Response response) {

      }

      @Override public void onMessage(ResponseBody message) throws IOException {
        String responseString = message.string();
        message.close();
        System.out.println("[recv] " + responseString);
        final ResponseEnvelope response = gson.fromJson(responseString, responseEnvelopeType);

        messages.onNext(response);
      }

      @Override public void onPong(Buffer payload) {

      }

      @Override public void onClose(int code, String reason) {
        Log.e("CLOSED", code + " " + reason);
        messages.onCompleted();
        latch.countDown();
      }
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Class that synchronizes writes to websocket
   */
  private static class LockingWebSocket implements WebSocket {
    private final WebSocket webSocket;

    public LockingWebSocket(WebSocket webSocket) {
      this.webSocket = webSocket;
    }

    @Override
    public synchronized void sendMessage(RequestBody message) throws IOException {
      webSocket.sendMessage(message);
    }

    @Override
    public synchronized void sendPing(Buffer payload) throws IOException {
      webSocket.sendPing(payload);
    }

    @Override
    public void close(int code, String reason) throws IOException {
      webSocket.close(code, reason);
    }
  }
}