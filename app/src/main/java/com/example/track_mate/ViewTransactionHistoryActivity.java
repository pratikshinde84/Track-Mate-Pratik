package com.example.track_mate;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewTransactionHistoryActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private RecyclerView recyclerView;
    private TransactionAdapter transactionAdapter;
    private List<String> transactionDetails;
    private Spinner spinnerFilter;

    private static final String PREFS_NAME = "BudgetPrefs";
    private static final String KEY_BUDGET = "budget_amount";
    private static final String KEY_FILTER_SELECTION = "filter_selection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_transaction_history);

        recyclerView = findViewById(R.id.recyclerView);
        transactionDetails = new ArrayList<>();
        spinnerFilter = findViewById(R.id.spinner_filter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(transactionDetails);
        recyclerView.setAdapter(transactionAdapter);

        // Restore last selected filter
        int lastSelectedFilter = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_FILTER_SELECTION, 0);
        spinnerFilter.setSelection(lastSelectedFilter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putInt(KEY_FILTER_SELECTION, position);
                editor.apply();
                fetchMessages(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Request SMS permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        } else {
            fetchMessages(lastSelectedFilter);
        }

        // Register an SMS content observer
        getContentResolver().registerContentObserver(
                Uri.parse("content://sms/inbox"), true, new SMSObserver(new Handler(Looper.getMainLooper())));
    }

    private void fetchMessages(int filterOption) {
        Uri uri = Uri.parse("content://sms/inbox");
        long startDateMillis = 0;

        Calendar calendar = Calendar.getInstance();

        if (filterOption == 0) { // Last Week
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            startDateMillis = calendar.getTimeInMillis();
        } else if (filterOption == 1) { // Last 30 Days
            calendar.add(Calendar.DAY_OF_YEAR, -30);
            startDateMillis = calendar.getTimeInMillis();
        }

        String selection = startDateMillis > 0 ? "date >= ?" : null;
        String[] selectionArgs = startDateMillis > 0 ? new String[]{String.valueOf(startDateMillis)} : null;

        Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, null);
        double totalAmountSpent = 0;
        transactionDetails.clear();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                Log.d("SMSBody", body);

                if (body.toLowerCase().contains("debited")) {
                    try {
                        String regex = "debited Rs\\. ([0-9.,]+) on ([0-9-]+) to ([^.]+)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(body);

                        if (matcher.find()) {
                            String amountStr = matcher.group(1);
                            String date = matcher.group(2);
                            String receiver = matcher.group(3).trim();

                            transactionDetails.add("Rs. " + amountStr + " sent to " + receiver + " on " + date + "\n");

                            double amount = Double.parseDouble(amountStr.replaceAll(",", ""));
                            totalAmountSpent += amount;

                            // Check budget only if the notification is being sent due to a transaction.
                            checkBudgetAndNotify(amount); // Pass only the current transaction amount.
                        }
                    } catch (Exception e) {
                        Log.e("ParseError", "Error parsing SMS: " + e.getMessage());
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.d("FetchMessages", "Cursor is null or empty.");
        }

        saveTotalAmountSpent(totalAmountSpent);
        transactionAdapter.notifyDataSetChanged();

        if (transactionDetails.isEmpty()) {
            Toast.makeText(this, "No debited messages found.", Toast.LENGTH_SHORT).show();
        }
    }



    private void saveTotalAmountSpent(double totalAmountSpent) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putFloat("total_spent", (float) totalAmountSpent)
                .apply();
    }

    private void checkBudgetAndNotify(double totalAmountSpent) {
        float budget = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_BUDGET, 0);

        if (totalAmountSpent > budget) {
            showNotification(totalAmountSpent, budget);
        }
    }

    private void showNotification(double totalAmountSpent, float budget) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "budget_notification_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Budget Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, HomeActivity.class); // Change to your main activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.icon) // Your notification icon
                .setContentTitle("Budget Alert")
                .setContentText("You have exceeded your budget! Total Spent: Rs. " + totalAmountSpent + ", Budget: Rs. " + budget)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // Attach the pending intent
                .setAutoCancel(true); // Dismiss the notification after it has been clicked

        notificationManager.notify(1, builder.build());
    }





    private class SMSObserver extends ContentObserver {
        public SMSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            fetchLatestTransaction(); // Fetch the latest SMS and process it
        }
    }

    private void fetchLatestTransaction() {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, null, null, "date DESC LIMIT 1");

        if (cursor != null && cursor.moveToFirst()) {
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            cursor.close();

            // Process SMS if it's a debited transaction
            if (body.toLowerCase().contains("debited")) {
                double amount = parseAmountFromSMS(body);
                if (amount > 0) {
                    double totalSpent = updateTotalSpent(amount);
                    checkBudgetAndNotify(totalSpent);
                }
            }
        }
    }

    private double parseAmountFromSMS(String body) {
        double amount = 0;
        try {
            String regex = "debited Rs\\. ([0-9.,]+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(body);

            if (matcher.find()) {
                String amountStr = matcher.group(1).replaceAll(",", "");
                amount = Double.parseDouble(amountStr);
            }
        } catch (Exception e) {
            Log.e("ParseError", "Error parsing SMS: " + e.getMessage());
        }
        return amount;
    }

    private double updateTotalSpent(double amount) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        double totalSpent = prefs.getFloat("total_spent", 0) + amount;
        prefs.edit().putFloat("total_spent", (float) totalSpent).apply();
        return totalSpent;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch messages
                fetchMessages(0); // Default to fetch messages with no filter
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
