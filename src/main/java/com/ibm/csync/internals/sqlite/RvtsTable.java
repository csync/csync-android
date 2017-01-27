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

final class RvtsTable {

  private RvtsTable() {
    throw new AssertionError();
  }

  static abstract class Properties {
    static final String TABLE_NAME = "rvts_log";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
  }

  static abstract class Columns implements BaseColumns {
    static final String PATH = "path";
    static final String RVTS = "rvts";
  }

  static abstract class Statements {
    /** CREATE TABLE rvts_log ( path, rvts , PRIMARY KEY (path, vts) ); */
    static final String CREATE_TABLE =
        "CREATE TABLE " + Properties.TABLE_NAME + " (" +
            Columns.PATH + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.RVTS + Properties.INT_TYPE + Properties.COMMA_SEP +
            " PRIMARY KEY (" + Columns.PATH + ") )";

    /** SELECT rvts FROM rvts_log WHERE path = ? */
    static final String GET_RVTS = "SELECT " + Columns.RVTS + " FROM " + Properties.TABLE_NAME +
        " WHERE " + Columns.PATH + " = ? ";

    /** DROP TABLE IF EXISTS rvts_log */
    static final String DELETE_TABLE =
        "DROP TABLE IF EXISTS " + Properties.TABLE_NAME;
  }
}
