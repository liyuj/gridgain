﻿/*
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

namespace Apache.Ignite.Core.Tests.ApiParity
{
    using System.Collections.Generic;
    using NUnit.Framework;

    /// <summary>
    /// Tests that <see cref="IDataRegionMetrics"/> has all APIs from Java Ignite interface.
    /// </summary>
    public class DataRegionMetricsParityTest
    {
        /** Known name mappings. */
        private static readonly Dictionary<string, string> KnownMappings = new Dictionary<string, string>
        {
            {"PagesFillFactor", "PageFillFactor"},
            {"PagesReplaceRate", "PageReplaceRate"},
            {"PagesReplaceAge", "PageReplaceAge"}
        };

        /** Properties that are missing on .NET side. */
        private static readonly string[] MissingProperties = {};

        /// <summary>
        /// Tests the API parity.
        /// </summary>
        [Test]
        public void TestDataRegionMetrics()
        {
            ParityTest.CheckInterfaceParity(
                @"modules\core\src\main\java\org\apache\ignite\DataRegionMetrics.java",
                typeof(IDataRegionMetrics),
                knownMappings: KnownMappings,
                knownMissingMembers: MissingProperties);
        }
    }
}