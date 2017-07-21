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

import random.amber.com.unlimitedtreeview.database.DataBaseHelper;


public class MainActivity extends Activity {
    public static final int MAX_PRIORITY = 255;
    private static final int sMaxLevelCount = 3;
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
        mDataBaseHelper = new DataBaseHelper(this.getApplicationContext());
        TreeCursorAdapter adapter = new TreeCursorAdapter(getLayoutInflater(), mDataBaseHelper);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInitializeRecyclerThread = new InitializeRecyclerThread(mDataBaseHelper);
        mInitializeRecyclerThread.setListener(new LoadListener() {
            @Override
            public void changeCursor(final Cursor cursor) {
                MainActivity.this.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                ((TreeCursorAdapter) mRecyclerView.getAdapter()).changeCursor(cursor);
                            }
                        });
            }
        });
        mInitializeRecyclerThread.start();
    }

    @Override
    protected void onPause() {
        TreeCursorAdapter adapter = (TreeCursorAdapter) mRecyclerView.getAdapter();
        adapter.closeResources();
        if (null != mInitializeRecyclerThread) {
            mInitializeRecyclerThread.clearListener();
        }
        super.onPause();
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
            int[] path = new int[sMaxLevelCount];
            for (int index = 0; index < sMaxLevelCount; index++)
                path[index] = sMaxLevelCount - 1;
            if (!mDataBaseHelper.modelExists(path, MAX_PRIORITY))
                DummyNodesFactory.createTree(mDataBaseHelper, sMaxLevelCount);
            final Cursor cursor = mDataBaseHelper.loadList(MAX_PRIORITY, true);
            cursor.getCount();
            if (null != mLoadListener)
                mLoadListener.changeCursor(cursor);
        }
    }
}
