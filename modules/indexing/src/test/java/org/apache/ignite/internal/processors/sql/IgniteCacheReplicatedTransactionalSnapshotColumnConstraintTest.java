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

package org.apache.ignite.internal.processors.sql;

import org.apache.ignite.cache.CacheAtomicityMode;
import org.jetbrains.annotations.NotNull;

/** */
public class IgniteCacheReplicatedTransactionalSnapshotColumnConstraintTest
    extends IgniteCacheReplicatedAtomicColumnConstraintsTest {
    /** {@inheritDoc} */
    @NotNull @Override protected CacheAtomicityMode atomicityMode() {
        return CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT;
    }

    /** */
    @Override public void testPutTooLongStringValueFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongStringKeyFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongStringValueFieldFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongStringKeyFieldFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongStringKeyFail2() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongStringKeyFail3() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalValueFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalKeyFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalKeyFail2() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalValueFieldFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalValueFieldFail2() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalKeyFieldFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalValueScaleFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalKeyScaleFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalKeyScaleFail2() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalValueFieldScaleFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalValueFieldScaleFail2() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }

    /** */
    @Override public void testPutTooLongDecimalKeyFieldScaleFail() {
        fail("https://issues.apache.org/jira/browse/IGNITE-10066");
    }
}
