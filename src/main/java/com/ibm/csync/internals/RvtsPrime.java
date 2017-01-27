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

public class RvtsPrime {

  private long rvtsPrime;
  private boolean initialRvtsPrime;

  public RvtsPrime() {
    this.rvtsPrime = 0L;
  }

  public long getRvtsPrime() {
    if (rvtsPrime <= 0L) {
      return Long.MAX_VALUE;
    }
    return rvtsPrime;
  }

  public void setRvtsPrime(long rvtsPrime) {
    this.rvtsPrime = rvtsPrime;
  }
}
