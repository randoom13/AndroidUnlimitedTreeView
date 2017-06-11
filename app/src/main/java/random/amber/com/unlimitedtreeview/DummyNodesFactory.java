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
package random.amber.com.unlimitedtreeview;

import random.amber.com.unlimitedtreeview.database.DataBaseHelper;
import random.amber.com.unlimitedtreeview.database.model.FlatModel;
import random.amber.com.unlimitedtreeview.database.model.PathHelper;
import android.text.TextUtils;

final class DummyNodesFactory {
    private static final int DEFAULT_PRIORITY = 4;

    private static void createNodes(DataBaseHelper dataBaseHelper, String parentPath, int maxLevelCount) {
        int[] path = PathHelper.toArray(parentPath);
        if (path.length < maxLevelCount)
            for (int index = 0; index < maxLevelCount; index++) {
                String title = TextUtils.isEmpty(parentPath) ?
                        "root " + index : "node " + parentPath + "," + index;
                FlatModel node = new FlatModel(path.length + 1 != maxLevelCount, title, true);
                String newPath = dataBaseHelper.addModel(node, DEFAULT_PRIORITY, path);
                createNodes(dataBaseHelper, newPath, maxLevelCount);
            }
    }

    static void createTree(DataBaseHelper dataBaseHelper, int maxLevelCount) {
        createNodes(dataBaseHelper, "", maxLevelCount);
    }
}
