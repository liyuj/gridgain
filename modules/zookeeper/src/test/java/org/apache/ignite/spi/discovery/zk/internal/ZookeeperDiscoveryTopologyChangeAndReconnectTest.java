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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.GridCacheAbstractFullApiSelfTest;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.spi.discovery.zk.ZookeeperDiscoverySpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.ZkTestClientCnxnSocketNIO;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.events.EventType.EVT_CLIENT_NODE_DISCONNECTED;
import static org.apache.ignite.events.EventType.EVT_CLIENT_NODE_RECONNECTED;

/**
 * Tests for Zookeeper SPI discovery.
 */
public class ZookeeperDiscoveryTopologyChangeAndReconnectTest extends ZookeeperDiscoverySpiTestBase {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testTopologyChangeMultithreaded() throws Exception {
        topologyChangeWithRestarts(false, false);
    }

    /**
     * @throws Exception If failed.
     */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-9138")
    @Test
    public void testTopologyChangeMultithreaded_RestartZk() throws Exception {
        try {
            topologyChangeWithRestarts(true, false);
        }
        finally {
            zkCluster.close();

            zkCluster = null;
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-9138")
    @Test
    public void testTopologyChangeMultithreaded_RestartZk_CloseClients() throws Exception {
        try {
            topologyChangeWithRestarts(true, true);
        }
        finally {
            zkCluster.close();

            zkCluster = null;
        }
    }

    /**
     * @param restartZk If {@code true} in background restarts on of ZK servers.
     * @param closeClientSock If {@code true} in background closes zk clients' sockets.
     * @throws Exception If failed.
     */
    private void topologyChangeWithRestarts(boolean restartZk, boolean closeClientSock) throws Exception {
        sesTimeout = 30_000;

        if (closeClientSock)
            testSockNio = true;

        long stopTime = System.currentTimeMillis() + GridTestUtils.SF.applyLB(30_000, 5_000);

        AtomicBoolean stop = new AtomicBoolean();

        IgniteInternalFuture<?> fut1;

        IgniteInternalFuture<?> fut2;

        try {
            fut1 = restartZk ? startRestartZkServers(stopTime, stop) : null;
            fut2 = closeClientSock ? startCloseZkClientSocket(stopTime, stop) : null;

            int INIT_NODES = 10;

            startGridsMultiThreaded(INIT_NODES);

            final int MAX_NODES = 20;

            final List<Integer> startedNodes = new ArrayList<>();

            for (int i = 0; i < INIT_NODES; i++)
                startedNodes.add(i);

            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            final AtomicInteger startIdx = new AtomicInteger(INIT_NODES);

            while (System.currentTimeMillis() < stopTime) {
                if (startedNodes.size() >= MAX_NODES) {
                    int stopNodes = rnd.nextInt(5) + 1;

                    log.info("Next, stop nodes: " + stopNodes);

                    final List<Integer> idxs = new ArrayList<>();

                    while (idxs.size() < stopNodes) {
                        int stopIdx = rnd.nextInt(startedNodes.size());

                        if (!idxs.contains(stopIdx))
                            idxs.add(startedNodes.get(stopIdx));
                    }

                    GridTestUtils.runMultiThreaded(new IgniteInClosure<Integer>() {
                        @Override public void apply(Integer threadIdx) {
                            int stopNodeIdx = idxs.get(threadIdx);

                            info("Stop node: " + stopNodeIdx);

                            stopGrid(stopNodeIdx);
                        }
                    }, stopNodes, "stop-node");

                    startedNodes.removeAll(idxs);
                }
                else {
                    int startNodes = rnd.nextInt(5) + 1;

                    log.info("Next, start nodes: " + startNodes);

                    GridTestUtils.runMultiThreaded(new Callable<Void>() {
                        @Override public Void call() throws Exception {
                            int idx = startIdx.incrementAndGet();

                            log.info("Start node: " + idx);

                            startGrid(idx);

                            synchronized (startedNodes) {
                                startedNodes.add(idx);
                            }

                            return null;
                        }
                    }, startNodes, "start-node");
                }

                U.sleep(rnd.nextInt(100) + 1);
            }
        }
        finally {
            stop.set(true);
        }

        if (fut1 != null)
            fut1.get();

        if (fut2 != null)
            fut2.get();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRandomTopologyChanges() throws Exception {
        randomTopologyChanges(false, false);
    }

    /**
     * @throws Exception If failed.
     */
    private void checkZkNodesCleanup() throws Exception {
        final ZookeeperClient zkClient = new ZookeeperClient(getTestResources().getLogger(),
            zkCluster.getConnectString(),
            30_000,
            null);

        final String basePath = ZookeeperDiscoverySpiTestHelper.IGNITE_ZK_ROOT + "/";

        final String aliveDir = basePath + ZkIgnitePaths.ALIVE_NODES_DIR + "/";

        try {
            List<String> znodes = listSubTree(zkClient.zk(), ZookeeperDiscoverySpiTestHelper.IGNITE_ZK_ROOT);

            boolean foundAlive = false;

            for (String znode : znodes) {
                if (znode.startsWith(aliveDir)) {
                    foundAlive = true;

                    break;
                }
            }

            assertTrue(foundAlive); // Sanity check to make sure we check correct directory.

            assertTrue("Failed to wait for unused znodes cleanup", GridTestUtils.waitForCondition(new GridAbsPredicate() {
                @Override public boolean apply() {
                    try {
                        List<String> znodes = listSubTree(zkClient.zk(), ZookeeperDiscoverySpiTestHelper.IGNITE_ZK_ROOT);

                        for (String znode : znodes) {
                            if (znode.startsWith(aliveDir) || znode.length() < basePath.length())
                                continue;

                            znode = znode.substring(basePath.length());

                            if (!znode.contains("/")) // Ignore roots.
                                continue;

                            // TODO ZK: https://issues.apache.org/jira/browse/IGNITE-8193
                            if (znode.startsWith("jd/"))
                                continue;

                            log.info("Found unexpected znode: " + znode);

                            return false;
                        }

                        return true;
                    }
                    catch (Exception e) {
                        error("Unexpected error: " + e, e);

                        fail("Unexpected error: " + e);
                    }

                    return false;
                }
            }, 10_000));
        }
        finally {
            zkClient.close();
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-9138")
    @Test
    public void testRandomTopologyChanges_RestartZk() throws Exception {
        randomTopologyChanges(true, false);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRandomTopologyChanges_CloseClients() throws Exception {
        randomTopologyChanges(false, true);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeployService1() throws Exception {
        startGridsMultiThreaded(3);

        grid(0).services(grid(0).cluster()).deployNodeSingleton("test", new GridCacheAbstractFullApiSelfTest.DummyServiceImpl());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeployService2() throws Exception {
        helper.clientMode(false);

        startGrid(0);

        helper.clientMode(true);

        startGrid(1);

        grid(0).services(grid(0).cluster()).deployNodeSingleton("test", new GridCacheAbstractFullApiSelfTest.DummyServiceImpl());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeployService3() throws Exception {
        IgniteInternalFuture fut = GridTestUtils.runAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                helper.clientModeThreadLocal(true);

                startGrid(0);

                return null;
            }
        }, "start-node");

        helper.clientModeThreadLocal(false);

        startGrid(1);

        fut.get();

        grid(0).services(grid(0).cluster()).deployNodeSingleton("test", new GridCacheAbstractFullApiSelfTest.DummyServiceImpl());
    }

    /**
     * Test with large user attribute on coordinator node.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLargeUserAttribute1() throws Exception {
        initLargeAttribute();

        startGrid(0);

        checkZkNodesCleanup();

        userAttrs = null;

        startGrid(1);

        helper.waitForEventsAcks(ignite(0));

        waitForTopology(2);
    }

    /**
     * Test with large user attribute on non-coordinator node.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLargeUserAttribute2() throws Exception {
        startGrid(0);

        initLargeAttribute();

        startGrid(1);

        helper.waitForEventsAcks(ignite(0));

        checkZkNodesCleanup();
    }

    /**
     * Test with large user attributes on random nodes.
     * Also tests that big messages (more than 1MB) properly separated and processed by zk.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLargeUserAttribute3() throws Exception {
        Set<Integer> idxs = ThreadLocalRandom.current()
            .ints(0, 10)
            .distinct()
            .limit(3)
            .boxed()
            .collect(Collectors.toSet());

        for (int i = 0; i < 10; i++) {
            info("Iteration: " + i);

            if (idxs.contains(i))
                initLargeAttribute();
            else
                userAttrs = null;

            helper.clientMode(i > 5);

            startGrid(i);
        }

        waitForTopology(10);
    }

    /**
     *
     */
    private void initLargeAttribute() {
        userAttrs = new HashMap<>();

        int[] attr = new int[1024 * 1024 + ThreadLocalRandom.current().nextInt(1024 * 512)];

        for (int i = 0; i < attr.length; i++)
            attr[i] = i;

        userAttrs.put("testAttr", attr);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLargeCustomEvent() throws Exception {
        Ignite srv0 = startGrid(0);

        // Send large message, single node in topology.
        IgniteCache<Object, Object> cache = srv0.createCache(largeCacheConfiguration("c1"));

        for (int i = 0; i < 100; i++)
            cache.put(i, i);

        assertEquals(1, cache.get(1));

        helper.waitForEventsAcks(ignite(0));

        startGridsMultiThreaded(1, 3);

        srv0.destroyCache("c1");

        // Send large message, multiple nodes in topology.
        cache = srv0.createCache(largeCacheConfiguration("c1"));

        for (int i = 0; i < 100; i++)
            cache.put(i, i);

        waitForTopology(4);

        ignite(3).createCache(largeCacheConfiguration("c2"));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClientReconnectSessionExpire1_1() throws Exception {
        clientReconnectSessionExpire(false);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClientReconnectSessionExpire1_2() throws Exception {
        clientReconnectSessionExpire(true);
    }

    /**
     * @param closeSock Test mode flag.
     * @throws Exception If failed.
     */
    private void clientReconnectSessionExpire(boolean closeSock) throws Exception {
        startGrid(0);

        sesTimeout = 2000;
        helper.clientMode(true);
        testSockNio = true;

        Ignite client = startGrid(1);

        client.cache(DEFAULT_CACHE_NAME).put(1, 1);

        reconnectClientNodes(log, Collections.singletonList(client), closeSock);

        assertEquals(1, client.cache(DEFAULT_CACHE_NAME).get(1));

        client.compute().broadcast(new ZookeeperDiscoverySpiTestHelper.DummyCallable(null));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testForceClientReconnect() throws Exception {
        final int SRVS = 3;

        startGrids(SRVS);

        helper.clientMode(true);

        startGrid(SRVS);

        reconnectClientNodes(Collections.singletonList(ignite(SRVS)), new Callable<Void>() {
            @Override public Void call() throws Exception {
                ZookeeperDiscoverySpi spi = helper.waitSpi(getTestIgniteInstanceName(SRVS), spis);

                spi.clientReconnect();

                return null;
            }
        });

        waitForTopology(SRVS + 1);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testForcibleClientFail() throws Exception {
        final int SRVS = 3;

        startGrids(SRVS);

        helper.clientMode(true);

        startGrid(SRVS);

        reconnectClientNodes(Collections.singletonList(ignite(SRVS)), new Callable<Void>() {
            @Override public Void call() throws Exception {
                ZookeeperDiscoverySpi spi = helper.waitSpi(getTestIgniteInstanceName(0), spis);

                spi.failNode(ignite(SRVS).cluster().localNode().id(), "Test forcible node fail");

                return null;
            }
        });

        waitForTopology(SRVS + 1);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDuplicatedNodeId() throws Exception {
        UUID nodeId0 = nodeId = UUID.randomUUID();

        startGrid(0);

        int failingNodeIdx = 100;

        for (int i = 0; i < 5; i++) {
            final int idx = failingNodeIdx++;

            nodeId = nodeId0;

            info("Start node with duplicated ID [iter=" + i + ", nodeId=" + nodeId + ']');

            Throwable err = GridTestUtils.assertThrows(log, new Callable<Void>() {
                @Override public Void call() throws Exception {
                    startGrid(idx);

                    return null;
                }
            }, IgniteCheckedException.class, null);

            assertTrue(err instanceof IgniteCheckedException);

            assertTrue(err.getMessage().contains("Failed to start processor:")
                || err.getMessage().contains("Failed to start manager:"));

            nodeId = null;

            info("Start node with unique ID [iter=" + i + ']');

            Ignite ignite = startGrid(idx);

            nodeId0 = ignite.cluster().localNode().id();

            waitForTopology(i + 2);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPing() throws Exception {
        sesTimeout = 5000;

        startGrids(3);

        final ZookeeperDiscoverySpi spi = helper.waitSpi(getTestIgniteInstanceName(1), spis);

        final UUID nodeId = ignite(2).cluster().localNode().id();

        IgniteInternalFuture<?> fut = GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                assertTrue(spi.pingNode(nodeId));
            }
        }, 32, "ping");

        fut.get();

        fut = GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                spi.pingNode(nodeId);
            }
        }, 32, "ping");

        U.sleep(100);

        stopGrid(2);

        fut.get();

        fut = GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                assertFalse(spi.pingNode(nodeId));
            }
        }, 32, "ping");

        fut.get();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testWithPersistence1() throws Exception {
        startWithPersistence(false);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testWithPersistence2() throws Exception {
        startWithPersistence(true);
    }

    /**
     * Reconnect client node.
     *
     * @param log  Logger.
     * @param clients Clients.
     * @param closeSock {@code True} to simulate reconnect by closing zk client's socket.
     * @throws Exception If failed.
     */
    private static void reconnectClientNodes(final IgniteLogger log,
        List<Ignite> clients,
        boolean closeSock)
        throws Exception {
        final CountDownLatch disconnectLatch = new CountDownLatch(clients.size());
        final CountDownLatch reconnectLatch = new CountDownLatch(clients.size());

        IgnitePredicate<Event> p = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event evt) {
                if (evt.type() == EVT_CLIENT_NODE_DISCONNECTED) {
                    log.info("Disconnected: " + evt);

                    disconnectLatch.countDown();
                }
                else if (evt.type() == EVT_CLIENT_NODE_RECONNECTED) {
                    log.info("Reconnected: " + evt);

                    reconnectLatch.countDown();
                }

                return true;
            }
        };

        List<String> zkNodes = new ArrayList<>();

        for (Ignite client : clients) {
            client.events().localListen(p, EVT_CLIENT_NODE_DISCONNECTED, EVT_CLIENT_NODE_RECONNECTED);

            zkNodes.add(ZookeeperDiscoverySpiTestHelper.aliveZkNodePath(client));
        }

        long timeout = 15_000;

        if (closeSock) {
            for (Ignite client : clients) {
                ZookeeperDiscoverySpi spi = (ZookeeperDiscoverySpi)client.configuration().getDiscoverySpi();

                ZkTestClientCnxnSocketNIO.forNode(client.name()).closeSocket(true);

                timeout = Math.max(timeout, (long)(spi.getSessionTimeout() * 1.5f));
            }
        }
        else {
            /*
             * Use hack to simulate session expire without waiting session timeout:
             * create and close ZooKeeper with the same session ID as ignite node's ZooKeeper.
             */
            List<ZooKeeper> dummyClients = new ArrayList<>();

            for (Ignite client : clients) {
                ZookeeperDiscoverySpi spi = (ZookeeperDiscoverySpi)client.configuration().getDiscoverySpi();

                ZooKeeper zk = ZookeeperDiscoverySpiTestHelper.zkClient(spi);

                for (String s : spi.getZkConnectionString().split(",")) {
                    try {
                        ZooKeeper dummyZk = new ZooKeeper(
                            s,
                            10_000,
                            null,
                            zk.getSessionId(),
                            zk.getSessionPasswd());

                        dummyZk.exists("/a", false);

                        dummyClients.add(dummyZk);

                        break;
                    }
                    catch (Exception e) {
                        log.warning("Can't connect to server " + s + " [err=" + e + ']');
                    }
                }
            }

            for (ZooKeeper zk : dummyClients)
                zk.close();
        }

        ZookeeperDiscoverySpiTestHelper.waitNoAliveZkNodes(log,
            ((ZookeeperDiscoverySpi)clients.get(0).configuration().getDiscoverySpi()).getZkConnectionString(),
            zkNodes,
            timeout);

        if (closeSock) {
            for (Ignite client : clients)
                ZkTestClientCnxnSocketNIO.forNode(client.name()).allowConnect();
        }

        ZookeeperDiscoverySpiTestHelper.waitReconnectEvent(log, disconnectLatch);

        ZookeeperDiscoverySpiTestHelper.waitReconnectEvent(log, reconnectLatch);

        for (Ignite client : clients)
            client.events().stopLocalListen(p);
    }

    /**
     * @param zk ZooKeeper client.
     * @param root Root path.
     * @return All children znodes for given path.
     * @throws Exception If failed/
     */
    private List<String> listSubTree(ZooKeeper zk, String root) throws Exception {
        for (int i = 0; i < 30; i++) {
            try {
                return ZKUtil.listSubTreeBFS(zk, root);
            }
            catch (KeeperException.NoNodeException e) {
                info("NoNodeException when get znodes, will retry: " + e);
            }
        }

        throw new Exception("Failed to get znodes: " + root);
    }

    /**
     * @param cacheName Cache name.
     * @return Configuration.
     */
    private CacheConfiguration<Object, Object> largeCacheConfiguration(String cacheName) {
        CacheConfiguration<Object, Object> ccfg = new CacheConfiguration<>(cacheName);

        ccfg.setAffinity(new TestAffinityFunction(1024 * 1024));
        ccfg.setWriteSynchronizationMode(FULL_SYNC);

        return ccfg;
    }

    /**
     * @param clients Clients.
     * @param c Closure to run.
     * @throws Exception If failed.
     */
    private void reconnectClientNodes(List<Ignite> clients, Callable<Void> c)
        throws Exception {
        final CountDownLatch disconnectLatch = new CountDownLatch(clients.size());
        final CountDownLatch reconnectLatch = new CountDownLatch(clients.size());

        IgnitePredicate<Event> p = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event evt) {
                if (evt.type() == EVT_CLIENT_NODE_DISCONNECTED) {
                    log.info("Disconnected: " + evt);

                    disconnectLatch.countDown();
                }
                else if (evt.type() == EVT_CLIENT_NODE_RECONNECTED) {
                    log.info("Reconnected: " + evt);

                    reconnectLatch.countDown();
                }

                return true;
            }
        };

        for (Ignite client : clients)
            client.events().localListen(p, EVT_CLIENT_NODE_DISCONNECTED, EVT_CLIENT_NODE_RECONNECTED);

        c.call();

        ZookeeperDiscoverySpiTestHelper.waitReconnectEvent(log, disconnectLatch);

        ZookeeperDiscoverySpiTestHelper.waitReconnectEvent(log, reconnectLatch);

        for (Ignite client : clients)
            client.events().stopLocalListen(p);
    }

    /**
     * @param restartZk If {@code true} in background restarts on of ZK servers.
     * @param closeClientSock If {@code true} in background closes zk clients' sockets.
     * @throws Exception If failed.
     */
    private void randomTopologyChanges(boolean restartZk, boolean closeClientSock) throws Exception {
        sesTimeout = 30_000;

        if (closeClientSock)
            testSockNio = true;

        List<Integer> startedNodes = new ArrayList<>();
        List<String> startedCaches = new ArrayList<>();

        int nextNodeIdx = 0;
        int nextCacheIdx = 0;

        long stopTime = System.currentTimeMillis() + GridTestUtils.SF.applyLB(30_000, 5_000);

        int MAX_NODES = 20;
        int MAX_CACHES = 10;

        AtomicBoolean stop = new AtomicBoolean();

        IgniteInternalFuture<?> fut1 = restartZk ? startRestartZkServers(stopTime, stop) : null;
        IgniteInternalFuture<?> fut2 = closeClientSock ? startCloseZkClientSocket(stopTime, stop) : null;

        try {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            while (System.currentTimeMillis() < stopTime) {
                if (!startedNodes.isEmpty() && rnd.nextInt(10) == 0) {
                    boolean startCache = startedCaches.size() < 2 ||
                        (startedCaches.size() < MAX_CACHES && rnd.nextInt(5) != 0);

                    int nodeIdx = startedNodes.get(rnd.nextInt(startedNodes.size()));

                    if (startCache) {
                        String cacheName = "cache-" + nextCacheIdx++;

                        log.info("Next, start new cache [cacheName=" + cacheName +
                            ", node=" + nodeIdx +
                            ", crd=" + (startedNodes.isEmpty() ? null : Collections.min(startedNodes)) +
                            ", curCaches=" + startedCaches.size() + ']');

                        ignite(nodeIdx).createCache(new CacheConfiguration<>(cacheName));

                        startedCaches.add(cacheName);
                    }
                    else {
                        if (startedCaches.size() > 1) {
                            String cacheName = startedCaches.get(rnd.nextInt(startedCaches.size()));

                            log.info("Next, stop cache [nodeIdx=" + nodeIdx +
                                ", node=" + nodeIdx +
                                ", crd=" + (startedNodes.isEmpty() ? null : Collections.min(startedNodes)) +
                                ", cacheName=" + startedCaches.size() + ']');

                            ignite(nodeIdx).destroyCache(cacheName);

                            assertTrue(startedCaches.remove(cacheName));
                        }
                    }
                }
                else {
                    boolean startNode = startedNodes.size() < 2 ||
                        (startedNodes.size() < MAX_NODES && rnd.nextInt(5) != 0);

                    if (startNode) {
                        int nodeIdx = nextNodeIdx++;

                        log.info("Next, start new node [nodeIdx=" + nodeIdx +
                            ", crd=" + (startedNodes.isEmpty() ? null : Collections.min(startedNodes)) +
                            ", curNodes=" + startedNodes.size() + ']');

                        startGrid(nodeIdx);

                        assertTrue(startedNodes.add(nodeIdx));
                    }
                    else {
                        if (startedNodes.size() > 1) {
                            int nodeIdx = startedNodes.get(rnd.nextInt(startedNodes.size()));

                            log.info("Next, stop [nodeIdx=" + nodeIdx +
                                ", crd=" + (startedNodes.isEmpty() ? null : Collections.min(startedNodes)) +
                                ", curNodes=" + startedNodes.size() + ']');

                            stopGrid(nodeIdx);

                            assertTrue(startedNodes.remove((Integer)nodeIdx));
                        }
                    }
                }

                U.sleep(rnd.nextInt(100) + 1);
            }
        }
        finally {
            stop.set(true);
        }

        if (fut1 != null)
            fut1.get();

        if (fut2 != null)
            fut2.get();
    }

    /**
     * @param stopTime Stop time.
     * @param stop Stop flag.
     * @return Future.
     */
    private IgniteInternalFuture<?> startRestartZkServers(final long stopTime, final AtomicBoolean stop) {
        return GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                while (!stop.get() && System.currentTimeMillis() < stopTime) {
                    U.sleep(rnd.nextLong(2500));

                    int idx = rnd.nextInt(ZK_SRVS);

                    log.info("Restart ZK server: " + idx);

                    zkCluster.getServers().get(idx).restart();

                    waitForZkClusterReady(zkCluster);
                }

                return null;
            }
        }, "zk-restart-thread");
    }

