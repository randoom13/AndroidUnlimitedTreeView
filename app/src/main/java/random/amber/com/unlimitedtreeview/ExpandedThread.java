package random.amber.com.unlimitedtreeview;

import android.database.Cursor;

import random.amber.com.unlimitedtreeview.database.DataBaseHelper;

public class ExpandedThread extends Thread {
    private final DataBaseHelper mDataBaseHelper;
    private final TreeCursorAdapter mAdapter;
    private int mPosition;
    private String mPath;
    private boolean mIsExpanded;

    public ExpandedThread(DataBaseHelper dataBaseHelper, TreeCursorAdapter adapter, int position, String path, boolean isExpanded) {
        mPosition = position;
        mIsExpanded = isExpanded;
        mPath = path;
        mDataBaseHelper = dataBaseHelper;
        mAdapter = adapter;
    }


    @Override
    public void run() {
        mDataBaseHelper.changeExpandedState(mPath, mIsExpanded, true);
        Cursor cursor = mDataBaseHelper.loadList(MainActivity.MAX_PRIORITY, false);
        mAdapter.changeCursorAfterExpand(cursor, mPosition);
    }
}
