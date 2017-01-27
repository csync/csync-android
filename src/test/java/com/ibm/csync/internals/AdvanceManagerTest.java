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
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.android.BuildConfig;
import com.ibm.csync.internals.response.Happy;
import com.ibm.csync.internals.websocket.CSTransport;
import com.ibm.csync.internals.websocket.OkHttpWebSocketConnection;
import com.ibm.csync.internals.websocket.WebSocketConnection;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AdvanceManagerTest {
  public static final String HOST = BuildConfig.CSYNC_HOST;
  public static final int PORT = BuildConfig.CSYNC_PORT;
  public static final String PROVIDER = BuildConfig.CSYNC_DEMO_PROVIDER;
  private static final String TOKEN = BuildConfig.CSYNC_DEMO_TOKEN;
  private final CSKey androidTestKey = CSKey.fromString("tests.android");
  private AdvanceManager advanceManager;
  private Request request =
      OkHttpWebSocketConnection.buildConnectRequest(HOST, PORT, false, PROVIDER, TOKEN);
  private WebSocketConnection connection =
      new OkHttpWebSocketConnection(request, new OkHttpClient());
  private CSTransport transport = new CSTransport(connection);

  @Before public void setUp() {
    RvtsPrime rvtsPrime = new RvtsPrime();

    advanceManager =
        new AdvanceManager(transport, new MemoryDBManager(), Schedulers.immediate(), rvtsPrime);
  }

  @Test public void testAdvanceValues() throws Exception {

    CSKey androidTestKey = CSKey.make("tests", "android").uuidChild();

    TestScheduler scheduler = new TestScheduler();

    TestSubscriber<Happy> writeSubscriber = new TestSubscriber();
    TestSubscriber<CSValue> advanceSubscriber = new TestSubscriber();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    transport.write(androidTestKey, "testData", CSAcl.PUBLIC_READ_WRITE_CREATE)
        .subscribe(writeSubscriber);

    writeSubscriber.awaitTerminalEvent();
    writeSubscriber.assertNoErrors();
    writeSubscriber.assertCompleted();

    assertEquals(0, writeSubscriber.getOnNextEvents().get(0).code);

    advanceManager.advanceValues(androidTestKey).first().subscribe(advanceSubscriber);

    scheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

    advanceSubscriber.awaitTerminalEvent();
    advanceSubscriber.assertCompleted();
    advanceSubscriber.assertNoErrors();

    CSValue valueFromAdvance = advanceSubscriber.getOnNextEvents().get(0);

    assertTrue(valueFromAdvance.key().matches(androidTestKey));
    assertEquals("$publicReadWriteCreate", valueFromAdvance.acl().rawAcl());
    assertEquals(false, valueFromAdvance.isKeyDeleted());
    assertEquals("testData", valueFromAdvance.data());

    transport.delete(androidTestKey).subscribe(deleteSubscriber);
    deleteSubscriber.awaitTerminalEvent();
  }


  /*@Test
  public void testHandleAdvanceResponse() throws Exception {

  }*/
}