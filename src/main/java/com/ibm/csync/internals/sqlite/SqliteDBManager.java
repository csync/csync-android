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

package com.ibm.csync.internals.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import com.ibm.csync.CSKey;
import com.ibm.csync.CSValue;
import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.internals.DBManager;
import com.ibm.csync.internals.query.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import static com.ibm.csync.internals.query.Predicate.eq;
import static com.ibm.csync.internals.query.Predicate.gt;
import static com.ibm.csync.internals.query.Predicate.le;
import static com.ibm.csync.internals.query.Query.select;

public class SqliteDBManager extends SQLiteOpenHelper implements DBManager {
  final static String pathNames[];
  private static final String TAG = SqliteDBManager.class.getName();
  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "ClientLog.db";
  private static final String TEXT_TYPE = " TEXT";
  private static final String INT_TYPE = " INTEGER";
  private static final String COMMA_SEP = ",";
  private static final int DEFAULT_QUERY_LIMIT = 100;

  static {
    pathNames = new String[16];
    for (int i = 0; i < pathNames.length; i++) {
      final String s = String.format("path%d", i);
      pathNames[i] = s;
    }
  }

  private final SQLiteDatabase db;

  public SqliteDBManager(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    db = this.getWritableDatabase();
  }

  public SqliteDBManager(Context context, String databaseName) {
    super(context, databaseName, null, DATABASE_VERSION);
    db = this.getWritableDatabase();
  }

  private static Predicate getPatternPred(final CSKey csKey) {
    final String[] paths = csKey.toArray();

    boolean endsInStar = false;

    Predicate p = Predicate.empty();
        /*
         * x     x_
         * x.*   x*_
         * *.x   *x_
         * x.*.* x**_
         *
         * a.b.c    (p0 = 'a') and (p1 = 'b') and (p2 = 'c') and (p3 is null)
         * a.b.*    (p0 = 'a') and (b1 = 'b') and (p2 is not null) and (p3 is null)
         * a.*.c    (p0 = 'a') and (b2 = 'c') and (p3 is null)
         *
         * a.*.#    (p0 = 'a') and (b2 is not null)
         */

    for (int i = 0; i < paths.length; i++) {
      final String part = paths[i].trim();
      endsInStar = false;

      if ("*".equals(part)) {
        endsInStar = true;
        continue;
      } else if ("#".equals(part)) {
        break;
      } else {
        p = eq(String.format("path%d", i), paths[i]).and(p);
      }
    }

    if (endsInStar) {
      p = p.and(Predicate.isNotNull(String.format("path%d", paths.length - 1)));
    }

    Log.d(TAG, "query string " + p.toString());

    return p;
  }

  @Override public void onCreate(SQLiteDatabase db) {
    SQLiteStatement createDataTable = db.compileStatement(DataTable.Statements.CREATE_TABLE);
    SQLiteStatement createRvtsTable = db.compileStatement(RvtsTable.Statements.CREATE_TABLE);
    SQLiteStatement createDirtyTable = db.compileStatement(DirtyTable.Statements.CREATE_TABLE);

    createDataTable.execute();
    createRvtsTable.execute();
    createDirtyTable.execute();
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    SQLiteStatement deleteDataTable = db.compileStatement(DataTable.Statements.DELETE_TABLE);
    SQLiteStatement deleteRvtsTable = db.compileStatement(RvtsTable.Statements.DELETE_TABLE);
    SQLiteStatement deleteDirtyTable = db.compileStatement(DirtyTable.Statements.DELETE_TABLE);

    deleteDataTable.execute();
    deleteRvtsTable.execute();
    deleteDirtyTable.execute();

    onCreate(db);
  }

  @Override public void closeDB() {
    db.close();
  }

