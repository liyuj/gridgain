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
 *
 */

package org.apache.ignite.internal.processors.cache.distributed.dht.preloader;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Partition counters map.
 */
public class IgniteDhtPartitionCountersMap2 implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private Map<Integer, CachePartitionFullCountersMap> map;

    /**
     * @return {@code True} if map is empty.
     */
    public synchronized boolean empty() {
        return map == null || map.isEmpty();
    }

    /**
     * @param cacheId Cache ID.
     * @param cntrMap Counters map.
     */
    public synchronized void putIfAbsent(int cacheId, CachePartitionFullCountersMap cntrMap) {
        if (map == null)
            map = new HashMap<>();

        if (!map.containsKey(cacheId))
            map.put(cacheId, cntrMap);
    }

    /**
     * @param cacheId Cache ID.
     * @return Counters map.
     */
    public synchronized CachePartitionFullCountersMap get(int cacheId) {
        if (map == null)
            return null;

        CachePartitionFullCountersMap cntrMap = map.get(cacheId);

        if (cntrMap == null)
            return null;

        return cntrMap;
    }
}
