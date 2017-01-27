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

public final class From extends Query {

  final Query query;
  final String[] list;

  public From(final Query query, final String... list) {
    this.query = query;
    this.list = list;
  }

  public Where where(final Predicate... preds) {
    return new Where(this, Predicate.AND(preds));
  }

  @Override
  public int hashCode() {
    return query.hashCode() ^ Arrays.hashCode(list);
  }

  @Override
  public boolean sameAs(final Query other) {
    if (!(other instanceof From)) return false;
    final From q = (From) other;
    return query.sameAs(q.query) && Arrays.deepEquals(list, q.list);
  }

  @Override
  public StringBuffer fill(final StringBuffer sb) {
    query.fill(sb);

    if (list.length > 0) {
      sb.append(" FROM ");
      visit(list, new Action1<String>() {
        @Override public void call(String s) {
          sb.append(s);
        }
      }, new Runnable() {
        @Override public void run() {
          sb.append(",");
        }
      });
    }

    return sb;
  }
}
