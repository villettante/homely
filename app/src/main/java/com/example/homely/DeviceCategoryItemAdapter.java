package com.example.homely;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceCategoryItemAdapter extends RecyclerView.Adapter<DeviceCategoryItemAdapter.ViewHolder>{

    private final List<DeviceCategoryItem> itemList;
    private OnItemClickListener listener;
    public DeviceCategoryItemAdapter(List<DeviceCategoryItem> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_home_page_devices_categories_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceCategoryItem item = itemList.get(position);
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());
        holder.icon.setImageResource(item.getIconResId());

        GradientDrawable background = new GradientDrawable();
        background.setColor(item.getBackgroundColor());
        background.setCornerRadius(60);

        holder.itemView.setBackground(background);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }



    public interface OnItemClickListener {
        void onItemClick(DeviceCategoryItem item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView subtitle;
        public ImageView icon;
        public View itemView;

        public ViewHolder(View view) {
            super(view);
            itemView = view.findViewById(R.id.item_container);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            icon = view.findViewById(R.id.icon);
        }
    }
}
