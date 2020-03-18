/*
 * Copyright (C) 2019 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.knotx.fragments.action.http.response;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;

import io.knotx.commons.json.MultiMapTransformer;
import io.knotx.fragments.action.http.HttpAction.HttpActionResult;
import io.knotx.fragments.action.http.log.HttpActionLogger;
import io.knotx.fragments.action.http.options.ResponseOptions;
import io.knotx.fragments.action.http.request.EndpointRequest;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.action.http.payload.ActionPayload;
import io.knotx.fragments.action.http.payload.ActionRequest;
import io.knotx.fragments.action.http.payload.ActionResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

public class EndpointResponseProcessor {

  public static final String TIMEOUT_TRANSITION = "_timeout";
  private static final String HTTP_ACTION_TYPE = "HTTP";
  private static final String METADATA_HEADERS_KEY = "headers";
  private static final String METADATA_STATUS_CODE_KEY = "statusCode";
  private static final String JSON = "JSON";
  private final boolean isJsonPredicate;
  private final boolean isForceJson;

  public EndpointResponseProcessor(ResponseOptions responseOptions) {
    this.isJsonPredicate = responseOptions.getPredicates().contains(JSON);
    this.isForceJson = responseOptions.isForceJson();
  }

  public HttpActionResult handleResponse(EndpointRequest endpointRequest,
      EndpointResponse endpointResponse, HttpActionLogger actionLogger) {
    if (isConsideredSuccess(endpointResponse)) {
      return handleWithSuccessResponseCode(endpointRequest, endpointResponse, actionLogger);
    } else {
      return handleWithErrorResponseCode(endpointRequest, endpointResponse, actionLogger);
    }
  }

  private HttpActionResult handleWithSuccessResponseCode(EndpointRequest endpointRequest,
      EndpointResponse endpointResponse, HttpActionLogger actionLogger) {
    actionLogger.onResponseCodeSuccessful();
    ActionPayload payload = successResponsePayload(endpointRequest, endpointResponse, actionLogger);
    return new HttpActionResult(payload, FragmentResult.SUCCESS_TRANSITION);
  }

  private HttpActionResult handleWithErrorResponseCode(EndpointRequest endpointRequest,
      EndpointResponse endpointResponse, HttpActionLogger actionLogger) {
    actionLogger.onResponseCodeUnsuccessful(new IOException(
        "The service responded with unsuccessful status code: " + endpointResponse.getStatusCode()
            .code()));
    ActionPayload payload = errorResponsePayload(endpointRequest, endpointResponse);
    return new HttpActionResult(payload, getErrorTransition(endpointResponse));
  }

  private ActionPayload successResponsePayload(EndpointRequest endpointRequest,
      EndpointResponse endpointResponse, HttpActionLogger httpActionLogger) {
    ActionRequest actionRequest = createActionRequest(endpointRequest);
    ActionResponse actionResponse = createSuccessActionResponse(endpointResponse);
    Object result = tryToRetrieveResultFrom(endpointResponse, httpActionLogger);
    return ActionPayload.create(actionRequest, actionResponse, result);
  }

  private ActionPayload errorResponsePayload(EndpointRequest endpointRequest,
      EndpointResponse endpointResponse) {
    ActionRequest actionRequest = createActionRequest(endpointRequest);
    ActionResponse actionResponse = createErrorActionResponse(endpointResponse);
    return ActionPayload.noResult(actionRequest, actionResponse);
  }

  private ActionRequest createActionRequest(EndpointRequest endpointRequest) {
    ActionRequest request = new ActionRequest(HTTP_ACTION_TYPE, endpointRequest.getPath());
    request.appendMetadata(METADATA_HEADERS_KEY,
        MultiMapTransformer.toJson(endpointRequest.getHeaders()));
    return request;
  }

  private ActionResponse createSuccessActionResponse(EndpointResponse endpointResponse) {
    return ActionResponse.success()
        .appendMetadata(METADATA_STATUS_CODE_KEY,
            String.valueOf(endpointResponse.getStatusCode().code()))
        .appendMetadata(METADATA_HEADERS_KEY,
            MultiMapTransformer.toJson(endpointResponse.getHeaders()));
  }

  private ActionResponse createErrorActionResponse(EndpointResponse endpointResponse) {
    return ActionResponse
        .error(endpointResponse.getStatusCode().toString(), endpointResponse.getStatusMessage())
        .appendMetadata(METADATA_STATUS_CODE_KEY,
            String.valueOf(endpointResponse.getStatusCode().code()))
        .appendMetadata(METADATA_HEADERS_KEY,
            MultiMapTransformer.toJson(endpointResponse.getHeaders()));
  }

  private Object tryToRetrieveResultFrom(EndpointResponse endpointResponse,
      HttpActionLogger httpActionLogger) {
    try {
      return retrieveResultFrom(endpointResponse);
    } catch (Exception exception) {
      httpActionLogger.onResponseProcessingFailed(exception);
      throw exception;
    }
  }

  private Object retrieveResultFrom(EndpointResponse response) {
    if (isForceJson || isJsonPredicate || isContentTypeHeaderJson(response)) {
      return bodyToJson(response.getBody().toString());
    } else {
      return response.getBody().toString();
    }
  }

  private boolean isContentTypeHeaderJson(EndpointResponse endpointResponse) {
    String contentType = endpointResponse.getHeaders().get(HttpHeaderNames.CONTENT_TYPE);
    return contentType != null && contentType.contains(HttpHeaderValues.APPLICATION_JSON);
  }

  private Object bodyToJson(String responseBody) {
    if (StringUtils.isBlank(responseBody)) {
      return new JsonObject();
    } else if (responseBody.startsWith("[")) {
      return new JsonArray(responseBody);
    } else {
      return new JsonObject(responseBody);
    }
  }

  private String getErrorTransition(EndpointResponse endpointResponse) {
    if (isTimeout(endpointResponse)) {
      return TIMEOUT_TRANSITION;
    } else {
      return ERROR_TRANSITION;
    }
  }

  private boolean isConsideredSuccess(EndpointResponse response) {
    return !HttpStatusClass.CLIENT_ERROR.contains(response.getStatusCode().code())
        && !HttpStatusClass.SERVER_ERROR.contains(response.getStatusCode().code());
  }

  private boolean isTimeout(EndpointResponse response) {
    return HttpResponseStatus.REQUEST_TIMEOUT == response.getStatusCode();
  }

}
