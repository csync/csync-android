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

import com.ibm.csync.internals.response.ResponseEnvelope;
import java.io.IOException;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ws.WebSocket;
import okio.Buffer;
import rx.Observable;
import rx.subjects.PublishSubject;

public class MockWebSocketConnection implements WebSocketConnection {
  private Response connectResponse;
  private PublishSubject<ResponseEnvelope> messages = PublishSubject.create();

  public MockWebSocketConnection() {

  }

  private WebSocket socket() {
    WebSocket mockedSocket = new WebSocket() {
      @Override public void sendMessage(RequestBody message) throws IOException {
        messages.onNext(new ResponseEnvelope());
      }

      @Override public void sendPing(Buffer payload) throws IOException {

      }

      @Override public void close(int code, String reason) throws IOException {
        messages.onCompleted();
      }
    };

    return mockedSocket;
  }

  @Override public Observable<Boolean> sendMessage(String message) {
    return null;
  }

  @Override public Observable<Boolean> disconnect() {
    return null;
  }

  @Override public Observable<ResponseEnvelope> messages() {
    return messages;
  }
}
