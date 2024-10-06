package com.example.track_mate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.card_set_limit).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, SetBudgetActivity.class));
        });

        findViewById(R.id.card_view_history).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ViewTransactionHistoryActivity.class));
        });

        findViewById(R.id.card_total_spent).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TotalMoneySpentActivity.class));
        });

        findViewById(R.id.card_export_pdf).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ExportPDFActivity.class));
        });
    }
}
