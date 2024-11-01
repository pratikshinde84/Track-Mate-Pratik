package com.example.track_mate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SetBudgetActivity extends AppCompatActivity {

    private EditText editTextBudget;

    private static final String PREFS_NAME = "BudgetPrefs";
    private static final String KEY_BUDGET = "budget_amount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_budget);

        editTextBudget = findViewById(R.id.edit_text_budget);
        Button buttonSetBudget = findViewById(R.id.button_set_budget);

        loadBudget();

        buttonSetBudget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBudget();
            }
        });
    }

    private void setBudget() {
        String budgetInput = editTextBudget.getText().toString().trim();

        if (budgetInput.isEmpty()) {
            Toast.makeText(SetBudgetActivity.this, "Please enter a budget amount", Toast.LENGTH_SHORT).show();
        } else {
            // Convert the input to a number and store it in SharedPreferences
            double budgetAmount = Double.parseDouble(budgetInput);
            saveBudget(budgetAmount);

            // Show a Toast message
            Toast.makeText(SetBudgetActivity.this, "Budget set to: ₹" + budgetAmount, Toast.LENGTH_LONG).show();

            redirectToHome();
        }
    }

    private void saveBudget(double budgetAmount) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_BUDGET, (float) budgetAmount);
        editor.apply();
    }

    private void loadBudget() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float savedBudget = sharedPreferences.getFloat(KEY_BUDGET, 0);  // Default to 0 if no budget is saved

        if (savedBudget > 0) {
            editTextBudget.setText(String.valueOf(savedBudget));
        }
    }

    private void redirectToHome() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SetBudgetActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }, 500);
    }
}
