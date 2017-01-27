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

public final class Where extends Query {
  public final From from;
  public final Predicate pred;

  public Where(final From from, final Predicate pred) {
    this.from = from;
    this.pred = pred;
  }

  @Override
  public int hashCode() {
    return from.hashCode() ^ pred.hashCode();
  }

  @Override
  public boolean sameAs(final Query other) {
    if (!(other instanceof Where)) return false;
    final Where q = (Where) other;
    return from.sameAs(q.from) && pred.sameAs(q.pred);
  }

  @Override
  public StringBuffer fill(final StringBuffer sb) {
    from.fill(sb);
    sb.append(" where ");
    return pred.fill(sb);
  }
}
