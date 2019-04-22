﻿/*
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

namespace Apache.Ignite.Core.Impl.DataStructures
{
    using System.Diagnostics;
    using Apache.Ignite.Core.DataStructures;

    /// <summary>
    /// Atomic reference.
    /// </summary>
    internal class AtomicReference<T> : PlatformTargetAdapter, IAtomicReference<T>
    {
        /** Opcodes. */
        private enum Op
        {
            Get = 1,
            Set = 2,
            CompareAndSetAndGet = 3,
            Close = 4,
            IsClosed = 5
        }

        /** */
        private readonly string _name;

        /** <inheritDoc /> */
        public AtomicReference(IPlatformTargetInternal target, string name)
            : base(target)
        {
            Debug.Assert(!string.IsNullOrEmpty(name));

            _name = name;
        }

        /** <inheritDoc /> */
        public string Name
        {
            get { return _name; }
        }

        /** <inheritDoc /> */
        public T Read()
        {
            return DoInOp<T>((int) Op.Get);
        }

        /** <inheritDoc /> */
        public void Write(T value)
        {
            DoOutOp((int) Op.Set, value);
        }

        /** <inheritDoc /> */
        public T CompareExchange(T value, T comparand)
        {
            return DoOutInOp((int) Op.CompareAndSetAndGet,
                writer =>
                {
                    writer.WriteObject(value);
                    writer.WriteObject(comparand);
                },
                stream => stream.ReadBool() ? comparand : Marshaller.StartUnmarshal(stream).Deserialize<T>());
        }

        /** <inheritDoc /> */
        public bool IsClosed
        {
            get { return DoOutInOp((int) Op.IsClosed) == True; }
        }

        /** <inheritDoc /> */
        public void Close()
        {
            DoOutInOp((int) Op.Close);
        }
    }
}
