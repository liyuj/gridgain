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

package org.apache.ignite.agent.processor.action;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.ignite.agent.action.controller.AbstractActionControllerTest;
import org.apache.ignite.agent.dto.action.JobResponse;
import org.apache.ignite.agent.dto.action.Request;
import org.apache.ignite.agent.dto.action.TaskResponse;
import org.apache.ignite.internal.util.typedef.F;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.ignite.agent.StompDestinationsUtils.buildActionJobResponseDest;
import static org.apache.ignite.agent.StompDestinationsUtils.buildActionTaskResponseDest;
import static org.apache.ignite.agent.dto.action.ResponseError.INTERNAL_ERROR_CODE;
import static org.apache.ignite.agent.dto.action.ResponseError.PARSE_ERROR_CODE;
import static org.apache.ignite.agent.dto.action.Status.COMPLETED;
import static org.apache.ignite.agent.dto.action.Status.FAILED;
import static org.apache.ignite.agent.dto.action.Status.RUNNING;

/**
 * Test for distributed action service.
 */
public class DistributedActionProcessorTest extends AbstractActionControllerTest {
    /**
     * Start grid instances.
     */
    @Before
    @Override public void startup() throws Exception {
        startup0(3);
    }

    /**
     * Should execute action on coordinator node by specific node ID in request.
     */
    @Test
    public void shouldExecuteActionOnCoordinatorNode() throws Exception {
        UUID crdId = cluster.localNode().id();
        String consistentId = String.valueOf(cluster.localNode().consistentId());

        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("ActionControllerForTests.nodeIdAction")
            .setNodeIds(singleton(crdId));

        executeAction(req, res -> {
            List<TaskResponse> taskResults =
                interceptor.getAllPayloads(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            Optional<TaskResponse> runningTask = taskResults.stream().filter(r -> r.getStatus() == RUNNING).findFirst();
            Optional<TaskResponse> completedTask = taskResults.stream().filter(r -> r.getStatus() == COMPLETED).findFirst();

            if (runningTask.isPresent() && completedTask.isPresent()) {
                UUID id = res.stream()
                    .map(r -> UUID.fromString(r.getResult().toString()))
                    .findFirst().get();

                return res.size() == completedTask.get().getJobCount() && id.equals(crdId);
            }

            return false;
        });

        JobResponse res = interceptor.getPayload(buildActionJobResponseDest(cluster.id(), req.getId()), JobResponse.class);

        assertEquals(consistentId, res.getNodeConsistentId());
        assertEquals(crdId, UUID.fromString((String) res.getResult()));
    }

    /**
     * Should execute action on nodes by specific node ID's in request.
     */
    @Test
    public void shouldExecuteActionOnNonCoordinatorNodes() throws Exception {
        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("ActionControllerForTests.nodeIdAction")
            .setNodeIds(nonCrdNodeIds);

        executeAction(req, res -> {
            List<TaskResponse> taskResults =
                interceptor.getAllPayloads(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            Optional<TaskResponse> runningTask = taskResults.stream().filter(r -> r.getStatus() == RUNNING).findFirst();
            Optional<TaskResponse> completedTask = taskResults.stream().filter(r -> r.getStatus() == COMPLETED).findFirst();

            if (runningTask.isPresent() && completedTask.isPresent()) {
                Set<UUID> results = res.stream()
                    .map(r -> UUID.fromString(r.getResult().toString()))
                    .collect(Collectors.toSet());

                return res.size() == completedTask.get().getJobCount() && results.equals(nonCrdNodeIds);
            }

            return false;
        });

        List<JobResponse> responses = interceptor.getAllPayloads(buildActionJobResponseDest(cluster.id(), req.getId()), JobResponse.class);
        boolean responsesHasCorrectConsistentIds = responses.stream().allMatch(r -> nonCrdNodeConsistentIds.contains(r.getNodeConsistentId()));

        assertTrue(responsesHasCorrectConsistentIds);
    }

    /**
     * Should execute action on all nodes.
     */
    @Test
    public void shouldExecuteActionOnAllNodes() throws Exception {
        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("ActionControllerForTests.nodeIdAction");

        executeAction(req, res -> {
            List<TaskResponse> taskResults =
                interceptor.getAllPayloads(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            Optional<TaskResponse> runningTask = taskResults.stream().filter(r -> r.getStatus() == RUNNING).findFirst();
            Optional<TaskResponse> completedTask = taskResults.stream().filter(r -> r.getStatus() == COMPLETED).findFirst();

            if (runningTask.isPresent() && completedTask.isPresent()) {
                Set<UUID> results = res.stream()
                    .map(r -> UUID.fromString(r.getResult().toString()))
                    .collect(Collectors.toSet());

                return res.size() == completedTask.get().getJobCount() && results.equals(allNodeIds);
            }

            return false;
        });

        List<JobResponse> responses = interceptor.getAllPayloads(buildActionJobResponseDest(cluster.id(), req.getId()), JobResponse.class);
        boolean responsesHasCorrectConsistentIds = responses.stream().allMatch(r -> allNodeConsistentIds.contains(r.getNodeConsistentId()));

        assertTrue(responsesHasCorrectConsistentIds);
    }

    /**
     * Should execute action on all nodes with one node stop after 1 second.
     */
    @Test
    public void shouldExecuteActionOnAllNodesWithNodeStop() throws Exception {
        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("ActionControllerForTests.nodeIdActionWithSleep")
            .setArgument(5000);

        executeActionAndStopNode(req, 1000, 1, res -> {
            List<TaskResponse> taskResults =
                interceptor.getAllPayloads(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            Optional<TaskResponse> runningTask = taskResults.stream().filter(r -> r.getStatus() == RUNNING).findFirst();
            Optional<TaskResponse> failedTask = taskResults.stream().filter(r -> r.getStatus() == FAILED).findFirst();

            if (runningTask.isPresent() && failedTask.isPresent()) {
                long failedJobCnt = res.stream()
                    .filter(r -> r.getStatus() == FAILED)
                    .count();

                return res.size() == failedTask.get().getJobCount() && failedJobCnt == 1;
            }

            return false;
        });
    }

    /**
     * Should send error response on response with invalid node id.
     */
    @Test
    public void shouldSendErrorResponseWithInvalidNodeId() {
        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("ActionControllerForTests.nodeIdAction")
            .setNodeIds(singleton(UUID.randomUUID()));

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);
            TaskResponse taskRes =
                interceptor.getPayload(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            return taskRes.getStatus() == FAILED && r.getStatus() == FAILED && r.getError().getCode() == INTERNAL_ERROR_CODE;
        });
    }

    /**
     * Should send error response on response with invalid argument.
     */
    @Test
    public void shouldSendErrorResponseWithInvalidArgument() {
        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("BaselineActions.updateAutoAdjustAwaitingTime")
            .setArgument("value");

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);
            TaskResponse taskRes =
                interceptor.getPayload(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            return taskRes.getStatus() == FAILED && r.getError().getCode() == PARSE_ERROR_CODE;
        });
    }

    /**
     * Should send error response on response with incorrect action.
     */
    @Test
    public void shouldSendErrorResponseWithIncorrectAction() {
        Request req = new Request()
            .setId(UUID.randomUUID())
            .setAction("InvalidAction.updateAutoAdjustEnabled")
            .setArgument(true);

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);
            TaskResponse taskRes =
                interceptor.getPayload(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            return taskRes.getStatus() == FAILED && r.getError().getCode() == PARSE_ERROR_CODE;
        });
    }
}