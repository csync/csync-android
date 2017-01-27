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
import java.util.List;
import rx.Observable;

/** Abstract def. of database ops required by Pickles for CSValue caching and syncing */
public interface DBManager {
  long NO_RVTS_FOUND = 0L;

  /* rvts_log operations */

  Observable<List<Long>> vtsToFetch(List<Long> vts);

  long getRvts(CSKey csKey);

  Observable<Long> getRvtsAsync(CSKey csKey);

  void upsertRvts(CSKey csKey, long rvts);

  /* data_log  operations */
  void addData(CSValue csValue);

  Observable<CSValue> cachedValues(CSKey csKey, long maxVts);

  /* dirty_log operations  */
  long addDirty(CSValue csValue);

  void deleteDirty(long id);

  List<DirtyTableEntry> getDirtyData();

  void closeDB();
}
