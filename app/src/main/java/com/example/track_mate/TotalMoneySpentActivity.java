package com.example.track_mate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TotalMoneySpentActivity extends AppCompatActivity {

    // SharedPreferences file name and keys for budget and total spent
    private static final String PREFS_NAME = "BudgetPrefs";
    private static final String KEY_BUDGET = "budget_amount";
    private static final String KEY_TOTAL_SPENT = "total_spent";

    private TextView textCurrentBudgetValue;
    private TextView textTotalSpentValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_money_spent);

        textCurrentBudgetValue = findViewById(R.id.text_current_budget_value);
        textTotalSpentValue = findViewById(R.id.text_total_spent_value);

        loadBudgetAndSpent();
    }

    // Method to load the budget and total money spent
    private void loadBudgetAndSpent() {
        // Load budget from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float currentBudget = sharedPreferences.getFloat(KEY_BUDGET, 0);  // Default to 0 if no budget is saved

        // Display the current budget in the TextView
        textCurrentBudgetValue.setText("₹" + currentBudget);

        // Load total amount spent from SharedPreferences
        float totalSpent = sharedPreferences.getFloat(KEY_TOTAL_SPENT, 0);  // Default to 0 if no total spent is saved
        textTotalSpentValue.setText("₹" + totalSpent);
    }
}
