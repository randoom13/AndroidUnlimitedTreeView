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


import random.amber.com.unlimitedtreeview.database.DataBaseHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;


public class MainActivity extends Activity {
    private static final int MAX_LEVEL_COUNT = 3;
    private static final int MAX_PRIORITY = 255;
    private RecyclerView mRecyclerView;
    private DataBaseHelper mDataBaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStrictMode();
        mRecyclerView = new RecyclerView(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        setContentView(mRecyclerView);
        mRecyclerView.setAdapter(new TreeCursorAdapter());
        mDataBaseHelper = new DataBaseHelper(this.getApplicationContext());
        new InitializeRecyclerThread().start();
    }

    @Override
    protected void onDestroy() {
        ((TreeCursorAdapter) mRecyclerView.getAdapter()).closeCursor();
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

    private void changeCursor(Cursor cursor) {
        ((TreeCursorAdapter) mRecyclerView.getAdapter()).changeCursor(cursor);
    }

    public class TreeCursorAdapter extends RecyclerView.Adapter<TreeViewHolder> {
        private Cursor mCursor;

        public TreeCursorAdapter() {
            super();
            mCursor = null;
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

        @Override
        public TreeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = MainActivity.this.
                    getLayoutInflater().inflate(R.layout.list_item, parent, false);
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
            new ExpandedThread(position, path, isExpanded).start();
        }

        void closeCursor() {
            if (mCursor != null)
                mCursor.close();
        }

        private class ExpandedThread extends Thread {
            private int mPosition;
            private String mPath;
            private boolean mIsExpanded;

            public ExpandedThread(int position, String path, boolean isExpanded) {
                mPosition = position;
                mIsExpanded = isExpanded;
                mPath = path;
            }

            @Override
            public void run() {
                mDataBaseHelper.changeExpandedState(mPath, mIsExpanded, true);
                Cursor cursor = mDataBaseHelper.loadList(255, false);
                int deltaItems = getItemCount() - cursor.getCount();
                if (deltaItems == 0)
                    return;
                Cursor oldCursor = mCursor;
                mCursor = cursor;
                notifyItemChanged(mPosition);
                if (deltaItems < 0)
                    notifyItemRangeInserted(mPosition + 1, -deltaItems);
                else
                    notifyItemRangeRemoved(mPosition + 1, deltaItems);

                if (oldCursor != null)
                    oldCursor.close();
            }
        }
    }

    private class InitializeRecyclerThread extends Thread {
        @Override
        public void run() {
            int[] path = new int[MAX_LEVEL_COUNT];
            for (int index = 0; index < MAX_LEVEL_COUNT; index++)
                path[index] = MAX_LEVEL_COUNT - 1;
            if (!mDataBaseHelper.modelExists(path, MAX_PRIORITY))
                DummyNodesFactory.createTree(mDataBaseHelper, MAX_LEVEL_COUNT);
            final Cursor cursor = mDataBaseHelper.loadList(MAX_PRIORITY, true);
            cursor.getCount();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeCursor(cursor);
                }
            });
        }
    }
}
