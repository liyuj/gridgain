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

package org.apache.ignite.internal.processors.cache.persistence.db.wal.crc;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.pagemem.wal.WALIterator;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteReplayWalIteratorInvalidCrcTest extends IgniteAbstractWalIteratorInvalidCrcTest {
    /** {@inheritDoc} */
    @NotNull @Override protected WALMode getWalMode() {
        return WALMode.LOG_ONLY;
    }

    /** {@inheritDoc} */
    @Override protected WALIterator getWalIterator(
        IgniteWriteAheadLogManager walMgr,
        boolean ignoreArchiveDir
    ) throws IgniteCheckedException {
        if (ignoreArchiveDir)
            throw new UnsupportedOperationException(
                "Cannot invoke \"getWalIterator\" with true \"ignoreArchiveDir\" parameter value."
            );
        else
            return walMgr.replay(null);
    }

    /**
     * {@inheritDoc}
     * Case is not relevant to the replay iterator.
     */
    @Test
    @Override public void testNotTailCorruptedPtr() {
    }
}
