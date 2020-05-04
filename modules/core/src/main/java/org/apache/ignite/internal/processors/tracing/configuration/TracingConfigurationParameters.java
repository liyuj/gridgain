/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.tracing.configuration;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import org.apache.ignite.internal.processors.tracing.Scope;
import org.apache.ignite.internal.processors.tracing.Span;
import org.jetbrains.annotations.NotNull;

/**
 * Set of tracing configuration parameters like sampling rate or supported scopes.
 */
public class TracingConfigurationParameters implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private static final double MIN_SAMPLING_RATE = 0d;

    /** */
    private static final double MAX_SAMPLING_RATE = 1d;

    /**
     * Number between 0 and 1 that more or less reflects the probability of sampling specific trace.
     * 0 and 1 have special meaning here, 0 means never 1 means always. Default value is 0 (never).
     */
    private final double samplingRate;

    /**
     * Set of {@link Scope} that defines which sub-traces will be included in given trace.
     * In other words, if child's span scope is equals to parent's scope
     * or it belongs to the parent's span supported scopes, then given child span will be attached to the current trace,
     * otherwise it'll be skipped.
     * See {@link Span#isChainable(org.apache.ignite.internal.processors.tracing.Scope)} for more details.
     */
    private final Set<Scope> supportedScopes;

    /**
     * Constructor.
     *
     * @param samplingRate Number between 0 and 1 that more or less reflects the probability of sampling specific trace.
     *  0 and 1 have special meaning here, 0 means never 1 means always. Default value is 0 (never).
     * @param supportedScopes Set of {@link Scope} that defines which sub-traces will be included in given trace.
     *  In other words, if child's span scope is equals to parent's scope
     *  or it belongs to the parent's span supported scopes, then given child span will be attached to the current trace,
     *  otherwise it'll be skipped.
     *  See {@link Span#isChainable(org.apache.ignite.internal.processors.tracing.Scope)} for more details.
     */
    private TracingConfigurationParameters(double samplingRate,
        Set<Scope> supportedScopes) {
        this.samplingRate = samplingRate;
        this.supportedScopes = Collections.unmodifiableSet(supportedScopes);
    }

    /**
     * @return Number between 0 and 1 that more or less reflects the probability of sampling specific trace.
     * 0 and 1 have special meaning here, 0 means never 1 means always. Default value is 0 (never).
     */
    public double samplingRate() {
        return samplingRate;
    }

    /**
     * @return Set of {@link Scope} that defines which sub-traces will be included in given trace.
     * In other words, if child's span scope is equals to parent's scope
     * or it belongs to the parent's span supported scopes, then given child span will be attached to the current trace,
     * otherwise it'll be skipped.
     * See {@link Span#isChainable(org.apache.ignite.internal.processors.tracing.Scope)} for more details.
     * If no scopes are specified, empty set will be returned.
     */
    public @NotNull Set<Scope> supportedScopes() {
        return Collections.unmodifiableSet(supportedScopes);
    }

    /**
     * {@code TracingConfigurationParameters} builder.
     */
    @SuppressWarnings("PublicInnerClass") public static class Builder {
        /** Counterpart of {@code TracingConfigurationParameters} samplingRate. */
        private double samplingRate;

        /** Counterpart of {@code TracingConfigurationParameters} supportedScopes. */
        private Set<Scope> supportedScopes = Collections.emptySet();

        /**
         * Builder method that allows to set sampling rate.
         *
         * @param samplingRate Number between 0 and 1 that more or less reflects the probability of sampling specific trace.
         * 0 and 1 have special meaning here, 0 means never 1 means always. Default value is 0 (never).
         * @return {@code TracingConfigurationParameters} instance.
         */
        public @NotNull Builder withSamplingRate(double samplingRate) {
            if (samplingRate < MIN_SAMPLING_RATE || samplingRate > MAX_SAMPLING_RATE) {
                throw new IllegalArgumentException("Specified sampling rate=[" + samplingRate + "] has invalid value." +
                    " Should be between 0 and 1 including boundaries.");
            }
            this.samplingRate = samplingRate;

            return this;
        }

        /**
         * Builder method that allows to set supported scopes.
         *
         * @param supportedScopes Set of {@link Scope} that defines which sub-traces will be included in given trace.
         * In other words, if child's span scope is equals to parent's scope
         * or it belongs to the parent's span supported scopes, then given child span will be attached to the current trace,
         * otherwise it'll be skipped.
         * See {@link Span#isChainable(org.apache.ignite.internal.processors.tracing.Scope)} for more details.
         * @return {@code TracingConfigurationParameters} instance.
         */
        public @NotNull Builder withSupportedScopes(Set<Scope> supportedScopes) {
            this.supportedScopes = supportedScopes == null ? Collections.emptySet() : supportedScopes;

            return this;
        }

        /**
         * Builder's build() method.
         *
         * @return {@code TracingConfigurationParameters} instance.
         */
        public TracingConfigurationParameters build() {
            return new TracingConfigurationParameters(samplingRate, supportedScopes);
        }
    }
}
