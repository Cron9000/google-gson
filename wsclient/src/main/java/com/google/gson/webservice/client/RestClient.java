/*
 * Copyright (C) 2008 Google Inc.
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
package com.google.gson.webservice.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.webservice.definition.WebServiceSystemException;
import com.google.gson.webservice.definition.rest.RestCallSpec;
import com.google.gson.webservice.definition.rest.RestRequest;
import com.google.gson.webservice.definition.rest.RestResponse;

/**
 * Main class used by clients to access a Gson Web service.
 * 
 * @author inder
 */
public class RestClient {
  private final WebServiceConfig config;
  private final Logger logger;
  private final Level logLevel;

  public RestClient(WebServiceConfig serverConfig) {
    this(serverConfig, null);
  }

  public RestClient(WebServiceConfig serverConfig, Level logLevel) {
    this.config = serverConfig;
    this.logger = logLevel == null ? null : Logger.getLogger(RestClient.class.getName());
    this.logLevel = logLevel;
  }
  
  private URL getWebServiceUrl(RestCallSpec<?> callSpec) {
    String url = config.getServiceBaseUrl() + callSpec.getPath().get();
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
  
  public <R> RestResponse<R> getResponse(RestCallSpec<R> callSpec, RestRequest<R> request) {
    Gson gson = new GsonBuilder().create();
    return getResponse(callSpec, request, gson);
  }

  public <R> RestResponse<R> getResponse(
      RestCallSpec<R> callSpec, RestRequest<R> request, Gson gson) {
    try {
      URL webServiceUrl = getWebServiceUrl(callSpec);
      if (logger != null) {
        logger.log(logLevel, "Opening connection to " + webServiceUrl);
      }
      HttpURLConnection conn = (HttpURLConnection) webServiceUrl.openConnection();
      RestRequestSender requestSender = new RestRequestSender(gson, logLevel);
      requestSender.send(conn, request);
      RestResponseReceiver<R> responseReceiver =
        new RestResponseReceiver<R>(gson, callSpec.getResponseSpec(), logLevel);
      return responseReceiver.receive(conn);
    } catch (IOException e) {
      throw new WebServiceSystemException(e);
    } catch (IllegalArgumentException e) {
      throw new WebServiceSystemException(e);
    }
  }
  
  @Override
  public String toString() {
    return String.format("config:%s", config);
  }
}
