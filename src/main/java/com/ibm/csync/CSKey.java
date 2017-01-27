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

package com.ibm.csync;

import com.ibm.csync.internals.query.Predicate;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * The CSKey class creates a friendly API for creating the paths needed throughout CSApp.
 *
 * In any path there are two wildcard operators in the path: <ul> <li> '#' - if the pound character
 * is used as the last part of the path, then the user will listen to all changes at and below the
 * given path in the tree.</li> <li> '*' - if the asterisk is used in any part of the path, then the
 * user will listen to all parts of the tree at that specified level in the tree.</li> </ul>
 */
public final class CSKey {
  public static final CSKey root = new CSKey(null, null, null);
  private static final String TAG = CSKey.class.getName();
  public final int length;
  final CSKey parent;
  final String me;
  private WeakReference<String[]> asArray;
  private WeakReference<String> asString = new WeakReference<>(null);

  private CSKey(final CSKey parent, final String me, final String[] asArray) {
    this.parent = parent;
    this.me = me;
    this.asArray = new WeakReference<>(asArray);
    this.length = (parent == null) ? 0 : parent.length + 1;
  }

  public static CSKey make(String... parts) {
    CSKey p = root;

    for (final String s : parts) {
      p = p.child(s);
    }

    return p;
  }

  public static CSKey fromString(final String x) {
    return make(x.split("\\."));
  }

  public int length() {
    return length;
  }

  public CSKey parent() {
    return parent;
  }

  public void fill(final String[] arr) {
    if (parent != null) {
      parent.fill(arr);
    }
    if (length > 0) {
      arr[length - 1] = me;
    }
  }

  public String[] toArray() {
    final String[] it = asArray.get();
    final String[] ret;
    if (it == null) {
      if (parent == null) {
        ret = new String[] {me};
      } else {
        ret = new String[length];
        fill(ret);
      }
      asArray = new WeakReference<>(ret);
    } else {
      ret = it;
    }
    return ret;
  }

  public CSKey child(String x) {
    return new CSKey(this, x, null);
  }

  public CSKey uuidChild() {
    return child(UUID.randomUUID().toString());
  }

  public final boolean matches(final CSKey other) {
    if (other == null) return false;

    final String[] a0 = toArray();
    final String[] a1 = other.toArray();

    for (int i = 0; i < Math.min(a0.length, a1.length); i++) {
      final String c0 = a0[i];
      final String c1 = a1[i];
      if ("*".equals(c0)) continue;
      if ("*".equals(c1)) continue;
      if ("#".equals(c0)) return true;
      if ("#".equals(c1)) return true;
      if (!c0.equals(c1)) return false;
    }

    if (a0.length > a1.length) {
      return "#".equals(a0[a1.length]);
    } else {
      return a1.length <= a0.length || "#".equals(a1[a0.length]);
    }
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof CSKey)) return false;

    final CSKey other = (CSKey) obj;
    return toString().equals(other.toString());
  }

  @Override
  public String toString() {
    final String it = asString.get();
    if (it == null) {
      final String[] arr = toArray();
      final StringBuilder sb = new StringBuilder();
      String dot = "";
      for (final String s : arr) {
        sb.append(dot);
        sb.append(s);
        dot = ".";
      }
      final String res = sb.toString();
      asString = new WeakReference<>(res);
      return res;
    } else {
      return it;
    }
  }

  public Predicate toQuery(final String fieldName[]) {
    final String[] arr = toArray();
    boolean checkNull = true;
    int checkNotNull = -1;

    Predicate p = Predicate.empty();

    for (int i = 0; i < arr.length; i++) {
      final String comp = arr[i];
      if ("*".equals(comp)) {
        checkNotNull = i;
      } else if ("#".equals(comp)) {
        checkNull = false;
        break;
      } else {
        p = Predicate.eq(fieldName[i], comp).and(p);
        checkNotNull = -1;
      }
    }

    if (checkNotNull >= fieldName.length) {
      checkNotNull = -1;
    }

    if (checkNotNull != -1) {
      p = p.and(Predicate.isNotNull(fieldName[checkNotNull]));
    }

    if (arr.length >= fieldName.length) {
      checkNull = false;
    }

    if (checkNull) {
      p = p.and(Predicate.isNull(fieldName[arr.length]));
    }

    return p;
  }
}
