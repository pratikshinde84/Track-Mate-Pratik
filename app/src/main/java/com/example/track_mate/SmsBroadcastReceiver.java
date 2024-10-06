package com.example.track_mate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

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
                        // Extract amount, date, and receiver
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

                // Save updated total in SharedPreferences
                sharedPreferences.edit().putFloat("total_spent", (float) totalSpent).apply();

                // Optionally, notify the UI (you may need to implement a method to refresh UI)
                // Example: ((MainActivity) context).updateUI();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
