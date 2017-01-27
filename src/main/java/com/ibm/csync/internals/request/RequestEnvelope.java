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

package com.ibm.csync.internals.request;

import com.ibm.csync.internals.websocket.CSTransport;

public class RequestEnvelope {
  public final int version = CSTransport.MESSAGE_VERSION;
  public final String kind;
  public final Long closure;
  public final CSRequest payload;

  public transient final long validUntil;

  public RequestEnvelope(final Long closure, final CSRequest payload) {
    this.validUntil = System.currentTimeMillis() + 10000;   // TODO: make timeout dynamic
    this.kind = (payload == null) ? null : payload.getKind().name();
    this.closure = closure;
    this.payload = payload;
  }
}
