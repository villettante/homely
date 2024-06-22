package com.example.homely;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomePageBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public HomePageBottomSheetDialogFragment() {}
    private OnItemSelectedListener onItemSelectedListener;
    private CarouselAdapter adapter;
    private TextView bottomSheetTitle;
    DatabaseReference databaseReference;
    List<Home> homes;
    Home currentHome;
    User user;

    public HomePageBottomSheetDialogFragment(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
        this.homes = new ArrayList<>();
    }

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

    public static HomePageBottomSheetDialogFragment newInstance(User user) {
        HomePageBottomSheetDialogFragment fragment = new HomePageBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        fragment.setArguments(args);
        return fragment;
    }

    private void fetchHomeNames(User user, OnFetchCompleteListener listener) {
        for (Home home : user.getHomes()) {
            databaseReference.child("homes").child(home.getId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String homeName = snapshot.child("name").getValue(String.class);
                                home.setName(homeName);
                                if (home.isCurrent()) {
                                    currentHome = home;
                                }
                            }

                            if (homes.size() == user.getHomes().size()) {
                                listener.onFetchComplete();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (homes.size() == user.getHomes().size()) {
                                listener.onFetchComplete();
                            }
                        }
                    });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);
        user = (User) getArguments().getSerializable("user");
        Log.e("HomePageBottomSheetDialogFragment", "User received: " + user.getEmail());

        databaseReference = FirebaseDatabase.getInstance().getReference();

        bottomSheetTitle = view.findViewById(R.id.bottom_sheet_title);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        homes = new ArrayList<>();
        fetchHomeNames(user, new OnFetchCompleteListener() {
            @Override
            public void onFetchComplete() {
                Log.e("!!!", homes.toString());
                if (currentHome != null) {
                    bottomSheetTitle.setText(currentHome.getName());
                }

                adapter = new CarouselAdapter(getContext(), homes, HomePageBottomSheetDialogFragment.this, new CarouselAdapter.OnItemClickListener() {
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
            }
        });
        return view;
    }
}
