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

namespace Apache.Ignite.Core.Impl.Cache.Near
{
    using System.Threading;

    /// <summary>
    /// <see cref="NearCache{TK, TV}"/> entry.
    /// </summary>
    internal class NearCacheEntry<T> : INearCacheEntry<T> // TODO: Why not struct?
    {
        /** */
        private volatile int _hasValue;
        
        /** */
        private T _value;

        /// <summary>
        /// Initializes a new instance of the <see cref="NearCacheEntry{T}"/> class.
        /// </summary>
        /// <param name="hasValue">Whether this entry has a value.</param>
        /// <param name="value">Value.</param>
        public NearCacheEntry(bool hasValue = false, T value = default(T))
        {
            _hasValue = hasValue ? 1 : 0;
            _value = value;
        }

        /** <inheritdoc /> */
        public bool HasValue
        {
            get { return _hasValue > 0; }
        }

        /** <inheritdoc /> */
        public T Value
        {
            get { return _value; }
        }

        /** <inheritdoc /> */
        public void SetValueIfEmpty(T value)
        {
            // Disable "a reference to a volatile field will not be treated as volatile": not an issue with Interlocked.
            #pragma warning disable 0420
            if (Interlocked.CompareExchange(ref _hasValue, 1, 0) == 0)
            {
                _value = value;
            }
            #pragma warning restore 0420
        }
    }
}