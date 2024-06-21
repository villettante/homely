package com.example.homely;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.ViewHolder> {

    private final List<String> items;
    private final Context context;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final HomePageBottomSheetDialogFragment bottomSheetDialogFragment;
    private OnItemClickListener onItemClickListener;

    public CarouselAdapter(Context context, List<String> items, HomePageBottomSheetDialogFragment bottomSheetDialogFragment, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.items = items;
        this.bottomSheetDialogFragment = bottomSheetDialogFragment;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.room_carousel_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        holder.button.setText(item);
        holder.button.setSelected(position == selectedPosition);
        holder.button.setElevation(0);

        if (position == selectedPosition) {
            holder.button.setTextColor(context.getColor(R.color.text_color_tertiary));
        } else {
            holder.button.setTextColor(context.getColor(R.color.text_color_header));
        }

        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyItemChanged(selectedPosition);
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(selectedPosition);
                onItemClickListener.onItemClick(item);
                bottomSheetDialogFragment.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String text);
    }

    public void selectButton(int position) {
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(selectedPosition);
            selectedPosition = position;
            notifyItemChanged(selectedPosition);
        }
    }

    public String getSelectedItem() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            return items.get(selectedPosition);
        }
        return null;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        Button button;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.carousel_button);
        }
    }
}