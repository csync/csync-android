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

import android.content.Context;
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.android.BuildConfig;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class) @Config(constants = BuildConfig.class, sdk = 21)
public class CSAppTest {
  public static final String HOST = BuildConfig.CSYNC_HOST;
  public static final int PORT = BuildConfig.CSYNC_PORT;
  public static final String PROVIDER = BuildConfig.CSYNC_DEMO_PROVIDER;
  private static final String TOKEN = BuildConfig.CSYNC_DEMO_TOKEN;
  private CSApp app;
  private CSAcl ACL = CSAcl.PUBLIC_READ_WRITE_CREATE;

  @Before public void setUp() {
    Context context = RuntimeEnvironment.application.getApplicationContext();
    app = new CSApp.Builder(HOST, PORT).useSSL(false).build();

    app.authenticate(PROVIDER, TOKEN);
  }

  @After public void tearDown() {
    //app.unauthenticate();
  }

  @Test public void testWriteApostrophe() {
    TestSubscriber<CSValue> listenSubscriber = new TestSubscriber<>();
    TestSubscriber<Boolean> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<Boolean> deleteSubscriber = new TestSubscriber<>();

    CSKey key = CSKey.fromString("tests.android").uuidChild();
    app.listen(key).first().subscribe(listenSubscriber);

    String apostrophe = "\'";
    app.write(key, apostrophe, ACL).subscribe(writeSubscriber);

    app.delete(key).subscribe(deleteSubscriber);

    writeSubscriber.awaitTerminalEvent();

    listenSubscriber.awaitTerminalEvent();
    listenSubscriber.assertNoErrors();

    deleteSubscriber.awaitTerminalEvent();

    List<CSValue> receivedValues = listenSubscriber.getOnNextEvents();
    assertEquals(1, receivedValues.size());

    CSValue receivedValue = receivedValues.get(0);
    assertTrue(receivedValue.key().matches(key));
    assertTrue(receivedValue.acl().rawAcl().equals(ACL.rawAcl()));
    assertTrue(receivedValue.data().equals(apostrophe));
  }

  @Test public void testDelete() {
    TestSubscriber<CSValue> listenSubscriber = new TestSubscriber<>();
    TestSubscriber<Boolean> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<Boolean> deleteSubscriber = new TestSubscriber<>();

    CSKey key = CSKey.fromString("tests.android").uuidChild();
    app.listen(key).take(2).subscribe(listenSubscriber);

    String apostrophe = "\'";
    app.write(key, apostrophe, ACL).subscribe(writeSubscriber);
    writeSubscriber.awaitTerminalEvent();

    app.delete(key).subscribe(deleteSubscriber);

    deleteSubscriber.awaitTerminalEvent();

    listenSubscriber.awaitTerminalEvent();
    listenSubscriber.assertNoErrors();

    assertEquals(true, deleteSubscriber.getOnNextEvents().get(0).booleanValue());

    List<CSValue> receivedValues = listenSubscriber.getOnNextEvents();
    assertEquals(2, receivedValues.size());

    CSValue deleted = receivedValues.get(1);

    assertTrue(deleted.key().matches(key));
    assertEquals(true, receivedValues.get(1).isKeyDeleted());
  }

  @Test public void testKeyWithMoreThan16Components() {
    CSKey individualTestKey =
        CSKey.fromString("tests.android.4.5.6.7.8.9.10.11.12.13.14.15.16.17.18");

    TestSubscriber<Boolean> writeSubscriber = new TestSubscriber<>();

    app.write(individualTestKey, "testing", ACL).subscribe(writeSubscriber);

    writeSubscriber.awaitTerminalEvent();
    writeSubscriber.assertNoErrors();
    writeSubscriber.assertCompleted();

    assertFalse(writeSubscriber.getOnNextEvents().get(0).booleanValue());
  }

  @Test public void testListenWithAdvance() {
    TestSubscriber<CSValue> listenSubscriber = new TestSubscriber<>();
    TestSubscriber<Boolean> writeSubscriber = new TestSubscriber<>();
    TestSubscriber<Boolean> deleteSubscriber = new TestSubscriber<>();

    CSKey key = CSKey.fromString("tests.android").uuidChild();

    String data = "testing1234";
    app.write(key, data, ACL).subscribe(writeSubscriber);

    writeSubscriber.awaitTerminalEvent();

    app.listen(key).first().subscribe(listenSubscriber);
    listenSubscriber.awaitTerminalEvent();
    listenSubscriber.assertNoErrors();

    List<CSValue> receivedValues = listenSubscriber.getOnNextEvents();
    assertEquals(1, receivedValues.size());

    CSValue receivedValue = receivedValues.get(0);
    assertTrue(receivedValue.key().matches(key));
    assertTrue(receivedValue.acl().rawAcl().equals(ACL.rawAcl()));
    assertTrue(receivedValue.data().equals(data));

    app.delete(key).subscribe(deleteSubscriber);
    deleteSubscriber.awaitTerminalEvent();
  }
}
