/*
 * Copyright (C) 2010 Google Inc.
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
package com.google.gson.rest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.rest.definition.ID;
import com.google.gson.rest.definition.RestResource;
import com.google.gson.rest.definition.RestResponse;
import com.google.gson.rest.definition.RestResponseSpec;
import com.google.gson.webservice.definition.ContentBodySpec;
import com.google.gson.webservice.definition.HeaderMap;
import com.google.gson.webservice.definition.HeaderMapSpec;
import com.google.gson.webservice.definition.WebServiceSystemException;
import com.google.gson.wsclient.internal.utils.ConnectionPreconditions;

/**
 * Receives a response coming on an {@link HttpURLConnection}.
 * 
 * @author inder
 */
public final class RestResponseReceiver<I extends ID, R extends RestResource<I, R>> {
  private final Gson gson;
  private final RestResponseSpec spec;
  private final Logger logger;
  private final Level logLevel;

  public RestResponseReceiver(Gson gson, RestResponseSpec spec) {
    this(gson, spec, null);
  }
  public RestResponseReceiver(Gson gson, RestResponseSpec spec, Level logLevel) {
    this.gson = gson;
    this.spec = spec;
    this.logger = logLevel == null ? null : Logger.getLogger(RestResponseReceiver.class.getName());
    this.logLevel = logLevel;
  }
  
  public RestResponse<I, R> receive(HttpURLConnection conn) {
    try {
      HeaderMapSpec paramSpec = spec.getHeadersSpec();
      Type bodyType = spec.getResourceType();
      // read response
      HeaderMap responseParams = readResponseHeaders(conn, paramSpec);
      R responseBody = readResponseBody(conn, bodyType);
      return new RestResponse<I, R>(responseParams, responseBody, bodyType);
    } catch (IOException e) {
      throw new WebServiceSystemException(e);
    }
  }

  private HeaderMap readResponseHeaders(HttpURLConnection conn, HeaderMapSpec paramsSpec) {    
    HeaderMap.Builder paramsBuilder = new HeaderMap.Builder(paramsSpec);    
    for (Map.Entry<String, Type> entry : paramsSpec.entrySet()) {
      String paramName = entry.getKey();
      String json = conn.getHeaderField(paramName);
      if (json != null) {
        if (logger != null) {
          logger.log(logLevel, String.format("Response Header: %s:%s\n", paramName, json));
        }
        Type typeOfT = paramsSpec.getTypeFor(paramName);
        Object value = gson.fromJson(json, typeOfT);
        paramsBuilder.put(paramName, value, typeOfT);
      }
    }
    return paramsBuilder.build();
  }

  @SuppressWarnings("unchecked")
  private R readResponseBody(
      HttpURLConnection conn, Type resourceType) throws IOException {
    String connContentType = conn.getContentType();
    ConnectionPreconditions.checkArgument(
      connContentType.contains(ContentBodySpec.JSON_CONTENT_TYPE), conn);
    Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    R body = (R) gson.fromJson(reader, resourceType);
    return body;
  }
}
