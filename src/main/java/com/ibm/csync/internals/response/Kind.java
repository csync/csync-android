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

package com.ibm.csync.internals.response;

import com.ibm.csync.CSValue;

public enum Kind {
  advanceResponse("advanceResponse", AdvanceResponse.class),
  fetchResponse("fetchResponse", FetchResponse.class),
  getAclsResponse("getAcls", GetAclsResponse.class),
  happy("happy", Happy.class),
  data("data", CSValue.class),
  error("error", Error.class),
  queryResponse("query", QueryResponse.class);

  final String rawKind;
  final Class<? extends CSResponse> cls;

  Kind(String rawKind, final Class<? extends CSResponse> cls) {
    this.rawKind = rawKind;
    this.cls = cls;
  }

  public String rawKind() {
    return rawKind;
  }
}
