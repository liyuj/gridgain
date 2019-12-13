/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.verify;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Repair meta including:
 * <ul>
 * <li>boolean flag that indicates whether data was fixed or not;</li>
 * <li>value that was used to fix entry;</li>
 * <li>repair algorithm that was used;</li>
 * </ul>
 */
public class PartitionReconciliationRepairMeta extends IgniteDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Boolean flag that indicates whether data was fixed or not. */
    private boolean fixed;

    /** Value that was used to fix entry. */
    // TODO: 06.12.19 Use String view and Binary view instead.
    private CacheObject val;

    /** Repair algorithm that was used. */
    private RepairAlgorithm repairAlg;

    /**
     * Default constructor for externalization.
     */
    public PartitionReconciliationRepairMeta() {
    }

    /**
     * Constructor.
     *
     * @param fixed Boolean flag that indicates whether data was fixed or not.
     * @param val Value that was used to fix entry.
     * @param repairAlg Repair algorithm that was used.
     */
    public PartitionReconciliationRepairMeta(boolean fixed, CacheObject val,
        RepairAlgorithm repairAlg) {
        this.fixed = fixed;
        this.val = val;
        this.repairAlg = repairAlg;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeBoolean(fixed);
        out.writeObject(val);
        U.writeEnum(out, repairAlg);
    }

    /** {@inheritDoc} */
    @Override
    protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        fixed = in.readBoolean();
        val = (CacheObject)in.readObject();
        repairAlg = RepairAlgorithm.fromOrdinal(in.readByte());
    }

    /**
     * @return Boolean flag that indicates whether data was fixed or not.
     */
    public boolean fixed() {
        return fixed;
    }

    /**
     * @return Value that was used to fix entry.
     */
    public CacheObject value() {
        return val;
    }

    /**
     * @return Repair algorithm that was used.
     */
    public RepairAlgorithm repairAlg() {
        return repairAlg;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(PartitionReconciliationRepairMeta.class, this);
    }

}