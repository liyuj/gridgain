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

package org.apache.ignite.spi.discovery.zk.internal;

import java.util.List;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.data.Stat;

/**
 *
 */
abstract class ZkAbstractChildrenCallback extends ZkAbstractCallabck implements AsyncCallback.Children2Callback {
    /**
     * @param rtState Runtime state.
     * @param impl Discovery impl.
     */
    ZkAbstractChildrenCallback(ZkRuntimeState rtState, ZookeeperDiscoveryImpl impl) {
        super(rtState, impl);
    }

    /** {@inheritDoc} */
    @Override public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {
        if (!onProcessStart())
            return;

        try {
            processResult0(rc, path, ctx, children, stat);

            onProcessEnd();
        }
        catch (Throwable e) {
            onProcessError(e);
        }
    }

    /**
     * @param rc
     * @param path
     * @param ctx
     * @param children
     * @param stat
     * @throws Exception If failed.
     */
    abstract void processResult0(int rc, String path, Object ctx, List<String> children, Stat stat)
        throws Exception;
}
