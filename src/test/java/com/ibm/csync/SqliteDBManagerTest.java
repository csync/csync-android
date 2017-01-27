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

package com.ibm.csync.android;

import android.content.Context;
import com.ibm.csync.CSKey;
import com.ibm.csync.CSValue;
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.internals.sqlite.DirtyTableEntry;
import com.ibm.csync.internals.sqlite.SqliteDBManager;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SqliteDBManagerTest {
  SqliteDBManager sqlite;
  private long NO_RVTS_FOUND = 0L;

  private String TEST_DATA = "This is a test CSValue string";
  private CSKey csKey = CSKey.make("rooms", "public", "msg1");
  private CSAcl ACL = CSAcl.PUBLIC_READ_WRITE_CREATE;

  private CSValue CSValue1;
  private CSValue CSValue2;

  @Before public void setUp() {
    Context context = RuntimeEnvironment.application.getApplicationContext();
    sqlite = new SqliteDBManager(context, null);

    CSValue1 = new CSValue(csKey, "TEST CSValue string", 2222, 1111, ACL, false);

    CSValue2 = new CSValue(csKey, "Change Test CSValue String", 2222, 1111, ACL, false);
  }

  @After public void tearDown() {
    sqlite.close();
  }

  //test inserting a new rvts.
  @Test public void testInsertRvts() {
    long testRvts = 1234;

    assertEquals(NO_RVTS_FOUND, sqlite.getRvts(csKey));
    sqlite.upsertRvts(csKey, testRvts);
    assertEquals(testRvts, sqlite.getRvts(csKey));
  }

  //test updating a previously inserted rvts.
  @Test public void testGetAndUpsertRvts() {
    long testRvts = 1111;
    long updatedTestRvts = 2222;

        /* 1. Assert getRvts() returns NO_RVTS_FOUND with empty database. */
    assertEquals(NO_RVTS_FOUND, sqlite.getRvts(csKey));

        /* 2. Assert upsertRvts() returns row id 1 when inserting first rvts.
        *       and check that getRvts() returns the inserted Rvts */
    sqlite.upsertRvts(csKey, testRvts);
    assertEquals(testRvts, sqlite.getRvts(csKey));

        /* 3. Assert upsertRvts() returns row id 1 when inserting first rvts.
        *       and check that getRvts() returns the updated Rvts */
    sqlite.upsertRvts(csKey, updatedTestRvts);
    assertEquals(updatedTestRvts, sqlite.getRvts(csKey));

    //TODO: test inserting null items.
  }

  //testGetData asserts that CSValue is not retrieved until a subscription to the Observable from
  // cachedValues
  @Test public void testGetData() {
    //CSValue1.vts() = 1111;

    TestSubscriber<CSValue> dataSubscriber = new TestSubscriber<>();

    Observable<CSValue> dbObservable = sqlite.cachedValues(csKey, Long.MAX_VALUE);
    sqlite.addData(CSValue1);

    List<CSValue> CSValueFromDB = dataSubscriber.getOnNextEvents();
    assertEquals(0, CSValueFromDB.size());

    dbObservable.subscribe(dataSubscriber);

    CSValueFromDB = dataSubscriber.getOnNextEvents();
    assertEquals(1, CSValueFromDB.size());

    assertDataEquals(CSValueFromDB.get(0), CSValue1);
  }

  //tests addData() ignores a second insert with the same vts timestamp.
  @Test public void testAddData() {
        /* 1. Add CSValue to the db and assert that cachedValues() returns this new inserted CSValue */
    TestSubscriber<CSValue> subscriber1 = new TestSubscriber<>();

    sqlite.addData(CSValue1);
    sqlite.cachedValues(csKey, Long.MAX_VALUE).subscribe(subscriber1);

    List<CSValue> CSValueFromDB = subscriber1.getOnNextEvents();
    assertEquals(1, CSValueFromDB.size());
    assertDataEquals(CSValueFromDB.get(0), CSValue1);

        /* 2. Try to overwrite inserted CSValue from step 1 and assert that the overwrite is ignored */
    TestSubscriber<CSValue> subscriber2 = new TestSubscriber<>();

    sqlite.addData(CSValue2);
    sqlite.cachedValues(csKey, Long.MAX_VALUE).subscribe(subscriber2);

    CSValueFromDB = subscriber2.getOnNextEvents();
    assertEquals(1, CSValueFromDB.size());
    assertTrue(!CSValueFromDB.get(0).data().equals(CSValue2));
    assertDataEquals(CSValueFromDB.get(0), CSValue1);
  }

  //@Test public void testGetCount() {
  //  //CSValue1.vts = 1111;
  //  //CSValue2.vts = 2222;
  //
  //  //CSValue1.data = "First Test CSValue string";
  //  //CSValue2.data = "Second Test CSValue string";
  //  CSKey key3 = CSKey.make("different", "path");
  //
  //  CSValue CSValue3 = new CSValue(key3, "Second Test CSValue String", 4444, 3333, ACL, false);
  //
  //      /* 1. Assert getCount() returns 0 with a rvts range = 0 or with invalid arguments. */
  //  assertEquals(0, sqlite.getCount(csKey, 0, 0, ACL));
  //  assertEquals(0, sqlite.getCount(csKey, Long.MIN_VALUE, 0, ACL));
  //  assertEquals(0, sqlite.getCount(csKey, Long.MIN_VALUE, Long.MAX_VALUE, ACL));
  //  assertEquals(0, sqlite.getCount(csKey, -1, -2, ACL));
  //
  //      /* 2. Add CSValue to the db and assert that getCount() returns 1 */
  //  sqlite.addData(CSValue1);
  //  assertEquals(1, sqlite.getCount(csKey, 0, Long.MAX_VALUE, ACL));
  //
  //      /* 3. Add a second CSValue to the db assert that getCount returns 2 */
  //  sqlite.addData(CSValue2);
  //  assertEquals(2, sqlite.getCount(csKey, 0, Long.MAX_VALUE, ACL));
  //
  //      /* 4. Add third CSValue from different path and assert getCount only returns 1 now with 3
  //      CSValue items in the db */
  //  sqlite.addData(CSValue3);
  //  assertEquals(1, sqlite.getCount(CSValue3.key(), 0, Long.MAX_VALUE, ACL));
  //}

  //@Test public void testGetAndAddDirty() {
  //  //TODO: test inserting null items.
  //
  //      /* 1. Assert getDirtyData() is empty upon db instantiation */
  //  assertTrue(sqlite.getDirtyData().isEmpty());
  //
  //      /* 2. Add CSValue to the dirty table and assert that getDirtyData() returns this new inserted
  //          dirtyData */
  //  sqlite.addDirty(CSValue1);
  //  List<DirtyTableEntry> dirtyTableEntry = sqlite.getDirtyData();
  //  assertEquals(1, dirtyTableEntry.size());
  //  assertEquals(1, dirtyTableEntry.get(0).id);
  //  assertDataEquals(dirtyTableEntry.get(0).csValue, CSValue1);
  //}

  //@Test public void testDeleteDirtyData() {
  //      /* 1. Assert deleteDirty() with invalid arguments performs without crashes. */
  //  sqlite.deleteDirty(Long.MIN_VALUE);
  //  sqlite.deleteDirty(Long.MAX_VALUE);
  //
  //      /* 2. Add CSValue to the dirty table and assert that getDirtyData() returns this new inserted
  //          dirtyData */
  //  sqlite.addDirty(CSValue1);
  //  int firstId = 1;
  //  List<DirtyTableEntry> dirtyTableEntry = sqlite.getDirtyData();
  //  assertEquals(1, dirtyTableEntry.size());
  //  assertEquals(firstId, dirtyTableEntry.get(0).id);
  //  assertDataEquals(dirtyTableEntry.get(0).csValue, CSValue1);
  //
  //  sqlite.deleteDirty(firstId);
  //  assertTrue(sqlite.getDirtyData().isEmpty());
  //}

  private void assertDataEquals(CSValue d1, CSValue d2) {
    assertEquals(d1.acl().rawAcl(), d2.acl().rawAcl());
    assertEquals(d1.cts(), d2.cts());
    assertEquals(d1.data(), d2.data());
    assertEquals(d1.isKeyDeleted(), d2.isKeyDeleted());
    assertTrue(d1.key().matches(d2.key()));
    assertEquals(d1.vts(), d2.vts());
  }
}
