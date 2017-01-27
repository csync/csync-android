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

import android.provider.BaseColumns;

final class DirtyTable {

  private DirtyTable() {
    throw new AssertionError();
  }

  static abstract class Properties {
    static final String TABLE_NAME = "dirty_log";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
  }

  static abstract class Columns implements BaseColumns {
    static final String PATH = "path";
    static final String VTS = "vts";
    static final String CTS = "cts";
    static final String DATA = "CSValue";
    static final String ACL = "acl";
  }

  static abstract class Statements {
    /**
     * CREATE TABLE dirty_log ( _id INTEGER PRIMARY KEY, path, vts, cts, CSValue, acl, PRIMARY KEY
     * (path , acl) );
     */
    static final String CREATE_TABLE =
        "CREATE TABLE " + Properties.TABLE_NAME + " (" +
            Columns._ID + " INTEGER PRIMARY KEY " + Properties.COMMA_SEP +
            Columns.PATH + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.VTS + Properties.INT_TYPE + Properties.COMMA_SEP +
            Columns.CTS + Properties.INT_TYPE + Properties.COMMA_SEP +
            Columns.DATA + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.ACL + Properties.TEXT_TYPE + " )";

    /** DROP TABLE IF EXISTS dirty_log */
    static final String DELETE_TABLE =
        "DROP TABLE IF EXISTS " + Properties.TABLE_NAME;

    /** SELECT * FROM dirty_log */
    static final String ALL_DIRTYDATA = "SELECT  * FROM " + Properties.TABLE_NAME;

    /** DELETE FROM dirty_log WHERE ._ID = ? */
    static final String DELETE_DIRTY =
        "DELETE FROM " + Properties.TABLE_NAME + " WHERE " + Columns._ID + " = ?";
  }
}
