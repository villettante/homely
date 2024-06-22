package com.example.homely;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class AddHomeActivity extends AppCompatActivity {

    private TextInputLayout textInputLayout;
    private TextInputEditText textInputEditText;
    private Button back, cancel, next;
    private static final int MIN_CHAR_LIMIT = 1;
    private static final int MAX_CHAR_LIMIT = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_home);

        textInputLayout = findViewById(R.id.textInputLayout);
        textInputEditText = findViewById(R.id.textInputEditText);

        back = findViewById(R.id.go_back_home_page);
        cancel = findViewById(R.id.cancel);
        next = findViewById(R.id.next_to_add_home_code_activity);
        next.setAlpha(.5f);
        next.setClickable(false);


        next.setOnClickListener(v -> {
            String homeName = Objects.requireNonNull(textInputEditText.getText()).toString();
            Intent intent = new Intent(AddHomeActivity.this, AddHomeCodeActivity.class);
            intent.putExtra("home_name", homeName);
            startActivity(intent);
            finish();
        });

        back.setOnClickListener(v -> {
            navigateBackOnHomePage();
        });

        cancel.setOnClickListener(v -> {
            navigateBackOnHomePage();
        });

        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                if (length >= 1 && length <= 20) {
                    next.setAlpha(1);
                    next.setClickable(true);
                } else {
                    next.setAlpha(.5f);
                    next.setClickable(false);
                }
                if (length > MAX_CHAR_LIMIT) {
                    textInputLayout.setError("Character limit exceeded!");
                } else if (length < MIN_CHAR_LIMIT) {
                    textInputLayout.setError("At least one character required!");
                } else {
                    textInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void navigateBackOnHomePage() {
        Intent intent = new Intent(AddHomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
