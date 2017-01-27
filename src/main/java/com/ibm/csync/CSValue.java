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

import com.ibm.csync.acls.CSAcl;
import com.ibm.csync.internals.response.Kind;
import com.ibm.csync.internals.response.CSResponse;

public final class CSValue extends CSResponse {
  private final CSKey key;
  private final String data;
  private final long cts;
  private final long vts;
  private final CSAcl acl;
  private final boolean keyDeleted;

  public CSValue(CSKey key, String data, long cts, long vts, CSAcl acl, boolean keyDeleted) {
    this.key = key;
    this.data = data;
    this.cts = cts;
    this.vts = vts;
    this.acl = acl;
    this.keyDeleted = keyDeleted;
  }

  public CSKey key() {
    return key;
  }

  public String data() {
    return data;
  }

  public long cts() {
    return cts;
  }

  public long vts() {
    return vts;
  }

  public CSAcl acl() {
    return acl;
  }

  public boolean isKeyDeleted() {
    return keyDeleted;
  }

  @Override
  public Kind getKind() {
    return Kind.data;
  }
}
