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

package org.apache.ignite.testsuites;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.apache.ignite.logger.java.JavaLoggerTest;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Logging self-test suite.
 */
@RunWith(AllTests.class)
public class IgniteLoggingSelfTestSuite {
    /**
    * @return P2P tests suite.
    */
   public static TestSuite suite() {
       TestSuite suite = new TestSuite("Ignite Logging Test Suite");

       suite.addTest(new JUnit4TestAdapter(JavaLoggerTest.class));

       return suite;
   }
}
