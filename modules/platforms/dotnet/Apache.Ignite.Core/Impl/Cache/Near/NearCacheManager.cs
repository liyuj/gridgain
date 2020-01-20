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
    using System.Diagnostics;
    using Apache.Ignite.Core.Cache.Configuration;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.Impl.Binary.IO;
    using Apache.Ignite.Core.Impl.Common;
    using Apache.Ignite.Core.Impl.Memory;

    /// <summary>
    /// Manages <see cref="NearCache{TK,TV}"/> instances.
    /// Multiple <see cref="CacheImpl{TK,TV}"/> instances can exist for a given cache, and all of them share the same
    /// <see cref="NearCache{TK,TV}"/> instance.
    /// </summary>
    internal class NearCacheManager
    {
        /// <summary>
        /// Near caches per cache id.
        /// Multiple <see cref="CacheImpl{TK,TV}"/> instances can point to the same Ignite cache,
        /// and share one <see cref="NearCache{TK,TV}"/> instance. 
        /// </summary> 
        private readonly CopyOnWriteConcurrentDictionary<int, INearCache> _nearCaches
            = new CopyOnWriteConcurrentDictionary<int, INearCache>();

        /// <summary>
        /// Gets the near cache.
        /// <para />
        /// Same Ignite cache can be retrieved with different generic type parameters, e.g.:
        /// Ignite.GetCache{int, int}("my-cache") and Ignite.GetCache{string, string}("my-cache").
        /// This is not recommended, and we assume it does not usually happen, so we optimize for only one set of
        /// type parameters, which avoids boxing and increased memory usage.
        ///
        /// When the use case above is detected, we downgrade near cache map to {object, object}, which will cause
        /// more boxing and casting. 
        /// </summary>
        public INearCache GetNearCache<TK, TV>(string cacheName,
            NearCacheConfiguration nearCacheConfiguration)
        {
            Debug.Assert(!string.IsNullOrEmpty(cacheName));
            Debug.Assert(nearCacheConfiguration != null);

            var cacheId = BinaryUtils.GetCacheId(cacheName);
            
            INearCache nearCache;
            return _nearCaches.TryGetValue(cacheId, out nearCache) 
                ? nearCache 
                : _nearCaches.GetOrAdd(cacheId, id => new NearCache<TK, TV>());
        }

        /// <summary>
        /// Reads cache entry from a stream and updates the near cache.
        /// </summary>
        public void Update(int cacheId, IBinaryStream stream, Marshaller marshaller)
        {
            INearCache nearCache;
            if (!_nearCaches.TryGetValue(cacheId, out nearCache))
            {
                return;
            }
            
            nearCache.Update(stream, marshaller);
        }

        public void Evict(int cacheId, PlatformMemoryStream stream, Marshaller marshaller)
        {
            INearCache nearCache;
            if (!_nearCaches.TryGetValue(cacheId, out nearCache))
            {
                return;
            }
            
            // TODO: Evict is called on replace, update and other things like that -
            // unnecessary callback that will happen anyway.
            // Can we get rid of it? Is it possible to miss updates if we don't use evict calls?
            // E.g.: entry is evicted from Near in Java and we no longer get updates for it, making it stale.
            // TODO: Investigate tests for this in Java.
            nearCache.Evict(stream, marshaller);
        }

        /// <summary>
        /// Stops near cache.
        /// </summary>
        public void Stop(int cacheId)
        {
            INearCache cache;
            if (_nearCaches.TryGetValue(cacheId, out cache))
            {
                cache.Clear();
                _nearCaches.Remove(cacheId);
            }
        }
    }
}