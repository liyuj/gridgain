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

import {Selector} from 'testcafe';
import {Table} from '../components/Table';

export const TEST_USER = {
    email: 'N3wUs3r@D0main.c0m',
    password: '1',
    passwordConfirm: '1',
    firstName: 'User',
    lastName: 'Name',
    country: 'Brazil',
    company: 'Acme Inc.',
    industry: 'Banking',
};

export const usersTable = new Table(Selector('ignite-list-of-registered-users'));
export const userNameCell = Selector('.ui-grid-cell-contents');