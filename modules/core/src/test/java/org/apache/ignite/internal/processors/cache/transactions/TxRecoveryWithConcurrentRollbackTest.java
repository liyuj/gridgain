/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.transactions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.processors.cache.distributed.GridCacheTxRecoveryRequest;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtTxFinishRequest;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxFinishRequest;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionState;
import org.junit.Test;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.testframework.GridTestUtils.runAsync;
import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;
import static org.apache.ignite.transactions.TransactionIsolation.READ_COMMITTED;

/**
 */
public class TxRecoveryWithConcurrentRollbackTest extends GridCommonAbstractTest {
    /** Backups. */
    private int backups = 1;

    /** Persistence. */
    private boolean persistence = false;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String name) throws Exception {
        final IgniteConfiguration cfg = super.getConfiguration(name);

        if (persistence) {
            cfg.setDataStorageConfiguration(
                new DataStorageConfiguration().
                    setWalSegmentSize(4 * 1024 * 1024).
                    setWalHistorySize(1000).
                    setCheckpointFrequency(Integer.MAX_VALUE).
                    setDefaultDataRegionConfiguration(
                        new DataRegionConfiguration().setPersistenceEnabled(true).setMaxSize(50 * 1024 * 1024)));
        }

        cfg.setActiveOnStart(false);
        cfg.setClientMode("client".equals(name));
        cfg.setCommunicationSpi(new TestRecordingCommunicationSpi());
        cfg.setCacheConfiguration(new CacheConfiguration(DEFAULT_CACHE_NAME).
            setCacheMode(PARTITIONED).
            setBackups(backups).
            setAtomicityMode(TRANSACTIONAL).
            setWriteSynchronizationMode(FULL_SYNC));

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();

        cleanPersistenceDir();
    }

    /**
     * The test enforces specific order in messages processing during concurrent tx rollback and tx recovery due to
     * node left.
     * <p>
     * Expected result: both DHT transactions produces same COMMITTED state on tx finish.
     * */
    @Test
    public void testRecoveryNotBreakingTxAtomicity() throws Exception {
        backups = 1;
        persistence = false;

        final IgniteEx node0 = startGrids(3);
        node0.cluster().active(true);

        final Ignite client = startGrid("client");

        final IgniteCache<Object, Object> cache = client.cache(DEFAULT_CACHE_NAME);

        final List<Integer> g0Keys = primaryKeys(grid(0).cache(DEFAULT_CACHE_NAME), 100);
        final List<Integer> g1Keys = primaryKeys(grid(1).cache(DEFAULT_CACHE_NAME), 100);

        final List<Integer> g2BackupKeys = backupKeys(grid(2).cache(DEFAULT_CACHE_NAME), 100, 0);

        Integer k1 = null;
        Integer k2 = null;

        for (Integer key : g2BackupKeys) {
            if (g0Keys.contains(key))
                k1 = key;
            else if (g1Keys.contains(key))
                k2 = key;

            if (k1 != null && k2 != null)
                break;
        }

        assertNotNull(k1);
        assertNotNull(k2);

        List<IgniteInternalTx> txs0 = null;
        List<IgniteInternalTx> txs1 = null;

        CountDownLatch stripeBlockLatch = new CountDownLatch(1);

        try(final Transaction tx = client.transactions().txStart(PESSIMISTIC, READ_COMMITTED)) {
            cache.put(k1, Boolean.TRUE);
            cache.put(k2, Boolean.TRUE);

            TransactionProxyImpl p  = (TransactionProxyImpl)tx;
            p.tx().prepare(true);

            txs0 = txs(grid(0));
            txs1 = txs(grid(1));
            List<IgniteInternalTx> txs2 = txs(grid(2));

            assertTrue(txs0.size() == 1);
            assertTrue(txs1.size() == 1);
            assertTrue(txs2.size() == 2);

            TestRecordingCommunicationSpi.spi(grid(1)).blockMessages(GridCacheTxRecoveryRequest.class, grid(0).name());
            TestRecordingCommunicationSpi.spi(client).blockMessages(GridNearTxFinishRequest.class, grid(0).name());

            int stripe = U.safeAbs(p.tx().xidVersion().hashCode());

            // Blocks stripe processing for rollback request on node1.
            grid(1).context().getStripedExecutorService().execute(stripe, () -> U.awaitQuiet(stripeBlockLatch));

            runAsync(() -> {
                TestRecordingCommunicationSpi.spi(client).waitForBlocked();

                client.close();

                return null;
            });

            tx.rollback();

            fail();
        }
        catch (Exception ignored) {
            // Expected.
        }

        assertNotNull(txs0);
        txs0.get(0).finishFuture().get();

        // Release rollback request processing.
        stripeBlockLatch.countDown();

        doSleep(1000); // Give some time for finish request to complete.

        TestRecordingCommunicationSpi.spi(grid(1)).stopBlock();

        assertNotNull(txs1);
        txs1.get(0).finishFuture().get();

        final TransactionState s1 = txs0.get(0).state();
        final TransactionState s2 = txs1.get(0).state();

        assertEquals(s1, s2);
    }

    /**
     * Stop near and primary node after primary tx rolled back with enabled persistence.
     * <p>
     * Expected result: after restarting a primary partitions are consistent.
     */
    @Test
    public void testRecoveryNotBreakingTxAtomicityOnNearAndPrimaryFail() throws Exception {
        backups = 2;
        persistence = true;

        final IgniteEx node0 = startGrids(3);
        node0.cluster().active(true);

        final Ignite client = startGrid("client");

        final IgniteCache<Object, Object> cache = client.cache(DEFAULT_CACHE_NAME);

        final Integer pk = primaryKey(grid(1).cache(DEFAULT_CACHE_NAME));

        IgniteInternalFuture<Void> fut = null;

        List<IgniteInternalTx> tx0 = null;
        List<IgniteInternalTx> tx2 = null;

        try(final Transaction tx = client.transactions().txStart(PESSIMISTIC, READ_COMMITTED)) {
            cache.put(pk, Boolean.TRUE);

            TransactionProxyImpl p = (TransactionProxyImpl)tx;
            p.tx().prepare(true);

            tx0 = txs(grid(0));
            tx2 = txs(grid(2));

            TestRecordingCommunicationSpi.spi(grid(1)).blockMessages(new IgniteBiPredicate<ClusterNode, Message>() {
                @Override public boolean apply(ClusterNode node, Message msg) {
                    return msg instanceof GridDhtTxFinishRequest;
                }
            });

            fut = runAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    TestRecordingCommunicationSpi.spi(grid(1)).waitForBlocked(2);

                    final List<IgniteInternalTx> txs = txs(grid(1));

                    client.close();
                    grid(1).close();

                    return null;
                }
            });

            tx.rollback();

            fail();
        }
        catch (Exception e) {
            // Expected.
            System.out.println();
        }

        fut.get();

        final IgniteInternalTx tx_0 = tx0.get(0);
        tx_0.finishFuture().get();

        final IgniteInternalTx tx_2 = tx2.get(0);
        tx_2.finishFuture().get();

        final TransactionState s1 = tx_0.state();
        final TransactionState s2 = tx_2.state();

        startGrid(1);

        assertPartitionsSame(idleVerify(grid(0), DEFAULT_CACHE_NAME));
    }

    /**
     * @param g Grid.
     */
    private List<IgniteInternalTx> txs(IgniteEx g) {
        return new ArrayList<>(g.context().cache().context().tm().activeTransactions());
    }
}
