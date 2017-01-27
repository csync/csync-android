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

public final class Select extends Query {
  final String[] projection;

  public Select(final String... projection) {
    this.projection = projection;
  }

  public From from(final String... tables) {
    return new From(this, tables);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(projection);
  }

  @Override
  public boolean sameAs(final Query other) {
    if (!(other instanceof Select)) return false;
    final Select q = (Select) other;
    return Arrays.equals(projection, q.projection);
  }

  @Override
  public StringBuffer fill(final StringBuffer sb) {
    sb.append("SELECT ");
    if (projection.length == 0) {
      sb.append("*");
    } else {
      visit(projection, new Action1<String>() {
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
