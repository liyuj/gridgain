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

package org.apache.ignite.examples;

import org.apache.ignite.examples.datagrid.CacheContinuousAsyncQueryExample;
import org.apache.ignite.examples.datagrid.CacheContinuousQueryExample;
import org.apache.ignite.examples.datagrid.CacheContinuousQueryWithTransformerExample;
import org.apache.ignite.testframework.junits.common.GridAbstractExamplesTest;
import org.junit.Test;

/**
 */
public class CacheContinuousQueryExamplesSelfTest extends GridAbstractExamplesTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCacheContinuousAsyncQueryExample() throws Exception {
        CacheContinuousAsyncQueryExample.main(new String[] {});
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCacheContinuousQueryExample() throws Exception {
        CacheContinuousQueryExample.main(new String[] {});
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCacheContinuousQueryWithTransformerExample() throws Exception {
        CacheContinuousQueryWithTransformerExample.main(new String[] {});
    }
}
