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

import rx.functions.Action1;

public abstract class Query {

  public static Select select(final String... args) {
    return new Select(args);
  }

  public static Delete delete() {
    return new Delete();
  }

  public abstract StringBuffer fill(StringBuffer sb);

  public abstract boolean sameAs(final Query other);

  public OrderBy orderByDesc(final String... fields) {
    return new OrderBy(this, true, fields);
  }

  public OrderBy orderByAsc(final String... fields) {
    return new OrderBy(this, false, fields);
  }

  public Limit limit(final int limit) {
    return new Limit(this, limit);
  }

  public <T> void visit(final T[] parts, final Action1<T> every, final Runnable middle) {
    boolean first = true;

    for (final T it : parts) {
      if (first) {
        first = false;
      } else {
        middle.run();
      }

      every.call(it);
    }
  }

  public StringBuffer toSB() {
    final StringBuffer sb = new StringBuffer();
    return fill(sb);
  }

  @Override
  public String toString() {
    return toSB().toString();
  }

  @Override
  public boolean equals(final Object obj) {
    return obj != null && (obj == this || obj instanceof Query && sameAs((Query) obj));
  }

  abstract public int hashCode();
}
