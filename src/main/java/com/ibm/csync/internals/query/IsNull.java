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

public final class IsNull extends Predicate {
  public final String name;

  public IsNull(final String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean sameAs(final Query other) {
    if (!(other instanceof IsNull)) return false;
    final IsNull q = (IsNull) other;
    return name.equals(q.name);
  }

  @Override
  public StringBuffer fill(final StringBuffer sb) {
    sb.append(name);
    sb.append(" is null");
    return sb;
  }
}
