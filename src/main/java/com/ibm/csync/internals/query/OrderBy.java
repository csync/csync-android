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

package com.ibm.csync.internals.query;

import java.util.Arrays;
import rx.functions.Action1;

public final class OrderBy extends Query {
  public final Query query;
  public final boolean desc;
  public final String[] fields;

  public OrderBy(final Query query, final boolean desc, final String... fields) {
    this.query = query;
    this.desc = desc;
    this.fields = fields;
  }

  @Override
  public int hashCode() {
    return query.hashCode() ^ Arrays.hashCode(fields) ^ (desc ? -1 : 0);
  }

  @Override
  public boolean sameAs(final Query other) {
    if (!(other instanceof OrderBy)) return false;
    final OrderBy q = (OrderBy) other;
    return query.equals(q.query) && (desc == q.desc) && Arrays.equals(fields, q.fields);
  }

  @Override
  public StringBuffer fill(final StringBuffer sb) {
    query.fill(sb);
    if (fields.length > 0) {
      sb.append(" ORDER BY ");
      visit(fields, new Action1<String>() {
        @Override public void call(String s) {
          sb.append(s);
        }
      }, new Runnable() {
        @Override public void run() {
          sb.append(",");
        }
      });

      sb.append(desc ? " DESC" : " ASC");
    }
    return sb;
  }
}
