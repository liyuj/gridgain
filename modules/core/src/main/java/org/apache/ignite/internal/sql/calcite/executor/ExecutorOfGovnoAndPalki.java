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
package org.apache.ignite.internal.sql.calcite.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.GridTopic;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.managers.communication.GridIoPolicy;
import org.apache.ignite.internal.managers.communication.GridMessageListener;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.IgniteInternalCache;
import org.apache.ignite.internal.processors.cache.QueryCursorImpl;
import org.apache.ignite.internal.processors.cache.query.GridCacheQueryMarshallable;
import org.apache.ignite.internal.sql.calcite.CalciteIndexing;
import org.apache.ignite.internal.sql.calcite.IgniteTable;
import org.apache.ignite.internal.sql.calcite.iterators.OutputOp;
import org.apache.ignite.internal.sql.calcite.iterators.PhysicalPlanCreator;
import org.apache.ignite.internal.sql.calcite.iterators.ReceiverOp;
import org.apache.ignite.internal.sql.calcite.plan.OutputNode;
import org.apache.ignite.internal.sql.calcite.plan.PlanStep;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.marshaller.Marshaller;
import org.apache.ignite.plugin.extensions.communication.Message;

import static org.apache.ignite.internal.sql.calcite.plan.PlanStep.Site.ALL_NODES;

/**
 * Made in the worst tradition. Do not use it in production code.
 */
public class ExecutorOfGovnoAndPalki implements GridMessageListener {

    private final AtomicLong qryIdCntr = new AtomicLong();

    private GridKernalContext ctx;

    private CalciteIndexing indexing;

    private Marshaller marshaller;

    private final Map<T2<UUID, Long>, Map<Integer, ReceiverOp>> receivers = new HashMap<>();// TODO clean maps

    private final Map<T2<UUID, Long>, Map<Integer, List<List<?>>>> receivedResults = new HashMap<>();// TODO clean maps

    private List<GridCacheContext> caches = new ArrayList<>();

    private Map<T2<UUID, Long>, IgniteInternalFuture<FieldsQueryCursor<List<?>>>> futs = new HashMap<>();

    /**
     *
     */
    public ExecutorOfGovnoAndPalki(GridKernalContext ctx, CalciteIndexing indexing) {
        this.ctx = ctx;
        this.indexing = indexing;
        marshaller = ctx.config().getMarshaller();
    }

    /**
     *
     */
    public List<FieldsQueryCursor<List<?>>> execute(List<PlanStep> multiStepPlan) {
        try {
            FieldsQueryCursor<List<?>> cursor = submitQuery(multiStepPlan).get();

            return Collections.singletonList(cursor);
        }
        catch (IgniteCheckedException e) {
            throw U.convertException(e);
        }
    }

    /**
     *
     */
    private IgniteInternalFuture<FieldsQueryCursor<List<?>>> submitQuery(List<PlanStep> multiStepPlan) {
        assert multiStepPlan.get(multiStepPlan.size() - 1).plan() instanceof OutputNode; // Last node is expected to be the output node.

        UUID locNode = ctx.localNodeId();
        long cntr = qryIdCntr.getAndIncrement();

        T2<UUID, Long> qryId = new T2<>(locNode, cntr);

        List<PlanStep> locNodePlan = new ArrayList<>(); // Plan for the local node.
        List<PlanStep> globalPlan = new ArrayList<>(); // Plan for other data nodes.

        for (PlanStep step : multiStepPlan) {
            locNodePlan.add(step);

            if (step.site() == ALL_NODES)
                globalPlan.add(step);
        }

        GridFutureAdapter<FieldsQueryCursor<List<?>>> qryFut = new GridFutureAdapter<>();

        synchronized (this) {
            futs.put(qryId, qryFut);
        }

        runLocalPlan(locNodePlan, qryId.getValue(), qryId.getKey(), qryFut);

        locNodePlan.get(locNodePlan.size() - 1);

        sendPlansForExecution(qryId, globalPlan);

        return qryFut;
    }

    private void sendPlansForExecution(T2<UUID, Long> qryId, List<PlanStep> plan) {
        try {
            QueryRequest req = new QueryRequest(qryId, plan);

            for (ClusterNode node : ctx.discovery().remoteNodes()) {

                if (!node.isClient()) {
                    send(req, node);
                }
            }
        }
        catch (IgniteCheckedException e) {
            throw U.convertException(e);
        }
    }

