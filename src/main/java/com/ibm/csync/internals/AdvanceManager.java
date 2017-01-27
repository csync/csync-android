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

package com.ibm.csync.internals;

import com.ibm.csync.CSKey;
import com.ibm.csync.CSValue;
import com.ibm.csync.internals.response.AdvanceResponse;
import com.ibm.csync.internals.response.FetchResponse;
import com.ibm.csync.internals.websocket.CSTransport;

import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

public class AdvanceManager {
  //Constructor Objects
  private final CSTransport transport;
  private final DBManager db;
  private double VTS_BACKOFF_PERCENT = 0.02;
  private RvtsPrime largestVtsSeen;

  public AdvanceManager(final CSTransport transport, final DBManager db,
      final Scheduler scheduler, RvtsPrime largestVtsSeen) {
    this.transport = transport;
    this.db = db;
    this.largestVtsSeen = largestVtsSeen;
  }

  public Observable<CSValue> advanceValues(final CSKey key) {
      return Observable.create(new AdvanceOnSubscribe(key, 0L, AdvanceManager.this));
  }

  public Observable<AdvanceResponse> sendAdvanceRequest(final CSKey csKey) {
    return db.getRvtsAsync(csKey)
        .flatMap(new Func1<Long, Observable<AdvanceResponse>>() {
            @Override
            public Observable<AdvanceResponse> call(final Long rvts) {
                return transport.advance(csKey, rvts);
            }
    });
  }

  public Observable<List<Long>> vtsToFetch(List<Long> vts) {
      return db.vtsToFetch(vts);
  }

  public Observable<FetchResponse> sendFetchRequest(List<Long> vts) {
      return transport.fetch(vts);
  }

  public void upsertRvts(final CSKey key, final Long vts) {
      db.upsertRvts(key, vts);
  }
}

