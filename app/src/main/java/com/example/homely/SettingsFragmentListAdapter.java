package com.example.homely;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SettingsFragmentListAdapter extends ArrayAdapter<SettingsFragmentListItem> {
    private final Context mContext;
    private final List<SettingsFragmentListItem> itemList;

    public SettingsFragmentListAdapter(@NonNull Context context, @NonNull List<SettingsFragmentListItem> objects) {
        super(context, 0, objects);
        mContext = context;
        itemList = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_settings_list_view_item, parent, false);
        }

        SettingsFragmentListItem currentItem = itemList.get(position);

        ImageView imageView = convertView.findViewById(R.id.listImage);
        TextView textView = convertView.findViewById(R.id.listName);

        imageView.setImageResource(currentItem.getIconResourceId());
        textView.setText(currentItem.getName());

        return convertView;
    }
}
