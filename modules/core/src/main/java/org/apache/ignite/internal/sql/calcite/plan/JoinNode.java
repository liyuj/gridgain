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
package org.apache.ignite.internal.sql.calcite.plan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.util.ImmutableIntList;
import org.apache.ignite.internal.sql.calcite.expressions.Condition;
import org.apache.ignite.internal.sql.calcite.rels.IgnitePlanVisitor;

/**
 * TODO: Add class description.
 */
public class JoinNode implements PlanNode {
        private PlanNode left;
        private PlanNode right;
        private ImmutableIntList leftJoinKeys;
        private ImmutableIntList rightJoinKeys;
        private Condition joinCond;
        private JoinRelType joinType;
        private JoinAlgorithm joinAlg;

    public JoinNode() {
    }

    public JoinNode(PlanNode left, PlanNode right, ImmutableIntList leftJoinKeys, ImmutableIntList rightJoinKeys,
        Condition joinCond, JoinRelType joinType,
        JoinAlgorithm joinAlg) {
        this.left = left;
        this.right = right;
        this.leftJoinKeys = leftJoinKeys;
        this.rightJoinKeys = rightJoinKeys;
        this.joinCond = joinCond;
        this.joinType = joinType;
        this.joinAlg = joinAlg;
    }

    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(joinAlg.ordinal());
        out.writeInt(joinType.ordinal());
        out.writeObject(joinCond);
        out.writeObject(leftJoinKeys);
        out.writeObject(rightJoinKeys);
        out.writeObject(left);
        out.writeObject(right);

    }

    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        joinAlg = JoinAlgorithm.values()[in.readInt()];
        joinType = JoinRelType.values()[in.readInt()];
        joinCond = (Condition)in.readObject();
        leftJoinKeys = (ImmutableIntList)in.readObject();
        rightJoinKeys = (ImmutableIntList)in.readObject();
        left = (PlanNode)in.readObject();
        right = (PlanNode)in.readObject();
    }

    public PlanNode left() {
        return left;
    }

    public PlanNode light() {
        return right;
    }

    public ImmutableIntList leftJoinKeys() {
        return leftJoinKeys;
    }

    public ImmutableIntList rightJoinKeys() {
        return rightJoinKeys;
    }

    public Condition joinCond() {
        return joinCond;
    }

    public JoinRelType joinType() {
        return joinType;
    }

    public JoinAlgorithm joinAlgorithm() {
        return joinAlg;
    }

    @Override public String toString(int level) {
        String margin = String.join("", Collections.nCopies(level, "  "));

        StringBuilder sb = new StringBuilder("\n");

        sb.append(margin)
            .append("JoinNode [cond=")
            .append(joinCond)
            .append(", joinType=")
            .append(joinType)
            .append(", joinAlg=" + joinAlg)
            .append("]")
            .append(left.toString(level + 1))
            .append(right.toString(level + 1));

        return sb.toString();
    }

    @Override public void accept(IgnitePlanVisitor visitor) {
        visitor.onJoin(this);
    }

    @Override public List<PlanNode> inputs() {
        return Arrays.asList(left, right);
    }

    @Override public String toString() {
        return toString(0);
    }

    public enum JoinAlgorithm {
        NESTED_LOOPS
    }
}
