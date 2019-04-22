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

package org.apache.ignite.loadtests.direct.newnodes;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.junits.common.GridCommonTest;

/**
 *
 */
@GridCommonTest(group = "Load Test")
public class GridSingleSplitsNewNodesMulticastLoadTest extends GridSingleSplitsNewNodesAbstractLoadTest {
    /** {@inheritDoc} */
    @Override protected DiscoverySpi getDiscoverySpi(IgniteConfiguration cfg) {
        DiscoverySpi discoSpi = cfg.getDiscoverySpi();

        assert discoSpi instanceof TcpDiscoverySpi : "Wrong default SPI implementation.";

        return discoSpi;
    }
}
