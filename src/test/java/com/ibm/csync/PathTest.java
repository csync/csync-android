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
import org.junit.Test;

import static com.ibm.csync.internals.query.Predicate.empty;
import static com.ibm.csync.internals.query.Predicate.eq;
import static com.ibm.csync.internals.query.Predicate.isNotNull;
import static com.ibm.csync.internals.query.Predicate.isNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PathTest {
  @Test public void make() {
    assertEquals("hello.world", CSKey.make("hello", "world").toString());
  }

  @Test public void root() {
    assertEquals(CSKey.make(), CSKey.root);
  }

  @Test public void parent() {
    final CSKey csKey = CSKey.make("hello", "world");
    final CSKey p = csKey.parent();
    final CSKey pp = p.parent();
    final CSKey ppp = pp.parent();

    assertEquals(CSKey.make("hello"), p);
    assertEquals(CSKey.root, pp);
    assertNull(ppp);
  }

  @Test public void child() {
    assertEquals(CSKey.make("hello", "world"), CSKey.make("hello").child("world"));
  }

  @Test public void toArray() {
    assertArrayEquals(new String[] {"hello", "world"}, CSKey.make("hello", "world").toArray());
  }

  @Test public void startsWith() {
    assertTrue(CSKey.make("hello", "world").matches(CSKey.make("hello", "#")));
    assertFalse(CSKey.make("hello", "world").matches(CSKey.make("world", "#")));
  }

  @Test public void fromString() {
    final CSKey csKey = CSKey.fromString("a.b.c");
    final String[] arr = csKey.toArray();

    assertEquals(3, arr.length);
    assertEquals(arr[0], "a");
    assertEquals(arr[1], "b");
    assertEquals(arr[2], "c");
  }

  private void check(final boolean b, final String path, String pattern) {
    assertEquals(b, CSKey.fromString(path).matches(CSKey.fromString(pattern)));
    assertEquals(b, CSKey.fromString(pattern).matches(CSKey.fromString(path)));
  }

  @Test public void matching() {
    check(true, "a.b.c.d", "a.b.c.d");
    check(true, "a.b.c.d", "#");
    check(true, "a.b.c.d", "a.#");
    check(true, "a.b.c.d", "a.b.#");
    check(true, "a.b.c.d", "a.b.c.#");
    check(true, "a.b.c.d", "a.b.c.d.#");
    check(false, "a.b.c.d", "a.b.c.d.e.#");
    check(true, "a", "a.#");

    check(false, "a.b.c.d", "b.#");
    check(true, "a.b.c.d", "*.*.*.*");
    check(false, "a.b.c.d", "*.*.*");
    check(true, "a.b.c.d", "a.*.*.*");
    check(true, "a.b.c.d", "a.*.c.d");
    check(false, "a.b.c.d", "a.*.c.d.e");
    check(true, "a.b.c.d", "*.#");
    check(true, "a", "*.#");

    check(true, "*", "*");
    check(false, "*.*", "*");
    check(true, "*.x", "x.*");
  }

  private String[] paths(int n) {
    final String[] ps = new String[n];
    for (int i = 0; i < n; i++) {
      ps[i] = String.format("p%d", i);
    }
    return ps;
  }

  private Predicate ts(final String[] ps, final String path) {
    return CSKey.fromString(path).toQuery(ps);
  }

  @Test public void query() {

    final String[] ps = paths(4);

    assertEquals(
        empty(),
        ts(ps, "#"));

    assertEquals(
        eq(ps[0], "a"),
        ts(ps, "a.#"));

    assertEquals(
        eq(ps[0], "a").and(isNull(ps[1])),
        ts(ps, "a"));

    assertEquals(
        isNotNull(ps[0]).and(isNull(ps[1])),
        ts(ps, "*"));

    assertEquals(
        isNotNull(ps[1]).and(isNull(ps[2])),
        ts(ps, "*.*"));

    assertEquals(
        isNotNull(ps[2]).and(isNull(ps[3])),
        ts(ps, "*.*.*"));

    assertEquals(
        isNotNull(ps[3]),
        ts(ps, "*.*.*.*"));

    assertEquals(
        isNotNull(ps[2]),
        ts(ps, "*.*.*.#"));

    assertEquals(
        eq(ps[2], "xyz").and(eq(ps[0], "abc")).and(isNull(ps[3])),
        ts(ps, "abc.*.xyz"));

    assertEquals(
        eq(ps[1], "xyz").and(isNull(ps[2])),
        ts(ps, "*.xyz"));

    assertEquals(
        isNotNull(ps[0]),
        ts(ps, "*.#"));

    //assertEquals("p1=bandp0=aandp2isnull",ts(4, "a.b"));
    //assertEquals("p2=bandp0=aandp1isnotnullandp3isnull",ts(4,"a.*.b"));

  }
}
