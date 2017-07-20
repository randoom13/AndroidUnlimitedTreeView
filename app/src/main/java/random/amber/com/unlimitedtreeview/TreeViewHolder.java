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

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.github.johnkil.print.PrintView;

import java.lang.ref.WeakReference;

import random.amber.com.unlimitedtreeview.database.model.FlatModel;
import random.amber.com.unlimitedtreeview.database.model.FlatModelParser;

public class TreeViewHolder extends RecyclerView.ViewHolder {
    public final TextView mTitle;
    public final View mMain;
    public final PrintView mIcon;
    private WeakReference<TreeCursorAdapter> mAdapterWR;

    public TreeViewHolder(View view, TreeCursorAdapter adapter) {
        super(view);
        mMain = view.findViewById(R.id.main);
        mIcon = (PrintView) view.findViewById(R.id.icon);
        mTitle = (TextView) view.findViewById(R.id.title);
        mAdapterWR = new WeakReference<TreeCursorAdapter>(adapter);
    }

    void bindModel(Cursor cursor) {
        FlatModel fm = FlatModelParser.ToFlatModel(cursor);
        int visibility = fm.getIsGroup() ? View.VISIBLE : View.INVISIBLE;
        mIcon.setVisibility(visibility);
        final boolean isExpanded = fm.getIsExpanded();
        int arrowState = isExpanded ? R.string.ic_keyboard_arrow_down : R.string.ic_keyboard_arrow_right;
        mIcon.getIcon()
                .setIconText(itemView.getResources().getString(arrowState));
        mTitle.setText(fm.getTitle());
        mMain.setPadding(40 * fm.getLevel(), 0, 0, 0);
        final String path = fm.getPath();
        mMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TreeCursorAdapter adapter = mAdapterWR.get();
                if (null != adapter)
                    adapter.expanded(getPosition(), path, !isExpanded);
            }
        });
    }
}
