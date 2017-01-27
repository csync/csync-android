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

package com.ibm.csync.acls;

public final class CSAcl {
  /** A static ACL that permits only the creator read, write and create access. */
  public static final CSAcl PRIVATE = CSAcl.customAcl("$private");
  /** A static ACL that permits all users read access, but only the creator has write and create access. */
  public static final CSAcl PUBLIC_READ = CSAcl.customAcl("$publicRead");
  /** A static ACL that permits all users write access, but only the creator has read and create access. */
  public static final CSAcl PUBLIC_WRITE = CSAcl.customAcl("$publicWrite");
  /** A static ACL that permits all users read and write access, but only the creator has create access. */
  public static final CSAcl PUBLIC_READ_WRITE = CSAcl.customAcl("$publicReadWrite");
  /** A static ACL that permits all users create access, but only the creator has read and write access. */
  public static final CSAcl PUBLIC_CREATE = CSAcl.customAcl("$publicCreate");
  /** A static ACL that permits all users read and create access, but only the creator has write access. */
  public static final CSAcl PUBLIC_READ_CREATE = CSAcl.customAcl("$publicReadCreate");
  /** A static ACL that permits all users write and create access, but only the creator has read access. */
  public static final CSAcl PUBLIC_WRITE_CREATE = CSAcl.customAcl("$publicWriteCreate");
  /** A static ACL that permits all users read, write and create access. */
  public static final CSAcl PUBLIC_READ_WRITE_CREATE = CSAcl.customAcl("$publicReadWriteCreate");

  public enum AccessType {
    READ, WRITE, CREATE
  }

  private String rawAcl;

  private CSAcl(String rawAclName) {
    this.rawAcl = rawAclName;
  }

  public static CSAcl customAcl(String rawAclName) {
    return new CSAcl(rawAclName);
  }

  //Accessor Methods
  public String rawAcl() {
    return this.rawAcl;
  }
}