    /**
     * @param stopTime Stop time.
     * @param stop Stop flag.
     * @return Future.
     */
    private IgniteInternalFuture<?> startCloseZkClientSocket(final long stopTime, final AtomicBoolean stop) {
        assert testSockNio;

        return GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                while (!stop.get() && System.currentTimeMillis() < stopTime) {
                    U.sleep(rnd.nextLong(100) + 50);

                    List<Ignite> nodes = G.allGrids();

                    if (!nodes.isEmpty()) {
                        Ignite node = nodes.get(rnd.nextInt(nodes.size()));

                        ZkTestClientCnxnSocketNIO nio = ZkTestClientCnxnSocketNIO.forNode(node);

                        if (nio != null) {
                            info("Close zk client socket for node: " + node.name());

                            try {
                                nio.closeSocket(false);
                            }
                            catch (Exception e) {
                                info("Failed to close zk client socket for node: " + node.name());
                            }
                        }
                    }
                }

                return null;
            }
        }, "zk-restart-thread");
    }

    /**
     * @param dfltConsistenId Default consistent ID flag.
     * @throws Exception If failed.
     */
    private void startWithPersistence(boolean dfltConsistenId) throws Exception {
        this.dfltConsistenId = dfltConsistenId;

        persistence = true;

        for (int i = 0; i < 3; i++) {
            info("Iteration: " + i);

            helper.clientMode(false);

            startGridsMultiThreaded(4, i == 0);

            helper.clientMode(true);

            startGridsMultiThreaded(4, 3);

            waitForTopology(7);

            stopGrid(1);

            waitForTopology(6);

            stopGrid(4);

            waitForTopology(5);

            stopGrid(0);

            waitForTopology(4);

            checkEventsConsistency();

            stopAllGrids();

            evts.clear();
        }
    }

    /** */
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static class TestAffinityFunction extends RendezvousAffinityFunction {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private int[] dummyData;

        /**
         * @param dataSize Dummy data size.
         */
        TestAffinityFunction(int dataSize) {
            dummyData = new int[dataSize];

            for (int i = 0; i < dataSize; i++)
                dummyData[i] = i;
        }
    }
}
