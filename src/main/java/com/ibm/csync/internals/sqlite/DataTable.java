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

final class DataTable {

  private DataTable() {
    throw new AssertionError();
  }

  static abstract class Properties {
    static final String TABLE_NAME = "data_log";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
  }

  static abstract class Columns implements BaseColumns {
    static final String PATH0 = "path0";
    static final String PATH1 = "path1";
    static final String PATH2 = "path2";
    static final String PATH3 = "path3";
    static final String PATH4 = "path4";
    static final String PATH5 = "path5";
    static final String PATH6 = "path6";
    static final String PATH7 = "path7";
    static final String PATH8 = "path8";
    static final String PATH9 = "path9";
    static final String PATH10 = "path10";
    static final String PATH11 = "path11";
    static final String PATH12 = "path12";
    static final String PATH13 = "path13";
    static final String PATH14 = "path14";
    static final String PATH15 = "path15";
    static final String VTS = "vts";
    static final String CTS = "cts";
    static final String DATA = "CSValue";
    static final String ACL = "acl";
  }

  static abstract class Statements {
    /** CREATE TABLE data_log ( path, vts, cts, CSValue, acl, PRIMARY KEY (path , acl) ); */
    static final String CREATE_TABLE =
        "CREATE TABLE " + Properties.TABLE_NAME + " (" +
            Columns.PATH0 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH1 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH2 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH3 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH4 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH5 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH6 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH7 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH8 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH9 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH10 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH11 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH12 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH13 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH14 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.PATH15 + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.VTS + Properties.INT_TYPE + Properties.COMMA_SEP +
            Columns.CTS + Properties.INT_TYPE + Properties.COMMA_SEP +
            Columns.DATA + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            Columns.ACL + Properties.TEXT_TYPE + Properties.COMMA_SEP +
            " PRIMARY KEY (" + Columns.VTS + ") )";

    /** DELETE TABLE IF EXISTS data_log */
    static final String DELETE_TABLE =
        "DROP TABLE IF EXISTS " + Properties.TABLE_NAME;

    /** DELETE FROM data_log WHERE ACL = ? */
    static final String DELETE_BY_ACL =
        "DELETE FROM " + Properties.TABLE_NAME + " WHERE " + Columns.ACL + " = ?";

    static final String GET_DATA_FOR_VTS =
        "SELECT * FROM " + Properties.TABLE_NAME + " WHERE " + Columns.VTS + " = ?";
  }
}
