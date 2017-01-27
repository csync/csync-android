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
import com.ibm.csync.internals.sqlite.DirtyTableEntry;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.functions.Func0;

/** An in-memory implementation of DBManager; good for mocking */
public class MemoryDBManager implements DBManager {
  private static final String TAG = MemoryDBManager.class.getName();

  private final List<RVTSInfo> rvtsTable = new ArrayList<>();
  private final List<CSValue> CSValueTable = new ArrayList<>();
  private final List<DirtyTableEntry> dirtyTable = new ArrayList<>();

  private long currentId = 0L;

  @Override public Observable<List<Long>> vtsToFetch(List<Long> vts) {
    return Observable.just(vts);
  }

  //TODO: Remove the commented out code after defining what no cache means.
  @Override public synchronized long getRvts(CSKey csKey) {
    //for (RVTSInfo r : rvtsTable) {
    //  if (r.csKey.equals(csKey) && (r.acl.equals(acl.rawAcl()))) {
    //    return r.rvts;
    //  }
    //}
    return 0L; // no value found
  }

  @Override public Observable<Long> getRvtsAsync(final CSKey csKey) {
    return Observable.defer(new Func0<Observable<Long>>() {
      @Override public Observable<Long> call() {
        return Observable.just(getRvts(csKey));
      }
    });
  }

  @Override public synchronized void upsertRvts(CSKey csKey, long rvts) {
    //for (RVTSInfo r : rvtsTable) {
    //  if (r.csKey.equals(csKey) && r.acl.equals(acl.rawAcl())) {
    //    // found previous entry, update it
    //    r.rvts = rvts;
    //    return;
    //  }
    //}
    //
    //// no previous entry in table, insert it
    //rvtsTable.add(new RVTSInfo(csKey, acl, rvts));
  }

  @Override public synchronized void addData(CSValue csValue) {
    //Log.d(TAG, "sync manager adding CSValue: " + csValue);
    //CSValueTable.add(csValue);
  }

  @Override public Observable<CSValue> cachedValues(final CSKey csKey, final long maxVts) {
    return Observable.empty();
    //return Observable.create(new Observable.OnSubscribe<CSValue>() {
    //  @Override
    //  public void call(Subscriber<? super CSValue> subscriber) {
    //    for (CSValue csValue : CSValueTable) {
    //      if (csValue.key().matches(csKey)) {
    //        subscriber.onNext(csValue);
    //      }
    //    }
    //    subscriber.onCompleted();
    //  }
    //});
  }

  @Override
  public long addDirty(CSValue csValue) {
    //long tempId = currentId;
    //Log.d(TAG, "Adding dirty CSValue to database: " + csValue);
    //dirtyTable.add(new DirtyTableEntry(tempId, csValue));
    //currentId++;
    //return currentId;
    return 0L;
  }

  @Override
  public void deleteDirty(long id) {
    //int tableSize = dirtyTable.size();
    //DirtyTableEntry curData;
    //for (int i = 0; i < tableSize; i++) {
    //  curData = dirtyTable.get(i);
    //  if (curData.id == id) {
    //    dirtyTable.remove(i);
    //    Log.d(TAG, "Removing dirty CSValue from database: " + curData);
    //    break;
    //  }
    //}
  }

  @Override
  public List<DirtyTableEntry> getDirtyData() {
    return dirtyTable;
  }

  @Override public void closeDB() {
    //TODO: remove all from the db and start clean.
  }

  private static class RVTSInfo {
    final CSKey csKey;
    final CSAcl acl;
    Long rvts;

    RVTSInfo(CSKey csKey, CSAcl acl, Long rvts) {
      this.csKey = csKey;
      this.acl = acl;
      this.rvts = rvts;
    }
  }
}
