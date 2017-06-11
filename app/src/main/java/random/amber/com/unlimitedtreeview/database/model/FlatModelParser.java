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
package random.amber.com.unlimitedtreeview.database.model;

import android.database.Cursor;

public class FlatModelParser {
    public static FlatModel ToFlatModel(Cursor c) {
        String path = c.getString(2);
        Boolean isGroup = c.getInt(3) != 0;
        String title = c.getString(4);
        Boolean isExpanded = c.getInt(5) == 1;
        FlatModel fm = new FlatModel(path, isGroup, title, isExpanded);
        return fm;
    }
}
