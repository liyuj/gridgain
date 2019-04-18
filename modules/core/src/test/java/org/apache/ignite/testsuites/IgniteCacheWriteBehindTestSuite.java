/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * (you may not use this file except in compliance with the License.
 * (You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * (distributed under the License is distributed on an "AS IS" BASIS,
 * (WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * (See the License for the specific language governing permissions and
 * (limitations under the License.
 */

package org.apache.ignite.testsuites;

import java.util.Collection;
import junit.framework.TestSuite;
import org.apache.ignite.internal.processors.cache.GridCachePartitionedWritesTest;
import org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStoreLocalTest;
import org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStoreMultithreadedSelfTest;
import org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStorePartitionedMultiNodeSelfTest;
import org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStorePartitionedTest;
import org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStoreReplicatedTest;
import org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStoreSelfTest;
import org.apache.ignite.internal.processors.cache.store.IgnteCacheClientWriteBehindStoreAtomicTest;
import org.apache.ignite.internal.processors.cache.store.IgnteCacheClientWriteBehindStoreNonCoalescingTest;
import org.apache.ignite.internal.processors.cache.store.IgnteCacheClientWriteBehindStoreTxTest;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Test suite that contains all tests for {@link org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStore}.
 */
@RunWith(AllTests.class)
public class IgniteCacheWriteBehindTestSuite {
    /**
     * @return Ignite Bamboo in-memory data grid test suite.
     */
    public static TestSuite suite() {
        return suite(null);
    }

    /**
     * @param ignoredTests Ignored tests.
     * @return IgniteCache test suite.
     */
    public static TestSuite suite(Collection<Class> ignoredTests) {
        TestSuite suite = new TestSuite("Write-Behind Store Test Suite");

        // Write-behind tests.
        GridTestUtils.addTestIfNeeded(suite, GridCacheWriteBehindStoreSelfTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCacheWriteBehindStoreMultithreadedSelfTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCacheWriteBehindStoreLocalTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCacheWriteBehindStoreReplicatedTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCacheWriteBehindStorePartitionedTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCacheWriteBehindStorePartitionedMultiNodeSelfTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCachePartitionedWritesTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, IgnteCacheClientWriteBehindStoreAtomicTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, IgnteCacheClientWriteBehindStoreTxTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, IgnteCacheClientWriteBehindStoreNonCoalescingTest.class, ignoredTests);

        return suite;
    }
}
