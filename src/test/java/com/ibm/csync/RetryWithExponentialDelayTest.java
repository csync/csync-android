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

import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.internals.RetryWithExponentialDelay;
import com.ibm.csync.internals.response.Happy;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;

public class RetryWithExponentialDelayTest {
  private int retryCount;
  private CSValue testValue = new CSValue(CSKey.make("f"), "data", 0, 0, CSAcl.PUBLIC_READ_WRITE_CREATE, false);

  @Before public void setUp() throws Exception {
    retryCount = 0;
  }

  @Test public void testRetryWhenSuccessAfterTwoRetries() {
    TestSubscriber subscriber = new TestSubscriber();
    int maxRetries = 4;
    final int failedAttempts = 2;

    Observable.just(testValue)
        .doOnNext(new Action1<CSValue>() {
          @Override public void call(CSValue csValue) {
            retryCount++;
          }
        })
        .flatMap(new Func1<CSValue, Observable<?>>() {
          @Override public Observable<?> call(CSValue csValue) {
            if (retryCount < failedAttempts) {
              return Observable.error(new Throwable());
            }
            return Observable.just(new Happy());          }
        })
        .retryWhen(new RetryWithExponentialDelay(maxRetries, 500))
        .subscribe(subscriber);

    assertEquals(2, retryCount);
    assertEquals(1, subscriber.getOnNextEvents().size());
  }

  @Test public void testRetryWhenReachesMaxRetries() {
    TestSubscriber subscriber = new TestSubscriber();
    int maxRetries = 3;

    Observable.just(testValue)
        .doOnNext(new Action1<CSValue>() {
          @Override public void call(CSValue csValue) {
            retryCount++;
          }
        })
        .flatMap(new Func1<CSValue, Observable<?>>() {
          @Override public Observable<?> call(CSValue csValue) {
            return Observable.error(new Throwable());
          }
        })
        .retryWhen(new RetryWithExponentialDelay(maxRetries, 500))
        .subscribe(subscriber);

    assertEquals(maxRetries, retryCount);
    assertEquals(0, subscriber.getOnNextEvents().size());
  }
}