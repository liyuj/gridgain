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

package org.apache.ignite.internal.processors.cache.persistence.file;

import com.google.common.base.Strings;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Checks if Direct IO can be set up if no persistent store is configured
 */
@RunWith(JUnit4.class)
public class IgniteNativeIoWithNoPersistenceTest extends GridCommonAbstractTest {

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration configuration = super.getConfiguration(igniteInstanceName);

        configuration.setDataStorageConfiguration(new DataStorageConfiguration()
            .setDefaultDataRegionConfiguration(new DataRegionConfiguration()));

        return configuration;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * Checks simple launch with native IO.
     * @throws Exception if failed
     */
    @Test
    public void testDirectIoHandlesNoPersistentGrid() throws Exception {
        IgniteEx ignite = startGrid(0);

        ignite.active(true);

        IgniteCache<Object, Object> cache = ignite.getOrCreateCache("cache");

        for (int i = 0; i < 100; i++)
            cache.put(i, valueForKey(i));


        stopAllGrids();
    }

    /**
     * @param i key.
     * @return value with extra data, which allows to verify
     */
    @NotNull private String valueForKey(int i) {
        return Strings.repeat(Integer.toString(i), 10);
    }
}
