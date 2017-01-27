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

public class CSAuthData {
  /** The uid for this user. It is unique across all auth providers. */
  public final String uid;
  /** The OAuth indentity provider that provided the token that identifies the user */
  public final String provider;
  /** The token used to authenticate the user with the CSync Service */
  public final String token;
  /** The expiration timestamp (seconds since epoch) for the OAuth token */
  public final long expires;

  public CSAuthData(String uid, String provider, String token, long expires) {
    this.uid = uid;
    this.provider = provider;
    this.token = token;
    this.expires = expires;
  }
}
