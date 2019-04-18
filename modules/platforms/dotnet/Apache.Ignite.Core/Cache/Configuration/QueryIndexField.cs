﻿/*
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

namespace Apache.Ignite.Core.Cache.Configuration
{
    using Apache.Ignite.Core.Impl.Common;

    /// <summary>
    /// Represents an indexed field.
    /// </summary>
    public class QueryIndexField
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="QueryIndexField"/> class.
        /// </summary>
        public QueryIndexField()
        {
            // No-op.
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="QueryIndexField"/> class.
        /// </summary>
        /// <param name="name">The name.</param>
        public QueryIndexField(string name)
        {
            IgniteArgumentCheck.NotNullOrEmpty(name, "name");

            Name = name;
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="QueryIndexField"/> class.
        /// </summary>
        /// <param name="name">The name.</param>
        /// <param name="isDescending">Sort direction.</param>
        public QueryIndexField(string name, bool isDescending) : this (name)
        {
            IsDescending = isDescending;
        }

        /// <summary>
        /// Gets the name.
        /// </summary>
        public string Name { get; set; }

        /// <summary>
        /// Gets a value indicating whether this index is descending. Default is false.
        /// </summary>
        public bool IsDescending { get; set; }
    }
}
