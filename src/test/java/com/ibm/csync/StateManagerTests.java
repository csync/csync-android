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

import com.ibm.csync.internals.StateManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StateManagerTests {
  private SimpleStateManager stateManager;

  @Before public void before() {
    stateManager = new SimpleStateManager();
  }

  @Test public void defaultState() {
    assertFalse(stateManager.getActual());
    assertEquals(1, stateManager.getDesired().intValue());
  }

  @Test public void consistency() {
    stateManager.increment();
    assertTrue(stateManager.getActual());
    assertEquals(2, stateManager.getDesired().intValue());

    stateManager.decrement();
    assertFalse(stateManager.getActual());
    assertEquals(1, stateManager.getDesired().intValue());
  }

  // for testing purposes only; tracks if int is in even or odd state
  private static final class SimpleStateManager extends StateManager<Boolean, Integer> {
    public SimpleStateManager() {
      super(false, 1);
    }

    public void increment() {
      setDesired(getDesired() + 1);
    }

    public void decrement() {
      setDesired(getDesired() - 1);
    }

    @Override protected boolean isConsistentState(Boolean b, Integer i) {
      return b == (i % 2 == 0);
    }

    @Override
    protected void makeConsistent(long cookie, Boolean b, Integer i) {
      boolean newActual = false;
      if (i % 2 == 0) {
        newActual = true;
      }

      try {
        setActual(cookie, newActual);
      } catch (TooLate ex) {
        ex.printStackTrace();
      }
    }
  }
}
