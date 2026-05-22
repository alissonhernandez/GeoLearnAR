package com.example.guiaeducativaar;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    MaterialButton btnExplorar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnExplorar = findViewById(R.id.btnExplorar);

        btnExplorar.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WelcomeActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });
    }
}