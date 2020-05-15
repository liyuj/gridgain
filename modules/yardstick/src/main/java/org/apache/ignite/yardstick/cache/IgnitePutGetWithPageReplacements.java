/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.yardstick.cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.ignite.events.EventType.EVT_PAGE_REPLACEMENT_STARTED;

/**
 * Ignite benchmark that performs payload with active page replacement.
 */
public class IgnitePutGetWithPageReplacements extends IgniteCacheAbstractBenchmark<Integer, Object> {
    /** Cache name. */
    private static final String CACHE_NAME = "CacheWithReplacement";

    /** Cache configuration */
    private CacheConfiguration<Integer, Object> cacheWithRep = new CacheConfiguration<>(CACHE_NAME);

    /** Active replacement flag. */
    private AtomicBoolean replacement = new AtomicBoolean();

    /** Payload counter. */
    private AtomicInteger progress = new AtomicInteger();

    /** In mem reg capacity. */
    private volatile int replCntr = Integer.MAX_VALUE / 2;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        ignite().events().remoteListen(new IgniteBiPredicate<UUID, Event>() {
            @Override public boolean apply(UUID uuid, Event evt) {
                if (evt.type() == EVT_PAGE_REPLACEMENT_STARTED) {
                    replacement.set(true);

                    return false;
                }

                return true;
            }
        }, null, EVT_PAGE_REPLACEMENT_STARTED);

        int portion = 100;

        Map<Integer, TestValue> putMap = new HashMap<>(portion, 1.f);

        while (progress.get() < 2 * replCntr) {
            putMap.clear();

            int progress0 = progress.getAndAdd(portion);

            for (int i = 0; i < portion; i++)
                putMap.put(progress0 + i, new TestValue(progress0 + i));

            cache().putAll(putMap);

            if (progress0 % 1000 == 0)
                BenchmarkUtils.println("progress=" + progress);

            if (replacement.compareAndSet(true, false)) {
                replCntr = progress.get();

                BenchmarkUtils.println("replCntr=" + replCntr);
            }
        }

        BenchmarkUtils.println("DataRegion fullfill complete. progress=" + progress + " replCntr=" + replCntr + ".");

        int cacheSize = 0;

        try (QueryCursor cursor = cache.query(new ScanQuery())) {
            for (Object o : cursor)
                cacheSize++;
        }

        BenchmarkUtils.println("cache size=" + cacheSize);
    }

    /** */
    @Override protected IgniteCache<Integer, Object> cache() {
        return ignite().getOrCreateCache(cacheWithRep);
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> map) throws Exception {
        int portion = 100;

        Map<Integer, TestValue> putMap = new HashMap<>(portion, 1.f);

        int progress0 = progress.getAndAdd(portion);

        for (int i = 0; i < portion; i++)
            putMap.put(progress0 + i, new TestValue(progress0 + i));

        cache().putAll(putMap);

        if (progress0 % 1000 == 0)
            BenchmarkUtils.println("progress=" + progress);

        return true;
    }

    /**
     * Class for test purpose.
     */
    private static class TestValue {
        /** */
        private int id;

        /** */
        @QuerySqlField(index = true)
        private final byte[] payload = new byte[64];

        /**
         * @param id ID.
         */
        private TestValue(int id) {
            this.id = id;
        }

        /**
         * @return ID.
         */
        public int getId() {
            return id;
        }

        /**
         * @return Payload.
         */
        public boolean hasPayload() {
            return payload != null;
        }
    }
}
