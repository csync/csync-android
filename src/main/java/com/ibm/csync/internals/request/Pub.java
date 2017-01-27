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

package com.ibm.csync.internals.request;

public class Pub extends CSRequest {
  final long cts;
  final String[] path;
  final String data;
  final boolean deletePath;
  final String assumeACL;

  public Pub(
      long cts,
      String[] path,
      String data,
      boolean deletePath,
      String assumeACL) {
    this.cts = cts;
    this.path = path;
    this.data = data;
    this.deletePath = deletePath;
    this.assumeACL = assumeACL;
  }

  @Override
  public Kind getKind() {
    return Kind.pub;
  }
}
