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

package com.ibm.csync.internals;

import com.ibm.csync.CSKey;
import com.ibm.csync.internals.response.Happy;
import com.ibm.csync.internals.websocket.CSTransport;
import rx.functions.Action1;

/** A state manager for a Pickles listen that will listen/unsub based on the number of subs */
public class SubStateManager extends StateManager<SubStateManager.SubStatus, Integer> {
  final private CSTransport transport;
  final private CSKey csKey;

  /** The CSKey being subbed to */
  public SubStateManager(final CSTransport transport, final CSKey csKey) {
    super(SubStatus.NO, 0);
    this.transport = transport;
    this.csKey = csKey;
  }

  @Override protected boolean isConsistentState(final SubStatus actual, final Integer desired) {
    return actual.compatibleWith(desired);
  }

  public synchronized void sub() {
    setDesired(getDesired() + 1);
  }

  public synchronized void unsub() {
    final int old = getDesired();
    if (old > 0) {
      setDesired(old - 1);
    }
  }

  @Override public void makeConsistent(final long cookie, SubStatus old, Integer desired) {

    if (desired <= 0) {
      //advanceManager.stopSync(csKey);
      transport.unlisten(csKey).subscribe(
          new NextAction(cookie, SubStatus.NO),
          new ErrorAction(cookie, SubStatus.UNKNOWN));
    } else {
      //advanceManager.startSync(csKey);
      transport.listen(csKey).subscribe(
          new NextAction(cookie, SubStatus.YES),
          new ErrorAction(cookie, SubStatus.UNKNOWN));
    }
  }

  public enum SubStatus {
    YES {
      public boolean compatibleWith(int desired) {
        return desired > 0;
      }
    },
    NO {
      public boolean compatibleWith(int desired) {
        return desired <= 0;
      }
    },
    UNKNOWN {
      public boolean compatibleWith(int desired) {
        return false;
      }
    };

    public abstract boolean compatibleWith(int desired);
  }

  private class NextAction implements Action1<Happy> {
    private final SubStatus actual;
    private final long cookie;

    public NextAction(final long cookie, final SubStatus actual) {
      this.actual = actual;
      this.cookie = cookie;
    }

    @Override public void call(Happy happy) {
      try {
        setActual(cookie, actual);
      } catch (TooLate ex) {
        ex.printStackTrace();
      }
    }
  }

  private class ErrorAction implements Action1<Throwable> {
    private final SubStatus actual;
    private final long cookie;

    public ErrorAction(final long cookie, final SubStatus actual) {
      this.actual = actual;
      this.cookie = cookie;
    }

    @Override public void call(Throwable t) {
      try {
        setActual(cookie, actual);
      } catch (TooLate ex) {
        ex.printStackTrace();
      }
    }
  }
}
