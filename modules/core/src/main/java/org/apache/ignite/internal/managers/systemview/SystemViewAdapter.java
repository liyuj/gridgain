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

package org.apache.ignite.internal.managers.systemview;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import org.apache.ignite.spi.systemview.view.SystemViewRowAttributeWalker;
import org.jetbrains.annotations.NotNull;

/**
 * System view backed by {@code data} {@link Collection}.
 */
public class SystemViewAdapter<R, D> extends AbstractSystemView<R> {
    /** Data backed by this view. */
    private final Collection<D> data;

    /** Row function. */
    private final Function<D, R> rowFunc;

    /**
     * @param name Name.
     * @param desc Description.
     * @param rowCls Row class.
     * @param walker Walker.
     * @param data Data.
     * @param rowFunc Row function.
     */
    public SystemViewAdapter(String name, String desc, Class<R> rowCls,
        SystemViewRowAttributeWalker<R> walker, Collection<D> data, Function<D, R> rowFunc) {
        super(name, desc, rowCls, walker);

        this.data = data;
        this.rowFunc = rowFunc;
    }

    /** {@inheritDoc} */
    @NotNull @Override public Iterator<R> iterator() {
        Iterator<D> data = this.data.iterator();

        return new Iterator<R>() {
            @Override public boolean hasNext() {
                return data.hasNext();
            }

            @Override public R next() {
                return rowFunc.apply(data.next());
            }
        };
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return data.size();
    }
}
