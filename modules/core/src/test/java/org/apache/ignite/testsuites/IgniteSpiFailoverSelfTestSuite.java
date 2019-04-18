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

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.apache.ignite.spi.failover.always.GridAlwaysFailoverSpiConfigSelfTest;
import org.apache.ignite.spi.failover.always.GridAlwaysFailoverSpiSelfTest;
import org.apache.ignite.spi.failover.always.GridAlwaysFailoverSpiStartStopSelfTest;
import org.apache.ignite.spi.failover.jobstealing.GridJobStealingFailoverSpiConfigSelfTest;
import org.apache.ignite.spi.failover.jobstealing.GridJobStealingFailoverSpiOneNodeSelfTest;
import org.apache.ignite.spi.failover.jobstealing.GridJobStealingFailoverSpiSelfTest;
import org.apache.ignite.spi.failover.jobstealing.GridJobStealingFailoverSpiStartStopSelfTest;
import org.apache.ignite.spi.failover.never.GridNeverFailoverSpiSelfTest;
import org.apache.ignite.spi.failover.never.GridNeverFailoverSpiStartStopSelfTest;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Failover SPI self-test suite.
 */
@RunWith(AllTests.class)
public class IgniteSpiFailoverSelfTestSuite {
    /**
     * @return Failover SPI tests suite.
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite("Ignite Failover SPI Test Suite");

        // Always failover.
        suite.addTest(new JUnit4TestAdapter(GridAlwaysFailoverSpiSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridAlwaysFailoverSpiStartStopSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridAlwaysFailoverSpiConfigSelfTest.class));

        // Never failover.
        suite.addTest(new JUnit4TestAdapter(GridNeverFailoverSpiSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridNeverFailoverSpiStartStopSelfTest.class));

        // Job stealing failover.
        suite.addTest(new JUnit4TestAdapter(GridJobStealingFailoverSpiSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridJobStealingFailoverSpiOneNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridJobStealingFailoverSpiStartStopSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridJobStealingFailoverSpiConfigSelfTest.class));

        return suite;
    }
}
