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

import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Efficiently and safely manages the state for an arbitrary process (e.g. connection)
 *
 * @param <A> actual state
 * @param <D> desired state
 */
public abstract class StateManager<A, D> {
  //private static final int THREAD_POOL_SIZE = 4;

  //protected boolean busy;
  // protected ScheduledExecutorService transactionQueue;
  private A actual;
  private D desired;
  private boolean busy = false;
  private long cookie = -1;

  private BehaviorSubject<A> stateSubject = BehaviorSubject.create();
  public Observable<A> state = stateSubject.distinctUntilChanged();

  /** Instantiate a StateManager with default actual and desired states */
  public StateManager(A defaultActualState, D defaultDesiredState) {
    actual = defaultActualState;
    desired = defaultDesiredState;
    stateSubject.onNext(defaultActualState);
    //transactionQueue = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
  }

  /** Monitors and corrects state consistency in an efficient, safe manner */
  private synchronized void nudge() {
    // state change currently being processed
    if (busy) return;
    // state is already consistent
    if (isConsistentState(actual, desired)) return;

    busy = true;
    cookie++;

    makeConsistent(cookie, actual, desired);
  }

  /** force an actual value */
  public synchronized void forceActual(final A a) {
    actual = a;
    busy = false;
    cookie++;
    nudge();
  }

  public synchronized A getActual() {
    return actual;
  }

  public synchronized void setActual(final long cookie, final A a) throws TooLate {
    if (cookie != this.cookie) throw new TooLate();
    //TODO: dont always have to throw TooLate.
    actual = a;
    busy = false;
    stateSubject.onNext(a);
    nudge();
  }

  public synchronized D getDesired() {
    return desired;
  }

  public synchronized void setDesired(final D d) {
    desired = d;
    nudge();
  }

  /** Determines that a given actual and desired state is consistent */
  abstract protected boolean isConsistentState(final A a, final D d);

  /** Behavior for making the actual and desired states consistent */
  abstract protected void makeConsistent(final long cookie, final A a, final D d);

  public static class TooLate extends Exception {

    private static final long serialVersionUID = 1L;
  }
}