    /**
     *
     */
    public void sendResult(List<List<?>> result, int linkId, T2<UUID, Long> qryId, UUID dest) throws IgniteCheckedException {

        System.out.println("sendResult!!! localNodeId=" + ctx.localNodeId() + ", result=" + result);

        QueryResponse res = new QueryResponse(result, linkId, qryId.getKey(), qryId.getValue());

        send(res, ctx.discovery().node(dest));
    }

    private void send(Message msg, ClusterNode node) throws IgniteCheckedException {
        if (msg instanceof GridCacheQueryMarshallable)
            ((GridCacheQueryMarshallable)msg).marshall(marshaller);

        ctx.io().sendToGridTopic(node, GridTopic.TOPIC_QUERY, msg, GridIoPolicy.QUERY_POOL);
    }

    /**
     *
     */
    @Override public void onMessage(UUID nodeId, Object msg, byte plc) {
        if (msg instanceof GridCacheQueryMarshallable)
            ((GridCacheQueryMarshallable)msg).unmarshall(marshaller, ctx);

        System.out.println("onMessage curNode=" + ctx.localNodeId() + ", node =" + nodeId + ", msg= " + msg);

        if (msg instanceof QueryRequest) {
            QueryRequest qryReq = (QueryRequest)msg;

            runLocalPlan(qryReq.plans(), qryReq.queryId(), qryReq.queryNode(), null);
        }
        else if (msg instanceof QueryResponse) {
            onResult((QueryResponse)msg);
        }
        else
            throw new IgniteException("unknown message:" + msg);

    }

    private void runLocalPlan(List<PlanStep> plans, Long qryId, UUID qryNode,
        GridFutureAdapter<FieldsQueryCursor<List<?>>> qryFut) {
        int dataCnt = ctx.discovery().aliveServerNodes().size();

        T2<UUID, Long> globalQryId = new T2<>(qryNode, qryId);

        PhysicalPlanCreator physicalPlanCreator = null;

        for (PlanStep step : plans) {
            physicalPlanCreator = new PhysicalPlanCreator(dataCnt, globalQryId, this);

            step.plan().accept(physicalPlanCreator);

            List<ReceiverOp> recList = physicalPlanCreator.receivers();

            if (!recList.isEmpty()) {
                synchronized (receivers) {
                    Map<Integer, ReceiverOp> qryRec = receivers.computeIfAbsent(globalQryId, k -> new HashMap<>());

                    for (ReceiverOp receiverOp : recList) {
                        Map<Integer, List<List<?>>> resultsForQry = receivedResults.get(globalQryId);

                        if (resultsForQry != null) {
                            List<List<?>> res = resultsForQry.remove(receiverOp.linkId());

                            if (res != null) {
                                receiverOp.onResult(res);

                                continue;
                            }
                        }

                        qryRec.put(receiverOp.linkId(), receiverOp);
                    }
                }
            }

            physicalPlanCreator.root().init();
        }

        if (qryFut != null) {// We are on initiator.
            assert physicalPlanCreator.root() != null;

            OutputOp outputOp = (OutputOp)physicalPlanCreator.root(); // Root plan on initiator is expected to be the last.

            outputOp.listen(new IgniteInClosure<IgniteInternalFuture<List<List<?>>>>() {
                @Override public void apply(IgniteInternalFuture<List<List<?>>> future) {
                    try {
                        QueryCursorImpl<List<?>> cur = new QueryCursorImpl<>(future.get());

                        qryFut.onDone(cur);
                    }
                    catch (IgniteCheckedException e) {
                        qryFut.onDone(e);
                    }
                }
            });
        }
    }


    private void onResult(QueryResponse response) {
        List<List<?>> res = response.result();

        T2<UUID, Long> qryId = new T2<>(response.queryNode(), response.queryId());

        int linkId = response.linkId();

        synchronized (receivers) {
            Map<Integer, ReceiverOp> qryReceivers = receivers.get(qryId);

            ReceiverOp rec = qryReceivers == null ? null : qryReceivers.remove(linkId);

            if (rec == null) { // We have a race here - receiver hasn't been registered yet.
                Map<Integer, List<List<?>>> resultsForQry = receivedResults.computeIfAbsent(qryId, k -> new HashMap<>());

                resultsForQry.put(linkId, res);
            }
            else {
                rec.onResult(res);
            }
        }

    }

    public void onCacheRegistered(GridCacheContext cache) {
        if (cache.userCache())
            caches.add(cache);
    }

    public GridCacheContext firstUserCache() {
        return caches.get(0);
    }

    public IgniteTable table(String tblName) {
        return indexing.table(tblName);
    }

    public IgniteInternalCache cache(String name) {
        return ctx.cache().cache(name);
    }
}
