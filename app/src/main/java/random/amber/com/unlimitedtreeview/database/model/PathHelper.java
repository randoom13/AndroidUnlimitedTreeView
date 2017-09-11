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

import android.text.TextUtils;

public class PathHelper {
    public static int getLevelFrom(String path) {
        return path.split(FlatModel.PATH_DIVIDER).length - 1;
    }

    public static String toString(int[] path, int maxLength) {
        StringBuilder builder = new StringBuilder();
        boolean hasItems = false;
        for (int index = 0; index < Math.min(path.length, maxLength); index++) {
            int location = path[index];
            if (hasItems)
                builder.append(FlatModel.PATH_DIVIDER);
            else hasItems = true;

            builder.append(location);
        }
        return builder.toString();
    }

    public static int[] toArray(String path) {
        if (TextUtils.isEmpty(path))
            return new int[0];
        String[] items = path.split(FlatModel.PATH_DIVIDER);
        int[] result = new int[items.length];
        for (int index = 0; index < items.length; index++) {
            result[index] = Integer.parseInt(items[index]);
        }
        return result;
    }

    public static String toString(int[] path) {
        StringBuilder builder = new StringBuilder();
        boolean hasItems = false;
        for (int location : path) {
            if (hasItems)
                builder.append(FlatModel.PATH_DIVIDER);
            else hasItems = true;

            builder.append(location);
        }
        return builder.toString();
    }
}