  //INSERT INTO dataTable (path, ....) VALUES ('path', ....);
  @Override
  public void addData(CSValue csValue) {

    final ContentValues newDataLog = new ContentValues();

    final String[] paths = csValue.key().toArray();
    final int nSet = Math.min(paths.length, 16);
    final int nNull = 16 - nSet;
    for (int i = 0; i < nSet; i++) {
      final String name = String.format("path%d", i);
      //Log.d(TAG, "set " + name + " to " + paths[i]);
      newDataLog.put(name, paths[i]);
    }
    //for (int i = 0; i < nNull; i++) {
    //  final String name = String.format("path%d", i + nSet);
    //  //Log.d(TAG, "set " + name + " to null");
    //}
    //newDataLog.put(DataTable.Columns.COLUMN_NAME_PATH, CSValue.getKey().toString());
    newDataLog.put(DataTable.Columns.VTS, csValue.vts());  // vts
    newDataLog.put(DataTable.Columns.CTS, csValue.cts());  // cts
    newDataLog.put(DataTable.Columns.DATA, csValue.data()); // insert CSValue
    newDataLog.put(DataTable.Columns.ACL, csValue.acl().rawAcl());  // acl

    // Inserting Row
    long insertRow = db.insertWithOnConflict(DataTable.Properties.TABLE_NAME, null, newDataLog,
        SQLiteDatabase.CONFLICT_IGNORE);
    Log.d(TAG, "SQL Adding CSValue: for row: " + insertRow);
  }

  @Override
  public Observable<CSValue> cachedValues(final CSKey csKey, final long maxVts) {
    return Observable.create(new Observable.OnSubscribe<CSValue>() {
      @Override
      public void call(Subscriber<? super CSValue> subscriber) {

        Cursor cursor;
        long currentMinVts = maxVts;

        boolean lastDataBlock = false;
        do {
          cursor = db.rawQuery(createDataQuery(csKey, currentMinVts, DEFAULT_QUERY_LIMIT), null);
          if (cursor.getCount() != DEFAULT_QUERY_LIMIT) {
            lastDataBlock = true;
          }

          // looping through all rows and adding to list
          if (cursor.moveToFirst()) {
            do {
              //Adding CSValue to outputStream observer
              CSValue csValue = transformCursorToData(cursor);
              currentMinVts = csValue.vts();
              subscriber.onNext(csValue);
            } while (cursor.moveToNext());
          }
        } while (!lastDataBlock);
        subscriber.onCompleted();
        cursor.close();
      }
    }).onBackpressureBuffer();
  }

  //SELECT * FROM data_table WHERE vts <= maxVts AND WHERE CSKey LIKE CSKey% ORDER BY vts DESC
  //       LIMIT queryLimit
  private String createDataQuery(CSKey csKey, long maxVts, int queryLimit) {
    final String q = select().
        from(DataTable.Properties.TABLE_NAME).
        where(csKey.toQuery(pathNames), le(DataTable.Columns.VTS, maxVts)).
        orderByDesc(DataTable.Columns.VTS).
        limit(queryLimit).toString();

    Log.d(TAG, q);

    return q;
  }

  @Override public Observable<List<Long>> vtsToFetch(List<Long> vts) {
    return Observable.just(getDataForVtsArray(vts));
  }

