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

import com.ibm.csync.internals.SubStateManager;
import com.ibm.csync.internals.response.Happy;
import com.ibm.csync.internals.websocket.CSTransport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;

import static com.ibm.csync.internals.SubStateManager.SubStatus;
import static org.junit.Assert.assertEquals;

public class SubStateManagerTests {
  private SubStateManager stateManager;

  @Before public void before() {
    CSTransport transport = Mockito.mock(CSTransport.class);
    CSKey csKey = CSKey.root;

    Observable<Happy> observable = Observable.just(new Happy());
    Mockito.when(transport.listen(csKey)).thenReturn(observable);
    Mockito.when(transport.unlisten(csKey)).thenReturn(observable);

    stateManager = new SubStateManager(transport, csKey);
  }

  @Test public void defaultState() {
    assertEquals(0, stateManager.getDesired().intValue());
    assertEquals(SubStatus.NO, stateManager.getActual());
  }

  @Test public void sub() {
    stateManager.sub();
    assertEquals(1, stateManager.getDesired().intValue());
    assertEquals(SubStatus.YES, stateManager.getActual());
    stateManager.sub();
    assertEquals(2, stateManager.getDesired().intValue());
    assertEquals(SubStatus.YES, stateManager.getActual());
  }

  @Test public void unsub() {
    stateManager.unsub();
    assertEquals(0, stateManager.getDesired().intValue());
    assertEquals(SubStatus.NO, stateManager.getActual());
  }

  @Test public void combine() {
    stateManager.sub();
    stateManager.unsub();
    assertEquals(0, stateManager.getDesired().intValue());
    assertEquals(SubStatus.NO, stateManager.getActual());
    stateManager.sub();
    assertEquals(1, stateManager.getDesired().intValue());
    assertEquals(SubStatus.YES, stateManager.getActual());
  }
}
