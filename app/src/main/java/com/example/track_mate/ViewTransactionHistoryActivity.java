package com.example.track_mate;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

    private static final String PREFS_NAME = "BudgetPrefs";
    private static final String KEY_BUDGET = "budget_amount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_transaction_history);

        recyclerView = findViewById(R.id.recyclerView);
        transactionDetails = new ArrayList<>();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(transactionDetails);
        recyclerView.setAdapter(transactionAdapter);

        // Check for SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        } else {
            fetchMessages();
        }
    }

    private void fetchMessages() {
        Uri uri = Uri.parse("content://sms/inbox");

        // Get the start of the current month in milliseconds
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = calendar.getTimeInMillis();

        // Define the selection criteria to fetch messages from the current month
        String selection = "date >= ?";
        String[] selectionArgs = new String[]{String.valueOf(startOfMonth)};

        Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, null);
        double totalAmountSpent = 0; // Initialize total amount spent

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));

                // Check for messages containing "debited"
                if (body.toLowerCase().contains("debited")) {
                    // Extract amount, date, and receiver from the message body
                    try {
                        String regex = "debited Rs\\. ([0-9.,]+) on ([0-9-]+) to ([^.]+)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(body);

                        if (matcher.find()) {
                            String amountStr = matcher.group(1);
                            String date = matcher.group(2);
                            String receiver = matcher.group(3).trim();

                            // Add the transaction detail to the list
                            transactionDetails.add("Rs. " + amountStr + " sent to " + receiver + " on " + date + "\n");

                            // Add to total amount spent
                            double amount = Double.parseDouble(amountStr.replaceAll(",", ""));
                            totalAmountSpent += amount;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Save total amount spent in SharedPreferences
        saveTotalAmountSpent(totalAmountSpent);

        // Notify the adapter to update the RecyclerView
        transactionAdapter.notifyDataSetChanged();

        if (transactionDetails.isEmpty()) {
            Toast.makeText(this, "No debited messages found for the current month.", Toast.LENGTH_SHORT).show();
        }

        // Check if total amount spent exceeds budget and send notification
        checkBudgetAndNotify(totalAmountSpent);
    }

    private void saveTotalAmountSpent(double totalAmountSpent) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putFloat("total_spent", (float) totalAmountSpent)
                .apply();
    }

    private void checkBudgetAndNotify(double totalAmountSpent) {
        float budget = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_BUDGET, 0); // Using the same key as in SetBudgetActivity

        if (totalAmountSpent > budget) {
            showNotification(totalAmountSpent, budget);
        }
    }

    private void showNotification(double totalAmountSpent, float budget) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "budget_notification_channel";

        // Create Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Budget Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.icon) // Replace with your notification icon
                .setContentTitle("Budget Exceeded!")
                .setContentText("You've spent ₹" + totalAmountSpent + ", exceeding your budget of ₹" + budget)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show notification
        notificationManager.notify(1, builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchMessages();
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
