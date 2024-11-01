package com.example.track_mate;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.core.app.NotificationCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String messageBody = smsMessage.getMessageBody();

                    // Check for "debited" in the message
                    if (messageBody.toLowerCase().contains("debited")) {
                        // Extract amount and process transaction
                        processTransaction(context, messageBody);
                    }
                }
            }
        }
    }

    private void processTransaction(Context context, String messageBody) {
        try {
            String regex = "debited Rs\\. ([0-9.,]+) on ([0-9-]+) to ([^.]+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(messageBody);

            if (matcher.find()) {
                String amountStr = matcher.group(1);
                double amount = Double.parseDouble(amountStr.replaceAll(",", ""));

                // Update total money spent in SharedPreferences
                SharedPreferences sharedPreferences = context.getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE);
                float totalSpent = sharedPreferences.getFloat("total_spent", 0);
                totalSpent += amount;

                sharedPreferences.edit().putFloat("total_spent", (float) totalSpent).apply();

                // Check if total spent exceeds budget and send notification
                checkBudgetAndNotify(context, totalSpent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkBudgetAndNotify(Context context, float totalSpent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE);
        float budget = sharedPreferences.getFloat("budget", 0); // Get your budget limit from SharedPreferences

        if (totalSpent > budget) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "budget_notification_channel")
                    .setSmallIcon(R.drawable.icon) // Your notification icon
                    .setContentTitle("Budget Exceeded")
                    .setContentText("Your spending has exceeded the budget limit!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());
        }
    }
}
