/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.console.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.ignite.console.json.RawContentDeserializer;

/**
 * Request result.
 */
public class RestResult {
    /** REST http code. */
    private int successStatus;

    /** The field contains description of error if server could not handle the request. */
    private String error;

    /** The field contains result of command. */
    private String res;

    /** Session token string representation. */
    private String sesTok;

    /**
     * @param successStatus REST http code.
     * @param error The field contains description of error if server could not handle the request.
     * @param sesTok The field contains session token.
     * @param res The field contains result of command.
     */
    @JsonCreator
    private RestResult(
        @JsonProperty("successStatus") int successStatus,
        @JsonProperty("error") String error,
        @JsonProperty("sessionToken") String sesTok,
        @JsonProperty("response") @JsonDeserialize(using = RawContentDeserializer.class) String res
    ) {
        this.successStatus = successStatus;
        this.error = error;
        this.res = res;
        this.sesTok = sesTok;
    }

    /**
     * @param status REST http code.
     * @param error The field contains description of error if server could not handle the request.
     * @return Request result.
     */
    public static RestResult fail(int status, String error) {
        return new RestResult(status, error, null, null);
    }

    /**
     * @return REST http code.
     */
    public int getSuccessStatus() {
        return successStatus;
    }

    /**
     * @return The field contains description of error if server could not handle the request.
     */
    public String getError() {
        return error;
    }

    /**
     * @return The field contains result of command.
     */
    @JsonRawValue
    public String getResponse() {
        return res;
    }

    /**
     * @return String representation of session token.
     */
    public String getSessionToken() {
        return sesTok;
    }

    /**
     * Clear session token.
     */
    public void clearSessionToken() {
        this.sesTok = null;
    }
}