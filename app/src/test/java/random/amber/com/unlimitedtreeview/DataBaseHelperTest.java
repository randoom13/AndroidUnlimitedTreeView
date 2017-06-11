package random.amber.com.unlimitedtreeview;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import random.amber.com.unlimitedtreeview.database.DataBaseHelper;
import random.amber.com.unlimitedtreeview.database.model.FlatModel;
import random.amber.com.unlimitedtreeview.database.model.FlatModelParser;
import random.amber.com.unlimitedtreeview.database.model.PathHelper;
import random.amber.com.unlimitedtreeview.database.tables.ExpandedGroupsTableInfo;
import random.amber.com.unlimitedtreeview.database.tables.ItemsRelationsTableInfo;
import random.amber.com.unlimitedtreeview.database.tables.NodesTableInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,  sdk=Build.VERSION_CODES.LOLLIPOP)
public class DataBaseHelperTest{
    private static String sTEST_DATABASE_NAME = "testTree";
    private final ItemsRelationsTableInfo mRelations;
    private final NodesTableInfo mNodes;
    private final ExpandedGroupsTableInfo mExpanded;
    private DataBaseHelper mDatebasehelper;

   public DataBaseHelperTest() {

        mRelations = new ItemsRelationsTableInfo();
        mNodes = new NodesTableInfo();
        mExpanded = new ExpandedGroupsTableInfo();
    }

    @Before
    public void before() throws Exception {
        Application application = RuntimeEnvironment.application;
        mDatebasehelper = new DataBaseHelper(application, sTEST_DATABASE_NAME, 1);
    }

    @After
    public void after() throws Exception {
        mDatebasehelper.close();
    }

    @Test
    public void modelNonExistsTest() throws Exception {
        createModelInDB("new item", 14, 6, "1,6", false, false);
        assertFalse(mDatebasehelper.modelExists(new int[]{1}, 255));
    }

    private void createModelInDB(String title, int nodeId, int priority, String path, boolean isGroup, boolean isExpanded) {
        SQLiteDatabase database = mDatebasehelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(mNodes.mName, title);
        values.put(mNodes.mNodeId, nodeId);
        values.put(mNodes.mPriority, priority);
        values.put(mNodes.mIsGroup, isGroup ? 1 : 0);
        database.beginTransaction();
        database.insert(mNodes.getTableName(), null, values);

        values = new ContentValues();
        values.put(mRelations.mItemId, nodeId);
        values.put(mRelations.mPath, path);
        values.put(mRelations.mLevel, PathHelper.getLevelFrom(path));
        database.insert(mRelations.getTableName(), null, values);

        if (isExpanded && isGroup) {
            values = new ContentValues();
            values.put(mExpanded.mGroupId, nodeId);
            database.insert(mExpanded.getTableName(), null, values);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    @Test
    public void modelExistsTest() throws Exception {
        createModelInDB("first item", 12, 6, "1,2,3", true, false);
        assertTrue(mDatebasehelper.modelExists(new int[]{1, 2, 3}, 255));
    }

    @Test
    public void removeModelTest() throws Exception {
        createModelInDB("second_item", 12, 6, "1,2,3", true, false);
        FlatModel model = new FlatModel(new int[]{1, 2, 3}, true, "first item", false);
        mDatebasehelper.removeModel(model);
        assertFalse(mDatebasehelper.modelExists(new int[]{1, 2, 3}, 255));
    }

    @Test
    public void insertModelTest() throws Exception {
        createModelInDB("newer_item", 11, 6, "1,2,6,4", true, false);
        createModelInDB("newer2_item", 13, 6, "1,2,6,5", true, false);
        FlatModel model = new FlatModel(new int[]{1, 2, 6}, true, "first item", false);
        assertTrue(mDatebasehelper.insertModel(model, 6, new int[]{1, 2, 6, 4}));
        Cursor cursor = mDatebasehelper.getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mItemId, mRelations.mPath}, null, null, null,
                null, null);
        cursor.move(1);
        int id1 = cursor.getInt(0);
        String path1 = cursor.getString(1);
        cursor.moveToNext();
        int id2 = cursor.getInt(0);
        String path2 = cursor.getString(1);
        cursor.close();
        assertEquals(id1, 11);
        assertEquals(path1, "1,2,6,5");
        assertEquals(id2, 13);
        assertEquals(path2, "1,2,6,6");
    }

