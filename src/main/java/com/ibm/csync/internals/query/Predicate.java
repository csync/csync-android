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

public abstract class Predicate extends Query {

  public static boolean isEmpty(final Predicate p) {
    return (p == null) || (p.isEmpty());
  }

  public static Predicate AND(final Predicate... preds) {
    if (preds.length == 0) {
      return null;
    } else {
      Predicate p = preds[0];
      for (int i = 1; i < preds.length; i++) {
        p = p.and(preds[i]);
      }
      return p;
    }
  }

  public static Predicate eq(final String name, final String value) {
    return new Eq(name, value);
  }

  public static Predicate le(final String name, final Object value) {
    return new BinaryTerm("<=", name, value);
  }

  public static Predicate gt(final String name, final Object value) {
    return new BinaryTerm(">", name, value);
  }

  public static Predicate empty() {
    return new Empty();
  }

  public static Predicate isNotNull(final String name) {
    return new IsNotNull(name);
  }

  public static Predicate isNull(final String name) {
    return new IsNull(name);
  }

  public boolean isEmpty() {
    return false;
  }

  public Predicate and(final Predicate p) {
    if (isEmpty()) {
      return p;
    } else if (p.isEmpty()) {
      return this;
    } else {
      return new And(this, p);
    }
  }

  public StringBuffer paran(final StringBuffer sb) {
    sb.append("(");
    fill(sb);
    sb.append(")");
    return sb;
  }
}