  //TODO: Refactor
  public List<Long> getDataForVtsArray(List<Long> vtsArray) {
    List<Long> temp = vtsArray;

    StringBuilder sb =
        new StringBuilder("SELECT " + DataTable.Columns.VTS + " FROM "
            + DataTable.Properties.TABLE_NAME + " WHERE "
            + DataTable.Columns.VTS + " IN (");

    for (int i = 0; i < vtsArray.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(Long.toString(vtsArray.get(i)));
    }

    sb.append(")");

    Cursor cursor = db.rawQuery(sb.toString(), null);

    try {
      // looping through all rows and adding to list
      if (cursor.moveToFirst()) {
        do {
          long vts = cursor.getLong(0);
          for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i) == vts) {
              temp.remove(i);
            }
          }
        } while (cursor.moveToNext());
      }
    }
    finally {
      cursor.close();
    }

    return temp;
  }

  //SELECT rvts FROM rvtsTable WHERE CSKey = 'CSKey' AND acl = 'acl';
  @Override
  public long getRvts(CSKey csKey) {
    long rvtsInDB;

    SQLiteStatement getRvts = db.compileStatement(RvtsTable.Statements.GET_RVTS);
    getRvts.bindString(1, csKey.toString());

    try {
      rvtsInDB = getRvts.simpleQueryForLong();
    } catch (SQLiteDoneException name) {
      rvtsInDB = NO_RVTS_FOUND;
    }

    return rvtsInDB;
  }

  @Override public Observable<Long> getRvtsAsync(final CSKey csKey) {
    return Observable.defer(new Func0<Observable<Long>>() {
      @Override public Observable<Long> call() {
        return Observable.just(getRvts(csKey))
            .subscribeOn(Schedulers.io());
      }
    });
  }

  //INSERT INTO rvtsTable (CSKey, acl, rvts) VALUES ('CSKey', 'acl', rvts);
  //UPDATE rvtsTable SET rvts = rvts WHERE CSKey = 'CSKey' and acl = 'acl' ;
  @Override
  public void upsertRvts(CSKey csKey, long rvts) {
    ContentValues newRvts = new ContentValues();
    newRvts.put(RvtsTable.Columns.PATH, csKey.toString());
    newRvts.put(RvtsTable.Columns.RVTS, rvts);

    // Inserting Row
    db.insertWithOnConflict(RvtsTable.Properties.TABLE_NAME, null, newRvts,
        SQLiteDatabase.CONFLICT_REPLACE);
    Log.d(TAG, "SQL Adding Rvts for " + csKey);
  }

  private CSValue transformCursorToData(Cursor dataCursor) {
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      final String part = dataCursor.getString(i);
      if (part == null) break;
      parts.add(part);
    }
    String[] path = parts.toArray(new String[parts.size()]);
    long vts = dataCursor.getLong(16);
    long cts = dataCursor.getLong(17);
    String data = dataCursor.getString(18);
    String acl = dataCursor.getString(19);

    return new CSValue(CSKey.make(path), data, cts, vts, CSAcl.customAcl(acl), false);
  }

  @Override
  public long addDirty(CSValue csValue) {
    //INSERT INTO dirtyTable (path, ....) VALUES ('path', ....);
    ContentValues newDirty = new ContentValues();
    newDirty.put(DirtyTable.Columns.PATH, csValue.key().toString());
    newDirty.put(DirtyTable.Columns.VTS, csValue.vts());  // vts
    newDirty.put(DirtyTable.Columns.CTS, csValue.cts());  // cts
    newDirty.put(DirtyTable.Columns.DATA, csValue.data()); // insert CSValue
    newDirty.put(DirtyTable.Columns.ACL, csValue.acl().rawAcl());  // acl

    // Inserting Row
    long insertRow =
        db.insertWithOnConflict(DirtyTable.Properties.TABLE_NAME, null, newDirty,
            SQLiteDatabase.CONFLICT_IGNORE);
    Log.d(TAG, "SQL Adding Dirty CSValue: for row: " + insertRow);

    return insertRow;
  }

  // SELECT * FROM data_log
  @Override
  public List<DirtyTableEntry> getDirtyData() { //TODO: change this method to return Observable?
    List<DirtyTableEntry> dirtyTableEntryList = new ArrayList<>();

    Cursor cursor = db.rawQuery(DirtyTable.Statements.ALL_DIRTYDATA, null);

    // looping through all rows and adding to list
    if (cursor.moveToFirst()) {
      do {
        long rowId = cursor.getLong(0);
        String[] path = cursor.getString(1).split("/");
        long vts = cursor.getLong(2);
        long cts = cursor.getLong(3);
        String data = cursor.getString(4);
        String acl = cursor.getString(5);

        CSValue csValue = new CSValue(CSKey.make(path), data, cts, vts, CSAcl.customAcl(acl), false);

        DirtyTableEntry dirtyTableEntry = new DirtyTableEntry(rowId, csValue);
        // Adding contact to list
        dirtyTableEntryList.add(dirtyTableEntry);
      } while (cursor.moveToNext());
    }

    cursor.close();
    // return contact list
    return dirtyTableEntryList;
  }

  @Override
  public void deleteDirty(long id) {
    SQLiteStatement deleteDirty = db.compileStatement(DirtyTable.Statements.DELETE_DIRTY);
    deleteDirty.bindLong(1, id);

    //TODO: Should use executeUpdateDelete() but requires API 11.
    deleteDirty.execute();
  }
}
