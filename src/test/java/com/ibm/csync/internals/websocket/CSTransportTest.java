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

import com.ibm.csync.CSKey;
import com.ibm.csync.CSValue;
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.android.BuildConfig;
import com.ibm.csync.internals.response.AdvanceResponse;
import com.ibm.csync.internals.response.FetchResponse;
import com.ibm.csync.internals.response.Happy;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;

public class CSTransportTest {
  private static final String HOST = BuildConfig.CSYNC_HOST;
  private static final int PORT = BuildConfig.CSYNC_PORT;
  private static final String PROVIDER = BuildConfig.CSYNC_DEMO_PROVIDER;
  private static final String TOKEN = BuildConfig.CSYNC_DEMO_TOKEN;
  private final CSKey androidTestKey = CSKey.fromString("tests.android");

  private Request request =
      OkHttpWebSocketConnection.buildConnectRequest(HOST, PORT, false, PROVIDER, TOKEN);
  private WebSocketConnection connection =
      new OkHttpWebSocketConnection(request, new OkHttpClient());
  private CSTransport transport = new CSTransport(connection);

  @Test public void testLiveValues() {
    TestSubscriber<Happy> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> listenSubscriber = new TestSubscriber<>();
    TestSubscriber<CSValue> liveSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    CSKey testKey = androidTestKey.uuidChild();

    transport.liveValues(testKey).first().subscribe(liveSubscriber);

    transport.listen(testKey).subscribe(listenSubscriber);

    transport.write(testKey, "testLiveValues", CSAcl.PUBLIC_READ_WRITE_CREATE)
        .subscribe(writeSubscriber);

    transport.delete(testKey).subscribe(deleteSubscriber);

    writeSubscriber.awaitTerminalEvent();
    writeSubscriber.assertNoErrors();
    writeSubscriber.assertCompleted();

    listenSubscriber.awaitTerminalEvent();
    listenSubscriber.assertCompleted();
    listenSubscriber.assertNoErrors();

    liveSubscriber.awaitTerminalEvent();
    liveSubscriber.assertNoErrors();
    liveSubscriber.assertCompleted();

    deleteSubscriber.awaitTerminalEvent();

    CSValue value = liveSubscriber.getOnNextEvents().get(0);
    assertEquals("testLiveValues", value.data());
    assertEquals(CSAcl.PUBLIC_READ_WRITE_CREATE.rawAcl(), value.acl().rawAcl());
  }

  @Test public void testWrite() throws Exception {
    TestSubscriber<Happy> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    CSKey testKey = androidTestKey.uuidChild();

    transport.write(testKey, "testData", CSAcl.PUBLIC_READ_WRITE_CREATE).subscribe(writeSubscriber);

    transport.delete(testKey).subscribe(deleteSubscriber);

    writeSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);
    writeSubscriber.assertNoErrors();
    writeSubscriber.assertCompleted();

    deleteSubscriber.awaitTerminalEvent();

    assertEquals(1, writeSubscriber.getOnNextEvents().size());
    Happy happyResponse = writeSubscriber.getOnNextEvents().get(0);
    assertEquals("OK", happyResponse.msg);
    assertEquals(0, happyResponse.code);
  }

  @Test public void testListen() throws Exception {
    TestSubscriber<Happy> listenSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    CSKey testKey = androidTestKey.uuidChild();

    transport.listen(testKey).subscribe(listenSubscriber);

    transport.delete(testKey).subscribe(deleteSubscriber);

    listenSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);
    listenSubscriber.assertNoErrors();
    listenSubscriber.assertCompleted();

    deleteSubscriber.awaitTerminalEvent();

    assertEquals(1, listenSubscriber.getOnNextEvents().size());
    Happy happyResponse = listenSubscriber.getOnNextEvents().get(0);
    assertEquals("OK", happyResponse.msg);
    assertEquals(0, happyResponse.code);
  }

  @Test public void testUnlisten() throws Exception {
    TestSubscriber<Happy> unListenSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    CSKey testKey = androidTestKey.uuidChild();

    transport.unlisten(testKey).subscribe(unListenSubscriber);

    transport.delete(testKey).subscribe(deleteSubscriber);

    unListenSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);

    unListenSubscriber.assertNoErrors();
    unListenSubscriber.assertCompleted();

    deleteSubscriber.awaitTerminalEvent();

    assertEquals(1, unListenSubscriber.getOnNextEvents().size());
    Happy happyResponse = unListenSubscriber.getOnNextEvents().get(0);
    assertEquals("OK", happyResponse.msg);
    assertEquals(0, happyResponse.code);
  }

  @Test public void testAdvance() throws Exception {
    TestSubscriber<Happy> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<AdvanceResponse> advanceSubscriber = new TestSubscriber<>();
    TestSubscriber<FetchResponse> fetchSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    CSKey testKey = androidTestKey.uuidChild();

    transport.write(testKey, "testAdvance", CSAcl.PUBLIC_READ_WRITE_CREATE)
        .subscribe(writeSubscriber);

    transport.advance(testKey, 0)
        .subscribe(advanceSubscriber);

    writeSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);
    advanceSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);

    advanceSubscriber.assertNoErrors();
    advanceSubscriber.assertCompleted();

    transport.fetch(advanceSubscriber.getOnNextEvents().get(0).vts).subscribe(fetchSubscriber);

    fetchSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);
    fetchSubscriber.assertNoErrors();
    fetchSubscriber.assertCompleted();

    assertEquals(1, advanceSubscriber.getOnNextEvents().size());
    assertEquals("$publicReadWriteCreate", fetchSubscriber.getOnNextEvents().get(0).response[0].acl().rawAcl());

    transport.delete(testKey).subscribe(deleteSubscriber);
    deleteSubscriber.awaitTerminalEvent();
  }

  @Test
  public void testDelete() throws Exception {
    TestSubscriber<Happy> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<Happy> deleteSubscriber = new TestSubscriber<>();

    CSKey testKey = androidTestKey.uuidChild();

    transport
        .write(testKey, "dataToDelete", CSAcl.PUBLIC_READ_WRITE_CREATE)
        .subscribe(writeSubscriber);

    transport
        .delete(testKey)
        .subscribe(deleteSubscriber);

    writeSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);
    writeSubscriber.assertNoErrors();
    writeSubscriber.assertCompleted();

    deleteSubscriber.awaitTerminalEvent(3000, TimeUnit.MILLISECONDS);
    deleteSubscriber.assertNoErrors();
    deleteSubscriber.assertCompleted();

    assertEquals(1, deleteSubscriber.getOnNextEvents().size());
    Happy happyResponse = deleteSubscriber.getOnNextEvents().get(0);

    assertEquals("OK", happyResponse.msg);
    assertEquals(0, happyResponse.code);
  }
}