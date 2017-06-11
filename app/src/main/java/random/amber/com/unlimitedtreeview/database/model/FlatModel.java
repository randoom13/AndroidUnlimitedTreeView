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


public class FlatModel {
    public final static String PATH_DIVIDER = ",";
    final Boolean mIsExpanded;
    final Boolean mIsGroup;
    final String mTitle;
    private String mPath;
    private int mLevel;
    private int mPriority;

    FlatModel(String path, Boolean isGroup, String title, Boolean isExpanded) {
        setPath(path);
        mIsGroup = isGroup;
        mTitle = title;
        mIsExpanded = isExpanded;
    }

    public FlatModel(int[] path, Boolean isGroup, String title, Boolean isExpanded) {
        this(PathHelper.toString(path), isGroup, title, isExpanded);
    }

    public FlatModel(Boolean isGroup, String title, Boolean isExpanded) {
        this("", isGroup, title, isExpanded);
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
        this.mLevel = PathHelper.getLevelFrom(path);
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    public int getLevel() {
        return mLevel;
    }

    public Boolean getIsGroup() {
        return mIsGroup;
    }

    public String getTitle() {
        return mTitle;
    }

    public Boolean getIsExpanded() {
        return mIsExpanded;
    }
}
