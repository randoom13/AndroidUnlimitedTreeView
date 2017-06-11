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

public class ItemsRelationsTableInfo extends BaseTableInfo {
    public final String mPath = "path";
    public final String mItemId = "item_id";
    public final String mLevel = "level";
    public final String mVisible = "visible";

    public ItemsRelationsTableInfo() {
        this(1);
    }

    ItemsRelationsTableInfo(int version) {
        super(version);
        mTableName = "items_relation";
        mVersion = version;
        mCreateTableScript = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                        "%s INTEGER NOT NULL, %s TEXT PRIMARY KEY NOT NULL UNIQUE, %s INTEGER, %s INTEGER)", mTableName,
                mItemId, mPath, mLevel, mVisible);
        //column path should be indexed, in order to perform fast with the LIKE clause
        mAdditionalScrips.add("CREATE INDEX IF NOT EXISTS " + mPath + "_index ON " + mTableName + "(" + mPath + ")");
    }
}
