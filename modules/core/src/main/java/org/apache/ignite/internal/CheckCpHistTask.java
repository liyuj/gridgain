/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTask;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.persistence.GridCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.persistence.checkpoint.CheckpointEntry;
import org.apache.ignite.internal.processors.cache.persistence.checkpoint.CheckpointHistory;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;

/**
 * Closure checks, that last checkpoint on applicable for particular groups.
 */
public class CheckCpHistTask implements ComputeTask<Map<UUID, Map<Integer, Set<Integer>>>, Boolean> {

    /** Reason of checkpoint, which can be triggered by this task. */
    public static final String CP_REASON = "required by other node that shutdown was gracefully";

    /** {@inheritDoc} */
    @Override public Map<CheckCpHistClosureJob, ClusterNode> map(
        List<ClusterNode> subgrid,
        Map<UUID, Map<Integer, Set<Integer>>> arg
    ) throws IgniteException {
        Map<CheckCpHistClosureJob, ClusterNode> res = new HashMap<>();

        for (ClusterNode node : subgrid) {
            if (arg.containsKey(node.id()))
                res.put(new CheckCpHistClosureJob(arg.get(node.id())), node);
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public ComputeJobResultPolicy result(ComputeJobResult res,
        List<ComputeJobResult> rcvd) throws IgniteException {
        if (!(boolean)res.getData())
            return ComputeJobResultPolicy.REDUCE;

        return ComputeJobResultPolicy.WAIT;
    }

    /** {@inheritDoc} */
    @Override public Boolean reduce(List<ComputeJobResult> results) throws IgniteException {
        for (ComputeJobResult result : results) {
            if (!(boolean)result.getData())
                return false;
        }

        return true;
    }

    /**
     * Job of checkpoint history task.
     */
    private static class CheckCpHistClosureJob implements ComputeJob {
        /** Logger. */
        @LoggerResource
        private IgniteLogger log;

        /** Auto-inject ignite instance. */
        @IgniteInstanceResource
        private Ignite ignite;

        /** Cancelled job flag. */
        private volatile boolean cancelled;

        /** list of group's ids. */
        Map<Integer, Set<Integer>> grpIds;

        /**
         * @param grpIds List of ids.
         */
        public CheckCpHistClosureJob(Map<Integer, Set<Integer>> grpIds) {
            this.grpIds = grpIds;
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            cancelled = true;
        }

        /** {@inheritDoc} */
        @Override public Boolean execute() throws IgniteException {
            log.info("Task called on node " + ignite.cluster().localNode());

            IgniteEx igniteEx = (IgniteEx)ignite;

            if (igniteEx.context().cache().context().database() instanceof GridCacheDatabaseSharedManager) {
                GridCacheSharedContext cctx = igniteEx.context().cache().context();
                GridCacheDatabaseSharedManager databaseMng = (GridCacheDatabaseSharedManager)cctx.database();
                CheckpointHistory cpHist = databaseMng.checkpointHistory();

                CheckpointEntry lastCp = cpHist.lastCheckpoint();
                try {

                    Map<Integer, CheckpointEntry.GroupState> staes = lastCp.groupState(cctx);

                    for (Integer grpId : grpIds.keySet()) {
                        if (cancelled)
                            return false;

                        if (!cpHist.isCheckpointApplicableForGroup(grpId, lastCp)) {
                            databaseMng.forceCheckpoint(CP_REASON);

                            break;
                        }

                        CheckpointEntry.GroupState groupState = staes.get(grpId);

                        for (int p : grpIds.get(grpId)) {
                            if (groupState.indexByPartition(p) < 0) {
                                databaseMng.forceCheckpoint(CP_REASON);

                                break;
                            }
                        }
                    }
                }
                catch (IgniteCheckedException e) {
                    log.warning("Can not read checkpoint [cp=" + lastCp.checkpointId() + ']', e);

                    return false;
                }
            }

            log.info("All grps are applicable: " + grpIds);

            return true;
        }
    }
}
