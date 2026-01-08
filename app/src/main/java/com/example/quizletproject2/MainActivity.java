package com.example.quizletproject2; // CHECK YOUR PACKAGE NAME

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView; // Import CardView

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private LinearLayout scanCard; // Variable for the Scan Card
    private CardView aiCard;           // Variable for the AI Chat Card (Added)
    private ImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // 1. Find the Views by ID
        scanCard = findViewById(R.id.cardScanner);
        profileIcon = findViewById(R.id.ivProfile);
        aiCard = findViewById(R.id.cardAiChat); // Finding the AI Card

        // 2. Set Click Listener for Scanner
        scanCard.setOnClickListener(v -> startQRScanner());

        // 3. Set Click Listener for AI Chat (Added)
        // This makes the card clickable and opens the AI Chat screen
        if (aiCard != null) {
            aiCard.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, AiChatActivity.class));
            });
        }

        // 4. Set Click Listener for Profile Icon
        profileIcon.setOnClickListener(this::showPopupMenu);
    }

    // Method to initiate the scan
    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR Code");
        integrator.setOrientationLocked(true); // Lock to portrait
        integrator.setBeepEnabled(true);
        integrator.initiateScan(); // This opens the camera
    }

    // Handle the result when Camera closes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // User cancelled the scan (pressed back button)
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                // QR Code detected!
                String scannedData = result.getContents();
                Toast.makeText(this, "Scanned: " + scannedData, Toast.LENGTH_LONG).show();

                // Optional: In the future, we can add logic here to do something
                // specific if the QR code is a "Quiz Set ID".
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logoutUser();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
