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

import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class RetryWithExponentialDelay implements
    Func1<Observable<? extends Throwable>, Observable<?>> {

  private final int maxRetries;
  private final int startDelayMillis;

  public RetryWithExponentialDelay(final int maxRetries, final int startDelayMillis) {
    this.maxRetries = maxRetries;
    this.startDelayMillis = startDelayMillis;
  }

  @Override public Observable<?> call(Observable<? extends Throwable> observable) {
    return observable
        .zipWith(Observable.range(1, maxRetries > 0 ? maxRetries : Integer.MAX_VALUE),
            new Func2<Throwable, Integer, Integer>() {
              @Override public Integer call(Throwable throwable, Integer attempt) {
                return attempt;
              }
            })
        .flatMap(new Func1<Integer, Observable<?>>() {
          @Override public Observable<?> call(Integer attempt) {
            long newInterval = startDelayMillis * ((long) attempt * (long) attempt);
            if (newInterval < 0) {
              newInterval = Long.MAX_VALUE;
            }
            // use Schedulers#immediate() to keep on same thread
            return Observable.timer(newInterval, TimeUnit.MILLISECONDS, Schedulers.immediate());
          }
        });
  }
}
