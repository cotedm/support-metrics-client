/**
 * Copyright 2015 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.confluent.support.metrics.utils;


import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebClient {
  private static final Logger log = LoggerFactory.getLogger(WebClient.class);
  private static final int requestTimeoutMs = 2000;
  public static final int DEFAULT_STATUS_CODE = HttpStatus.SC_BAD_GATEWAY;

  /**
   * Sends a POST request to a web server
   * @param customerId: customer Id on behalf of which the request is sent
   * @param bytes: request payload
   * @param httpPost: A POST request structure
   * @return an HTTP Status code
   */
  public static int send(String customerId, byte[] bytes, HttpPost httpPost) {
    int statusCode = DEFAULT_STATUS_CODE;
    if (bytes != null && bytes.length > 0 && httpPost != null && customerId != null) {

      // add the body to the request
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      builder.addTextBody("cid", customerId);
      builder.addBinaryBody("file", bytes, ContentType.DEFAULT_BINARY, "filename");
      httpPost.setEntity(builder.build());

      // set the HTTP config
      final RequestConfig config = RequestConfig.custom().
          setConnectTimeout(requestTimeoutMs).
          setConnectionRequestTimeout(requestTimeoutMs).
          setSocketTimeout(requestTimeoutMs).
          build();

      // send request
      try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
           CloseableHttpResponse response = httpclient.execute(httpPost)) {
        log.debug("POST request returned {}", response.getStatusLine().toString());
        statusCode = response.getStatusLine().getStatusCode();
      } catch (IOException e) {
        log.debug("Could not submit metrics to Confluent: {}", e.getMessage());
      }
    } else {
      statusCode = HttpStatus.SC_BAD_REQUEST;
    }
    return statusCode;
  }
}
