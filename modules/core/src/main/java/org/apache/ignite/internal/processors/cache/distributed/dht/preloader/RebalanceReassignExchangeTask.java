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

package org.apache.ignite.internal.processors.cache.distributed.dht.preloader;

import org.apache.ignite.internal.processors.cache.CachePartitionExchangeWorkerTask;

/**
 *
 */
public class RebalanceReassignExchangeTask implements CachePartitionExchangeWorkerTask {
    /** */
    private final GridDhtPartitionExchangeId exchId;

    /** */
    private final GridDhtPartitionsExchangeFuture exchFut;

    /**
     * @param exchId Exchange ID.
     * @param exchFut Exchange future.
     */
    public RebalanceReassignExchangeTask(GridDhtPartitionExchangeId exchId, GridDhtPartitionsExchangeFuture exchFut) {
        assert exchId != null;
        assert exchFut != null;

        this.exchId = exchId;
        this.exchFut = exchFut;
    }

    /** {@inheritDoc} */
    @Override public boolean skipForExchangeMerge() {
        return true;
    }

    /**
     * @return Exchange ID.
     */
    public GridDhtPartitionExchangeId exchangeId() {
        return exchId;
    }

    /**
     * @return Exchange future.
     */
    public GridDhtPartitionsExchangeFuture future() {
        return exchFut;
    }
}
