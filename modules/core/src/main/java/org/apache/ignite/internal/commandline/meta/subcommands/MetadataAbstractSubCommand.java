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

package org.apache.ignite.internal.commandline.meta.subcommands;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.logging.Logger;
import org.apache.ignite.internal.client.GridClient;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.commandline.Command;
import org.apache.ignite.internal.commandline.CommandArgIterator;
import org.apache.ignite.internal.commandline.CommandLogger;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;

/** */
public abstract class MetadataAbstractSubCommand<
    MetadataArgsDto extends IgniteDataTransferObject,
    MetadataResultDto extends IgniteDataTransferObject
> implements Command<MetadataArgsDto> {
    /** Filesystem. */
    protected static final FileSystem FS = FileSystems.getDefault();

    /** */
    private MetadataArgsDto args;

    /** {@inheritDoc} */
    @Override public boolean experimental() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public final void printUsage(Logger log) {
        throw new UnsupportedOperationException("printUsage");
    }

    /** {@inheritDoc} */
    @Override public final void parseArguments(CommandArgIterator argIter) {
        args = parseArguments0(argIter);
    }

    /** {@inheritDoc} */
    @Override public final Object execute(GridClientConfiguration clientCfg, Logger log) throws Exception {
        try (GridClient client = Command.startClient(clientCfg)) {
            MetadataResultDto res = execute0(clientCfg, client);

            printResult(res, log);
        }
        catch (Throwable e) {
            log.severe("Failed to execute metadata command='" + name() + "'");
            log.severe(CommandLogger.errorMessage(e));

            throw e;
        }

        return null;
    }

    /** */
    protected MetadataResultDto execute0(
        GridClientConfiguration clientCfg,
        GridClient client
    ) throws Exception {
        return null;
    }

    /** {@inheritDoc} */
    @Override public final MetadataArgsDto arg() {
        return args;
    }

    /** */
    protected abstract String taskName();

    /** */
    protected MetadataArgsDto parseArguments0(CommandArgIterator argIter) {
        return null;
    }

    /** */
    protected abstract void printResult(MetadataResultDto res, Logger log);

    /**
     *
     */
    public static class VoidDto extends IgniteDataTransferObject {
        /** {@inheritDoc} */
        @Override protected void writeExternalData(ObjectOutput out) throws IOException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override protected void readExternalData(byte protoVer, ObjectInput in)
            throws IOException, ClassNotFoundException {
            // No-op.
        }
    }
}
