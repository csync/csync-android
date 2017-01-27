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

public class Binary extends Predicate {
  public final String op;
  public final Predicate left;
  public final Predicate right;

  public Binary(final String op, final Predicate left, final Predicate right) {
    this.op = op;
    this.left = left;
    this.right = right;
  }

  @Override
  public int hashCode() {
    return op.hashCode() ^ left.hashCode() ^ right.hashCode();
  }

  @Override
  public boolean sameAs(final Query other) {
    if (!(other instanceof Binary)) return false;
    final Binary q = (Binary) other;
    return q.op.equals(op) && q.left.sameAs(left) && q.right.sameAs(right);
  }

  @Override
  public StringBuffer fill(final StringBuffer sb) {
    if (left.isEmpty()) {
      return right.fill(sb);
    } else if (right.isEmpty()) {
      return left.fill(sb);
    } else {
      left.paran(sb);
      sb.append(" ");
      sb.append(op);
      sb.append(" ");
      return right.paran(sb);
    }
  }
}
