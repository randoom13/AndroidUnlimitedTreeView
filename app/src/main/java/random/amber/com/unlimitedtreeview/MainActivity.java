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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import random.amber.com.unlimitedtreeview.database.DataBaseHelper;


public class MainActivity extends Activity {
    private static final int sMAX_LEVEL_COUNT = 3;
    private static final int sMAX_PRIORITY = 255;
    private RecyclerView mRecyclerView;
    private DataBaseHelper mDataBaseHelper;
    private InitializeRecyclerThread mInitializeRecyclerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStrictMode();
        mRecyclerView = new RecyclerView(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        setContentView(mRecyclerView);
        TreeCursorAdapter adapter = new TreeCursorAdapter(getLayoutInflater());
        mRecyclerView.setAdapter(adapter);
        adapter.setListener(new ExpandedListener() {
            @Override
            public void click(int position, String path, boolean isExpanded) {
                new ExpandedThread(MainActivity.this, position, path, isExpanded).start();
            }
        });
        mDataBaseHelper = new DataBaseHelper(this.getApplicationContext());
        mInitializeRecyclerThread = new InitializeRecyclerThread(mDataBaseHelper);
        mInitializeRecyclerThread.setListener(new LoadListener() {
            @Override
            public void changeCursor(final Cursor cursor) {
                ((TreeCursorAdapter) mRecyclerView.getAdapter()).changeCursor(cursor);
            }
        });
        mInitializeRecyclerThread.start();
    }

    @Override
    protected void onDestroy() {
        TreeCursorAdapter adapter = (TreeCursorAdapter) mRecyclerView.getAdapter();
        adapter.closeResources();
        if (null != mInitializeRecyclerThread) {
            mInitializeRecyclerThread.clearListener();
        }
        mDataBaseHelper.close();
        super.onDestroy();
    }

    private void setupStrictMode() {
        StrictMode.ThreadPolicy.Builder builder =
                new StrictMode.ThreadPolicy.Builder()
                        .detectAll().penaltyLog();
        if (BuildConfig.DEBUG) {
            builder.penaltyFlashScreen();
        }
        StrictMode.setThreadPolicy(builder.build());
    }

    interface LoadListener {
        void changeCursor(Cursor cursor);
    }

    interface ExpandedListener {
        void click(int position, String path, boolean isExpanded);
    }

    private static class InitializeRecyclerThread extends Thread {
        private LoadListener mLoadListener;
        private DataBaseHelper mDataBaseHelper;

        public InitializeRecyclerThread(DataBaseHelper dataBaseHelper) {
            super();
            mDataBaseHelper = dataBaseHelper;
        }

        public void setListener(LoadListener listener) {
            mLoadListener = listener;
        }

        public void clearListener() {
            mLoadListener = null;
        }

        @Override
        public void run() {
            int[] path = new int[sMAX_LEVEL_COUNT];
            for (int index = 0; index < sMAX_LEVEL_COUNT; index++)
                path[index] = sMAX_LEVEL_COUNT - 1;
            if (!mDataBaseHelper.modelExists(path, MAX_PRIORITY))
                DummyNodesFactory.createTree(mDataBaseHelper, sMAX_LEVEL_COUNT);
            final Cursor cursor = mDataBaseHelper.loadList(MAX_PRIORITY, true);
            cursor.getCount();
            if (null != mLoadListener)
                mLoadListener.changeCursor(cursor);
        }
    }

    private static class ExpandedThread extends Thread {
        private int mPosition;
        private String mPath;
        private boolean mIsExpanded;
        private WeakReference<MainActivity> mActivityWK;

        public ExpandedThread(MainActivity activity, int position, String path, boolean isExpanded) {
            mPosition = position;
            mIsExpanded = isExpanded;
            mPath = path;
            mActivityWK = new WeakReference<MainActivity>(activity);
        }


        @Override
        public void run() {
            MainActivity activity = mActivityWK.get();
            if (null == activity)
                return;
            DataBaseHelper mDataBaseHelper = activity.mDataBaseHelper;
            activity = null;
            mDataBaseHelper.changeExpandedState(mPath, mIsExpanded, true);
            Cursor cursor = mDataBaseHelper.loadList(sMAX_PRIORITY, false);
            activity = mActivityWK.get();
            if (null == activity)
                return;

            TreeCursorAdapter adapter = (TreeCursorAdapter) activity.mRecyclerView.getAdapter();
            adapter.changeCursorAfterExpand(cursor, mPosition);
        }
    }

    public static class TreeCursorAdapter extends RecyclerView.Adapter<TreeViewHolder> {
        private final LayoutInflater mInflater;
        private Cursor mCursor;
        private ExpandedListener mListener;

        public TreeCursorAdapter(LayoutInflater inflater) {
            super();
            mCursor = null;
            mInflater = inflater;
        }

        public void setListener(ExpandedListener listener) {
            mListener = listener;
        }

        public void clearListener() {
            mListener = null;
        }

        public void changeCursor(Cursor cursor) {
            if (cursor == mCursor)
                return;
            Cursor oldCursor = mCursor;
            mCursor = cursor;
            notifyDataSetChanged();
            if (oldCursor != null)
                oldCursor.close();
        }

        public void changeCursorAfterExpand(Cursor cursor, int position) {
            int deltaItems = getItemCount() - cursor.getCount();
            if (deltaItems == 0)
                return;
            Cursor oldCursor = mCursor;
            mCursor = cursor;
            notifyItemChanged(position);
            if (deltaItems < 0)
                notifyItemRangeInserted(position + 1, -deltaItems);
            else
                notifyItemRangeRemoved(position + 1, deltaItems);

            if (oldCursor != null)
                oldCursor.close();
        }

        @Override
        public TreeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.list_item, parent, false);
            return new TreeViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(TreeViewHolder holder, int position) {
            if (mCursor != null && mCursor.moveToPosition(position)) {
                holder.bindModel(mCursor);
            }
        }

        @Override
        public int getItemCount() {
            if (mCursor == null)
                return 0;
            return mCursor.getCount();
        }

        void expanded(int position, String path, boolean isExpanded) {
            if (null != mListener && !mCursor.isClosed())
                mListener.click(position, path, isExpanded);
        }

        void closeResources() {
            clearListener();
            if (mCursor != null)
                mCursor.close();
        }


    }
}
