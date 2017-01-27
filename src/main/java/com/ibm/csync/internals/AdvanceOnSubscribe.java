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

import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

public class AdvanceOnSubscribe implements Observable.OnSubscribe<CSValue> {
  private static final long advancePeriod = 100000;    //in ms

  private final CSKey key;
  private final long delay;
  private final AdvanceManager advanceManager;

  AdvanceOnSubscribe(CSKey key, long delay, AdvanceManager advanceManager) {
    this.key = key;
    this.delay = delay;
    this.advanceManager = advanceManager;
  }

  @Override public void call(final Subscriber<? super CSValue> subscriber) {
    //TODO: upsertRvts has to be after addData into the DB.
    Observable
        .timer(delay, TimeUnit.MILLISECONDS)
        .flatMap(new Func1<Long, Observable<AdvanceResponse>>() {
          @Override public Observable<AdvanceResponse> call(Long tick) {
            return advanceManager.sendAdvanceRequest(key);
          }
        })
            //This is the fetch
        .flatMap(new Func1<AdvanceResponse, Observable<CSValue>>() {
            @Override
            public Observable<CSValue> call(final AdvanceResponse advanceResponse) {
                return Observable.just(advanceResponse.vts)//.subscribeOn(Schedulers.io())
                .flatMap(new Func1<List<Long>, Observable<List<Long>>>() {
                    @Override
                    public Observable<List<Long>> call(List<Long> vts) {
                        return advanceManager.vtsToFetch(vts);
                    }
                })
                // Now we have a list of vts we need to fetch
                .flatMap(new Func1<List<Long>, Observable<FetchResponse>>() {
                    @Override public Observable<FetchResponse> call(List<Long> vts) {
                        return advanceManager.sendFetchRequest(vts);
                    }
                })
                // Now we have the new fetch values
                .doOnNext(new Action1<FetchResponse>() {
                    @Override
                    public void call(FetchResponse fetchResponse) {
                        advanceManager.upsertRvts(key, advanceResponse.maxvts);

                        final long delay1 = (advanceResponse.vts.size() == 0) ? advancePeriod : 0;
                        Observable
                                .create(new AdvanceOnSubscribe(key, delay1, advanceManager))
                                .subscribe(subscriber);
                    }
                })
                .flatMap(new Func1<FetchResponse, Observable<CSValue>>() {
                    @Override public Observable<CSValue> call(FetchResponse fetchResponse) {
                        return Observable.from(fetchResponse.response);
                    }
                });
            }
        })
        .subscribe(subscriber);
  }
}
