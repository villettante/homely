package com.example.homely;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

public class HomePageBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private OnItemSelectedListener onItemSelectedListener;
    private CarouselAdapter adapter;
    private TextView bottomSheetTitle;

    public interface OnItemSelectedListener {
        void onItemSelected(String text);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.onItemSelectedListener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        bottomSheetTitle = view.findViewById(R.id.bottom_sheet_title);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<String> items = Arrays.asList("Spring Street Home", "Sommersby Street Home", "Goethe Webber Apartment Complex");
        bottomSheetTitle.setText(items.get(0));
        adapter = new CarouselAdapter(getContext(), items, this, new CarouselAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String text) {
                if (onItemSelectedListener != null) {
                    onItemSelectedListener.onItemSelected(text);
                    bottomSheetTitle.setText(text);
                }
                dismiss();
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.selectButton(0);

        return view;
    }
}
