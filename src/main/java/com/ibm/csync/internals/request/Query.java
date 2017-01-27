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

import java.util.List;

public class Query extends CSRequest {

  String query;
  String schema;
  String predicate;
  List<String> projection;

  public Query(final String query, final String schema, final String predicate,
      final List<String> projection) {
    this.query = query;
    this.schema = schema;
    this.predicate = predicate;
    this.projection = projection;
  }

  @Override
  public Kind getKind() {
    return Kind.query;
  }
}
