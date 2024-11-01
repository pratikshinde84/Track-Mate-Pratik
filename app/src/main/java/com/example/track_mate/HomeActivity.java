package com.example.track_mate;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.FileNotFoundException;

public class HomeActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

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
            exportToPDF();
        });
    }

    private void exportToPDF() {
        // Check for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            createPdf();
        }
    }

    private void createPdf() {
        // Create a directory for the PDF
        File pdfDir = new File(getExternalFilesDir(null), "TrackMatePDFs");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        String pdfFileName = "TransactionHistory.pdf";
        File pdfFile = new File(pdfDir, pdfFileName);

        try {
            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Fetch transaction details from ViewTransactionHistoryActivity
            String[] transactions = fetchTransactionData(); // Call to fetch transaction data

            for (String transaction : transactions) {
                document.add(new Paragraph(transaction));
            }

            document.close();
            Toast.makeText(this, "PDF exported to " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String[] fetchTransactionData() {
        // Fetch transaction data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TrackMatePrefs", MODE_PRIVATE);
        String transactionsString = prefs.getString("transactions", "");
        return transactionsString.split(","); // Split the string back into an array
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPdf();
            } else {
                Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
