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
package random.amber.com.unlimitedtreeview.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import random.amber.com.unlimitedtreeview.database.model.FlatModel;
import random.amber.com.unlimitedtreeview.database.model.FlatModelParser;
import random.amber.com.unlimitedtreeview.database.model.PathHelper;
import random.amber.com.unlimitedtreeview.database.tables.BaseTableInfo;
import random.amber.com.unlimitedtreeview.database.tables.ExpandedGroupsTableInfo;
import random.amber.com.unlimitedtreeview.database.tables.FreeNodesTableInfo;
import random.amber.com.unlimitedtreeview.database.tables.ItemsRelationsTableInfo;
import random.amber.com.unlimitedtreeview.database.tables.NodesTableInfo;
import android.text.TextUtils;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "treeManager";
    private static final int SCHEMA = 1;
    private String mWorkTableScript;
    private String mFlatListScript;
    private String mUpdateExpandedVisibilityScript;
    private ItemsRelationsTableInfo mRelations = new ItemsRelationsTableInfo();
    private NodesTableInfo mNodes = new NodesTableInfo();
    private ExpandedGroupsTableInfo mExpandedGroups = new ExpandedGroupsTableInfo();
    private FreeNodesTableInfo mFreeNodes = new FreeNodesTableInfo();

    public DataBaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
        initializeScripts();
    }

    public DataBaseHelper(Context context) {
        this(context, DATABASE_NAME, SCHEMA);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        BaseTableInfo[] mTableInfos = new BaseTableInfo[4];
        mTableInfos[0] = mNodes;
        mTableInfos[1] = mRelations;
        mTableInfos[2] = mExpandedGroups;
        mTableInfos[3] = mFreeNodes;
        db.beginTransaction();
        try {

            for (BaseTableInfo tableInfo : mTableInfos) {
                db.execSQL(tableInfo.mCreateTableScript);

                if (tableInfo.getHasAdditionalScrips())
                    for (String script : tableInfo.mAdditionalScrips)
                        db.execSQL(script);
            }
            db.setTransactionSuccessful();
            Log.e(this.getClass().getSimpleName(), "All tables in database was created successfully");
        } catch (SQLException ex) {
            Log.e(this.getClass().getSimpleName(), "Can't create tables!", ex);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new RuntimeException("How did we get here?");
    }


    //region private methods
    private void updateRelationVisibility(SQLiteDatabase database) {
        Cursor cursor = database.rawQuery("SELECT MAX(" + mRelations.mLevel + ") FROM "
                + mRelations.getTableName(), null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        int maxLevel = cursor.getInt(0);
        cursor.close();
        for (int level = 0; level <= maxLevel; level++) {
            database.execSQL(mUpdateExpandedVisibilityScript + level);
        }
    }

    private void updateRelationVisibility(SQLiteDatabase database, String path, int minId) {
        int minLevel = PathHelper.getLevelFrom(path);

        Cursor cursor = database.rawQuery("SELECT MAX(" + mRelations.mLevel + ") FROM "
                + mRelations.getTableName(), null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        int maxLevel = cursor.getInt(0);
        cursor.close();
        String script = " AND " + mRelations.mItemId + " > " + minId;
        cursor = database.rawQuery("SELECT " + mRelations.mItemId +
                " FROM " + mRelations.getTableName() + " WHERE " + mRelations.mLevel + " = "
                + minLevel + script + " LIMIT 1", null);
        if (cursor.moveToFirst()) {
            script += " AND " + mRelations.mItemId + "<" + cursor.getString(0);
        }
        cursor.close();
        for (int level = minLevel + 1; level <= maxLevel; level++) {
            database.execSQL(mUpdateExpandedVisibilityScript + level + script);
        }
    }

    private int getFreeId(SQLiteDatabase readableDatabase) {
        Cursor cursor = readableDatabase.query(mFreeNodes.getTableName(),
                new String[]{mFreeNodes.mNodeId}, null, null, null, null, "1");
        int node_id = 0;
        if (cursor.moveToFirst()) {
            node_id = cursor.getInt(0);
            cursor.close();
            return node_id;
        }
        cursor.close();
        cursor = readableDatabase.rawQuery("SELECT MAX(" + mFreeNodes.mNodeId + ") FROM " +
                mNodes.getTableName(), null);
        if (cursor.moveToFirst())
            node_id = cursor.getInt(0) + 1;
        cursor.close();
        return node_id;
    }

    private boolean removeModel(SQLiteDatabase database, FlatModel flatModel) {
        if (TextUtils.isEmpty(flatModel.getPath()))
            return false;

        String selection = mRelations.mPath + " = " +
                DatabaseUtils.sqlEscapeString(flatModel.getPath());
        Cursor cursor = database.query(mRelations.getTableName(), new String[]{mRelations.mItemId},
                selection, null, null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        int items_id = cursor.getInt(0);

        database.delete(mNodes.getTableName(), mNodes.mNodeId + " = " + items_id, null);
        database.delete(mRelations.getTableName(),
                selection, null);
        database.delete(mExpandedGroups.getTableName(),
                mExpandedGroups.mGroupId + " = " + items_id, null);

        ContentValues values = new ContentValues();
        values.put(mFreeNodes.mNodeId, items_id);
        database.insert(mFreeNodes.getTableName(), null, values);
        return true;
    }

    private void insertOrReplaceModel(SQLiteDatabase writableDatabase, FlatModel flatModel,
                                      String path, int node_id) {
        ContentValues values = new ContentValues();
        values.put(mNodes.mNodeId, node_id);
        values.put(mNodes.mName, flatModel.getTitle());
        values.put(mNodes.mPriority, flatModel.getPriority());
        values.put(mNodes.mIsGroup, flatModel.getIsGroup().compareTo(false));
        writableDatabase.replace(mNodes.getTableName(), null, values);
        if (flatModel.getIsGroup() && flatModel.getIsExpanded()) {
            values = new ContentValues();
            values.put(mExpandedGroups.mGroupId, node_id);
            writableDatabase.replace(mExpandedGroups.getTableName(), null, values);
        } else writableDatabase.delete(mExpandedGroups.getTableName(), mExpandedGroups.mGroupId
                + " = " + node_id, null);

        values = new ContentValues();
        values.put(mRelations.mItemId, node_id);
        values.put(mRelations.mPath, path);
        values.put(mRelations.mLevel, PathHelper.getLevelFrom(path));
        writableDatabase.replace(mRelations.getTableName(), null, values);
        writableDatabase.delete(mFreeNodes.getTableName(), mFreeNodes.mNodeId + " = " + node_id,
                null);
    }

    private String getWorkTable(int priority, boolean isOrder) {
        String result = String.format(mWorkTableScript, priority);
        if (isOrder)
            result += " ORDER BY " + mRelations.mPath;
        return result;
    }

    private String getMaxFreeChildIndex(int priority, int[] parentPath) {
        boolean isTop = parentPath.length == 0;
        String sql = "( " + getWorkTable(priority, true) + " DESC) ";
        String parentLocation = PathHelper.toString(parentPath);
        String sqlWhere = "";
        if (isTop)
            sqlWhere = mRelations.mLevel + " = 0 LIMIT 1";
        else {
            int childLevel = PathHelper.getLevelFrom(parentLocation) + 1;
            String path = DatabaseUtils.sqlEscapeString(parentLocation +
                    FlatModel.PATH_DIVIDER + "%");
            sqlWhere = mRelations.mPath + " LIKE " + path + " AND " +
                    mRelations.mLevel + " = " + childLevel + " LIMIT 1";
        }

        sql = "SELECT " + mRelations.mPath + " FROM " + sql + " WHERE " + sqlWhere;
        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        int nextMaxFreeIndex = 0;
        if (cursor.moveToFirst()) {
            int[] lastChildPath = PathHelper.toArray(cursor.getString(0));
            nextMaxFreeIndex = lastChildPath[lastChildPath.length - 1] + 1;
        }
        cursor.close();
        String newPath = String.valueOf(nextMaxFreeIndex);
        if (!isTop)
            newPath = parentLocation + FlatModel.PATH_DIVIDER + newPath;

        return newPath;
    }

    private int getMaxIndexOnLevel(SQLiteDatabase database, int priority, int[] level) {
        boolean isTop = level.length == 1;
        String sql = "( " + getWorkTable(priority, true) + " DESC) ";
        String location = PathHelper.toString(level, level.length - 1);
        if (isTop)
            sql = "SELECT " + mRelations.mPath + " FROM " + sql + " WHERE " +
                    mRelations.mLevel + " = 0 LIMIT 1";
        else
            sql = String.format("SELECT " + mRelations.mPath + " FROM %s WHERE " +
                            mRelations.mPath + " LIKE '%s' LIMIT 1",
                    sql, location + FlatModel.PATH_DIVIDER + "%");

        Cursor cursor = database.rawQuery(sql, null);
        String maxItem = cursor.moveToFirst() ? cursor.getString(0) : "0";
        cursor.close();
        int[] newPath = PathHelper.toArray(maxItem);
        return newPath[newPath.length - 1];
    }

    private void freeRelationPath(SQLiteDatabase database, AllocateRelationPathParameters holeParams) {
        for (int index = holeParams.mMinIndex; index <= holeParams.mMaxIndex - 1; index++) {
            String oldPath = holeParams.mParentPath + String.valueOf(index + 1);
            String newPath = holeParams.mParentPath + index;
            replaceItemsContainsPaths(database, oldPath, newPath);
        }
    }

    private void replaceItemsContainsPaths(SQLiteDatabase database, String oldLocation, String newLocation) {
        int position = oldLocation.length() + 1;
        String pathExpression = "('" + newLocation + FlatModel.PATH_DIVIDER +
                "' || SUBSTR(" + mRelations.mPath + "," + position + "))";
        ContentValues values = new ContentValues();
        values.put(mRelations.mPath, pathExpression);
        database.update(mRelations.getTableName(), values,
                mRelations.mPath + "=" + DatabaseUtils.sqlEscapeString(
                        oldLocation + FlatModel.PATH_DIVIDER + "%"), null);
        values = new ContentValues();
        values.put(mRelations.mPath, newLocation);
        database.update(mRelations.getTableName(), values,
                mRelations.mPath + "=" + DatabaseUtils.sqlEscapeString(oldLocation), null);
    }

    private void reserveRelationPath(SQLiteDatabase database, AllocateRelationPathParameters holeParams) {
        for (int index = holeParams.mMaxIndex; index >= holeParams.mMinIndex; index--) {
            String oldPath = holeParams.mParentPath + index;
            String newPath = holeParams.mParentPath + String.valueOf(index + 1);
            replaceItemsContainsPaths(database, oldPath, newPath);
        }
    }

    //v1
    private void initializeScripts() {
        mWorkTableScript = "SELECT " + mRelations.mItemId + ", " + mRelations.mPath + ", " + mRelations.mVisible + ", " +
                mNodes.mIsGroup + ", " + mNodes.mName + "," + mRelations.mLevel + " FROM " +
                mRelations.getTableName() + " INNER JOIN " + mNodes.getTableName() + " ON " +
                "( " + mNodes.mNodeId + " = " + mRelations.mItemId + " AND %d >= " + mNodes.mPriority
                + " )";

        //CursorAdapter requires that the Cursor's result set must include a column named exactly "_id".
        mFlatListScript = "SELECT rowid _id, " + mRelations.mItemId + ", items." +
                mRelations.mPath + ", items." + mNodes.mIsGroup + ", items." + mNodes.mName + "," +
                "(SELECT 1 FROM " + mExpandedGroups.getTableName() +
                " WHERE " + mExpandedGroups.mGroupId + "=" + mRelations.mItemId + ")" +
                " FROM ( %s ) AS items " +
                "WHERE " + mRelations.mVisible + " = 1 GROUP BY items." + mRelations.mItemId +
                " ORDER BY items." + mRelations.mPath;

        String sql = "SELECT 1"
                + " FROM " + mRelations.getTableName() + " as parent " +
                " WHERE " + mRelations.getTableName() + "." + mRelations.mLevel + " = 0 OR" +
                " (parent." + mRelations.mVisible + " = 1 AND parent." +
                mRelations.mLevel + "+1=" + mRelations.getTableName() + "." + mRelations.mLevel +
                " AND " + mRelations.getTableName() + "." + mRelations.mPath +
                " LIKE (parent." + mRelations.mPath + " || '" + FlatModel.PATH_DIVIDER + "%')" +
                " AND (SELECT * FROM " + mExpandedGroups.getTableName() + " WHERE parent." + mRelations.mItemId +
                " = " + mExpandedGroups.mGroupId + " LIMIT 1) IS NOT NULL" + " ) LIMIT 1";
        mUpdateExpandedVisibilityScript = "UPDATE " + mRelations.getTableName() +
                " SET " + mRelations.mVisible +
                " = (" + sql + ") WHERE " + mRelations.getTableName() + "." + mRelations.mLevel + " =";

        ;
    }
    //endregion

    //region public methods

    public void changeExpandedState(String path, boolean isState, boolean isForceUpdate) {
        Cursor cursor = getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mItemId}, mRelations.mPath
                        + " = " + DatabaseUtils.sqlEscapeString(path), null, null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }
        int id = cursor.getInt(0);
        cursor.close();
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            if (isState) {
                ContentValues values = new ContentValues();
                values.put(mExpandedGroups.mGroupId, id);
                database.replace(mExpandedGroups.getTableName(), null, values);
            } else
                database.delete(mExpandedGroups.getTableName(),
                        mExpandedGroups.mGroupId + " = " + id, null);
            if (isForceUpdate)
                updateRelationVisibility(database, path, id);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public FlatModel getModel(String path, int priority) {
        String sql = "( " + getWorkTable(priority, false) + " ) ";
        sql = String.format(mFlatListScript, sql);
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM (" + sql + ") WHERE " + mRelations.mPath +
                " = " + path, null);
        if (!cursor.moveToFirst())
            return null;
        return FlatModelParser.ToFlatModel(cursor);
    }

    public boolean replaceModel(FlatModel flatModel, String path, int priority) {
        SQLiteDatabase database = getWritableDatabase();
        String sql = "SELECT " + mRelations.mItemId + " FROM " + mRelations.getTableName() +
                " WHERE " + mRelations.mPath + " = " + path;
        Cursor cursor = database.rawQuery(sql, null);
        if (!cursor.moveToFirst()) {
            return false;
        }
        int nodeId = cursor.getInt(0);
        database.beginTransaction();
        try {
            insertOrReplaceModel(database, flatModel, path, nodeId);
            database.setTransactionSuccessful();
            return true;
        } finally {
            database.endTransaction();
        }
    }

    public void removeModel(FlatModel flatModel) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            removeModel(database, flatModel);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public boolean insertModel(FlatModel flatModel, int priority, int[] path)
            throws SQLException {
        if (!modelExists(path, priority)) {
            return false;
        }
        int maxIndex = getMaxIndexOnLevel(getReadableDatabase(), priority, path);
        AllocateRelationPathParameters holeParams =
                new AllocateRelationPathParameters(maxIndex, path);
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            reserveRelationPath(database, holeParams);
            int[] modelPath = PathHelper.toArray(flatModel.getPath());
            AllocateRelationPathParameters modelHoleParams = new AllocateRelationPathParameters(
                    getMaxIndexOnLevel(database, priority, modelPath), modelPath);
            String location = PathHelper.toString(path);
            if (removeModel(database, flatModel))
                freeRelationPath(database, modelHoleParams);
            int nodeId = getFreeId(database);
            insertOrReplaceModel(database, flatModel, location, nodeId);
            flatModel.setPath(location);
            updateRelationVisibility(database);
            database.setTransactionSuccessful();
            return true;
        } finally {
            database.endTransaction();
        }
    }

    public String addModel(FlatModel flatModel, int priority, int[] parentPath) throws SQLException {
        boolean isTop = parentPath.length == 0;
        if (!isTop && !modelExists(parentPath, priority))
            return null;

        String newPath = getMaxFreeChildIndex(priority, parentPath);
        int[] modelPath = PathHelper.toArray(flatModel.getPath());
        int maxIndex = getMaxIndexOnLevel(getReadableDatabase(), priority, modelPath);
        AllocateRelationPathParameters holeParams = new AllocateRelationPathParameters(maxIndex, modelPath);
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            if (removeModel(database, flatModel))
                freeRelationPath(database, holeParams);
            int nodeId = getFreeId(database);
            insertOrReplaceModel(database, flatModel, newPath, nodeId);
            updateRelationVisibility(database);
            flatModel.setPath(newPath);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return newPath;
    }

    public boolean modelExists(int[] path, int priority) {
        String sql = "( " + getWorkTable(priority, true) + ") ";
        sql = String.format("SELECT " + mRelations.mPath + " FROM %s WHERE " +
                mRelations.mPath + " = '%s'  LIMIT 1", sql, PathHelper.toString(path));
        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    public Cursor loadList(int priority, boolean forceUpdate) throws SQLException {
        if (forceUpdate) {
            SQLiteDatabase database = getWritableDatabase();
            database.beginTransaction();
            try {
                updateRelationVisibility(database);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
        String sql = "( " + getWorkTable(priority, false) + " ) ";
        sql = String.format(mFlatListScript, sql);

        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        //To wrap Cursor for properly work
        return new CursorWrapper(cursor);
    }
    //endregion public methods

    final static class AllocateRelationPathParameters {
        final int mMaxIndex;
        final int mMinIndex;
        final int[] mPath;
        final String mParentPath;

        public AllocateRelationPathParameters(int maxIndex, int[] path) {
            mMaxIndex = maxIndex;
            mMinIndex = path.length == 0 ? 0 : path[path.length - 1];
            mPath = path;
            boolean hasParent = path.length > 1;
            mParentPath = hasParent ?
                    PathHelper.toString(path, path.length - 1) + FlatModel.PATH_DIVIDER : "";
        }
    }
}
