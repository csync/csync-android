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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ibm.csync.CSKey;
import com.ibm.csync.CSValue;
import com.ibm.csync.acls.CSAcl;
import java.lang.reflect.Type;

public class CSValueDeserializer implements JsonDeserializer<CSValue> {
  @Override
  public CSValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    if (json.isJsonObject()) {
      JsonObject jsonObject = (JsonObject) json;

      CSKey key = jsonArraytoCSKey(jsonObject.get("path").getAsJsonArray());
      String data = null;
      if (jsonObject.has("data")) {
        data = jsonObject.get("data").getAsString();
      }
      long cts = jsonObject.get("cts").getAsLong();
      long vts = jsonObject.get("vts").getAsLong();
      boolean keyDeleted = jsonObject.get("deletePath").getAsBoolean();
      CSAcl acl = CSAcl.customAcl(jsonObject.get("acl").getAsString());

      return new CSValue(key, data, cts, vts, acl, keyDeleted);
    }
    return null;
  }

  private CSKey jsonArraytoCSKey(JsonArray array) {
    CSKey key = CSKey.root;
    for (int i = 0; i < array.size(); i++) {
      key = key.child(array.get(i).getAsString());
    }
    return key;
  }
}
