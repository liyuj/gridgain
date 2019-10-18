/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.glowroot.converter.model;

/**
 * Ignite transaction commit trace item produced by {@code org.apache.ignite.glowroot.TransactionAspect}
 */
public final class CommitTraceItem extends TraceItem {

    /** Label.**/
    private final String lb;

    /**
     * Constructor.
     *
     * @param glowrootTx Glowroot transaction.
     * @param durationNanos Trace duration in nanoseconds.
     * @param offsetNanos Trace offset in nanoseconds from the begining of glowroot transaction.
     * @param lb Label.
     */
    public CommitTraceItem(GlowrootTransactionMeta glowrootTx, long durationNanos, long offsetNanos, String lb) {
        super(glowrootTx, durationNanos, offsetNanos);

        this.lb = lb;
    }

    /**
     * @return Label.
     */
    public String label() {
        return lb;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "CommitTraceItem{" +
            "label='" + lb + '\'' +
            '}';
    }
}
