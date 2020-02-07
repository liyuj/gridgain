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

package org.apache.ignite.internal.processors.cache.checker.processor.workload;

import java.util.UUID;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.checker.processor.PipelineWorkload;
import org.apache.ignite.internal.processors.cache.checker.tasks.CollectPartitionKeysByBatchTask;

/**
 * Describes batch workload for {@link CollectPartitionKeysByBatchTask} include the pagination.
 */
public class Batch implements PipelineWorkload {
    /** Cache name. */
    private final String cacheName;

    /** Partition id. */
    private final int partId;

    /** Lower key, uses for pagination. The first request should set this value to null. */
    private final KeyCacheObject lowerKey;

    /** Session id. */
    private final long sessionId;

    /** Workload chain id. */
    private final UUID workloadChainId;

    /**
     *
     */
    public Batch(long sessionId, UUID workloadChainId, String cacheName, int partId, KeyCacheObject lowerKey) {
        this.sessionId = sessionId;
        this.workloadChainId = workloadChainId;
        this.cacheName = cacheName;
        this.partId = partId;
        this.lowerKey = lowerKey;
    }

    /**
     *
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     *
     */
    public int partitionId() {
        return partId;
    }

    /**
     *
     */
    public KeyCacheObject lowerKey() {
        return lowerKey;
    }

    /** {@inheritDoc} */
    @Override public long sessionId() {
        return sessionId;
    }

    /** {@inheritDoc} */
    @Override public UUID workloadChainId() {
        return workloadChainId;
    }
}