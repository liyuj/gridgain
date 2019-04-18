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

package org.apache.ignite.internal.visor.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for information about cache partitions.
 */
public class VisorCachePartitions extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private Map<Integer, Long> primary;

    /** */
    private Map<Integer, Long> backup;

    /**
     * Default constructor.
     */
    public VisorCachePartitions() {
        primary = new HashMap<>();
        backup = new HashMap<>();
    }

    /**
     * Add primary partition descriptor.
     *
     * @param partId Partition id.
     * @param cnt Number of primary keys in partition.
     */
    public void addPrimary(int partId, long cnt) {
       primary.put(partId, cnt);
    }

    /**
     * Add backup partition descriptor.
     *
     * @param partId Partition id.
     * @param cnt Number of backup keys in partition.
     */
    public void addBackup(int partId, long cnt) {
       backup.put(partId, cnt);
    }

    /**
     * @return Get list of primary partitions.
     */
    public Map<Integer, Long> getPrimary() {
        return primary;
    }

    /**
     * @return Get list of backup partitions.
     */
    public Map<Integer, Long> getBackup() {
        return backup;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeMap(out, primary);
        U.writeMap(out, backup);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        primary = U.readMap(in);
        backup = U.readMap(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCachePartitions.class, this);
    }
}