    @Test
    public void insertModelOnTopTest() throws Exception {
        createModelInDB("newer_item", 11, 6, "4", true, false);
        createModelInDB("newer2_item", 13, 6, "5", true, false);
        FlatModel model = new FlatModel(new int[]{3}, true, "first item", false);
        assertTrue(mDatebasehelper.insertModel(model, 6, new int[]{4}));
        Cursor cursor = mDatebasehelper.getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mItemId, mRelations.mPath}, null, null, null,
                null, null);
        cursor.move(1);
        int id1 = cursor.getInt(0);
        String path1 = cursor.getString(1);
        cursor.moveToNext();
        int id2 = cursor.getInt(0);
        String path2 = cursor.getString(1);
        cursor.close();
        assertEquals(id1, 11);
        assertEquals(path1, "5");
        assertEquals(id2, 13);
        assertEquals(path2, "6");
    }

    @Test
    public void addModelinEmptyGroupTest() throws Exception {
        createModelInDB("newer_item", 11, 6, "4", true, false);
        createModelInDB("newer2_item", 13, 6, "5", true, false);
        FlatModel model = new FlatModel(new int[]{3}, true, "first item", false);
        assertTrue(mDatebasehelper.addModel(model, 6, new int[]{4}) != null);
        Cursor cursor = mDatebasehelper.getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mItemId, mRelations.mPath}, mRelations.mItemId + "= 14", null, null,
                null, null);
        cursor.moveToFirst();
        String path1 = cursor.getString(1);
        cursor.moveToNext();
        cursor.close();
        assertEquals(path1, "4,0");
    }

    @Test
    public void insertModelinNonEmptyGroupTest() throws Exception {
        createModelInDB("newer_item", 11, 6, "4", true, false);
        createModelInDB("newer2_item", 13, 6, "4,0", true, false);
        FlatModel model = new FlatModel(new int[]{3}, true, "first item", false);
        assertTrue(mDatebasehelper.addModel(model, 6, new int[]{4}) != null);
        Cursor cursor = mDatebasehelper.getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mPath}, mRelations.mItemId + "= 14", null, null,
                null, null);
        cursor.moveToFirst();
        String path1 = cursor.getString(0);
        cursor.moveToNext();
        cursor.close();
        assertEquals("4,1", path1);
    }

    @Test
    public void addModelnTopTest() throws Exception {
        createModelInDB("newer_item", 11, 6, "4", true, false);
        createModelInDB("newer2_item", 13, 6, "4,0", true, false);
        FlatModel model = new FlatModel(new int[]{3}, true, "first item", false);
        assertTrue(mDatebasehelper.addModel(model, 6, new int[]{}) != null);
        Cursor cursor = mDatebasehelper.getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mItemId, mRelations.mPath}, mRelations.mItemId + "= 14", null, null,
                null, null);
        cursor.moveToFirst();
        String path1 = cursor.getString(1);
        cursor.close();
        assertEquals("5", path1);
    }

    @Test
    public void addModelnTopTestv2() throws Exception {
        createModelInDB("newer_item", 11, 6, "4", true, false);
        createModelInDB("newer2_item", 13, 6, "4,0", true, false);
        createModelInDB("first_item", 17, 6, "3", true, false);
        FlatModel model = new FlatModel(new int[]{4}, true, "first item", false);
        assertTrue(mDatebasehelper.addModel(model, 6, new int[]{}) != null);
        Cursor cursor = mDatebasehelper.getReadableDatabase().query(mRelations.getTableName(),
                new String[]{mRelations.mItemId}, mRelations.mPath + " = 5", null, null,
                null, null);
        cursor.moveToFirst();
        assertEquals(11, cursor.getInt(0));
        cursor.close();
    }

    @Test
    public void getNonEmptyFlatList() {
        createModelInDB("newer_item", 11, 6, "4", true, false);
        createModelInDB("newer2_item", 13, 6, "4,0", true, false);
        Cursor cursor = mDatebasehelper.loadList(255, true);
        int count = cursor.getCount();
        cursor.close();
        assertEquals(1, count);
    }

    @Test
    public void getLongFlatListTest() {
        createModelInDB("newer_item", 15, 6, "5", true, true);
        createModelInDB("newer_item", 14, 6, "5,0", true, false);
        createModelInDB("neDDwer_item", 141, 6, "5,0,1", true, false);
        createModelInDB("newer2_item", 13, 6, "4", true, true);
        createModelInDB("newer2_item", 18, 6, "4,0", true, false);
        Cursor cursor = mDatebasehelper.loadList(255, true);
        int count = cursor.getCount();
        cursor.moveToFirst();
        String[] paths = new String[count];
        for (int index = 0; index < count; index++) {
            FlatModel fm = FlatModelParser.ToFlatModel(cursor);
            paths[index] = fm.getPath();
            cursor.moveToNext();
        }
        cursor.close();
        assertEquals(4, count);
        assertEquals("4", paths[0]);
        assertEquals("4,0", paths[1]);
        assertEquals("5", paths[2]);
        assertEquals("5,0", paths[3]);
    }

    @Test
    public void getFlatListTest() {
        createModelInDB("newerd_item", 15, 6, "5", true, true);
        createModelInDB("newers_item", 14, 6, "5,0", true, true);
        createModelInDB("nxewer_item", 141, 6, "5,1,0", true, false);
        createModelInDB("newxer_item", 142, 6, "5,1,1", true, false);
        createModelInDB("nxewer2_item", 13, 6, "4", true, true);
        createModelInDB("newer2x_item", 148, 6, "4,0", true, true);
        createModelInDB("nexwer2_item", 18, 6, "4,0,1", true, false);
        Cursor cursor = mDatebasehelper.loadList(255, true);
        int count = cursor.getCount();
        cursor.moveToFirst();
        String[] paths = new String[count];
        for (int index = 0; index < count; index++) {
            FlatModel fm = FlatModelParser.ToFlatModel(cursor);
            paths[index] = fm.getPath();
            cursor.moveToNext();
        }
        cursor.close();
        assertEquals(5, count);
        assertEquals("4", paths[0]);
        assertEquals("4,0", paths[1]);
        assertEquals("4,0,1", paths[2]);
        assertEquals("5", paths[3]);
        assertEquals("5,0", paths[4]);
        cursor.close();
    }
}
