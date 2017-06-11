/*
 * Copyright (C) 2017 KHLIVNIUK OLEKSANDR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package random.amber.com.unlimitedtreeview.database.tables;

public class FreeNodesTableInfo extends BaseTableInfo {
    public final String mNodeId = "node_id";

    public FreeNodesTableInfo() {
        this(1);
    }

    FreeNodesTableInfo(int version) {
        super(version);
        mTableName = "free_nodes";
        mVersion = version;
        mCreateTableScript = String.format("CREATE TABLE IF NOT EXISTS %s ("
                + "%s INTEGER PRIMARY KEY NOT NULL)", mTableName, mNodeId);
    }
}
