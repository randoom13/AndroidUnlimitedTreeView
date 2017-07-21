package random.amber.com.unlimitedtreeview;


import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import random.amber.com.unlimitedtreeview.database.DataBaseHelper;

public class TreeCursorAdapter extends RecyclerView.Adapter<TreeViewHolder> {
    private final LayoutInflater mInflater;
    private final DataBaseHelper mDataBaseHelper;
    private Cursor mCursor;

    public TreeCursorAdapter(LayoutInflater inflater, DataBaseHelper dataBaseHelper) {
        super();
        mCursor = null;
        mInflater = inflater;
        mDataBaseHelper = dataBaseHelper;
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

    void changeCursorAfterExpand(Cursor cursor, final int position) {
        final int deltaItems = getItemCount() - cursor.getCount();
        if (deltaItems == 0)
            return;
        final Cursor oldCursor = mCursor;
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
        new ExpandedThread(mDataBaseHelper, this, position, path, isExpanded).start();
    }

    void closeResources() {
        if (mCursor != null)
            mCursor.close();
    }


}
