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

package org.apache.ignite.spi.collision;

import java.util.EventListener;

/**
 * Listener to be set on {@link CollisionSpi} for notification of external
 * collision events (e.g. job stealing). Once grid receives such notification,
 * it will immediately invoke collision SPI.
 */
public interface CollisionExternalListener extends EventListener {
    /**
     * Callback invoked by Collision SPI whenever an external collision
     * event occurs.
     */
    public void onExternalCollision();
}