package com.example.quizletproject2; // CHECK YOUR PACKAGE NAME

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView; // Import CardView

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

// --- AdMob Imports ---
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private LinearLayout scanCard; // Variable for the Scan Card
    private CardView aiCard;       // Variable for the AI Chat Card (Changed to CardView)
    private CardView chatCard;     // Variable for the Real-time Chat Card (Study Group)
    private ImageView profileIcon;

    // --- AdMob Variables ---
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize AdMob SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // SDK Initialized
            }
        });

        // 2. Load Banner Ad
        mAdView = findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        // 3. Pre-load Interstitial Ad
        loadInterstitialAd();

        mAuth = FirebaseAuth.getInstance();

        // (Optional) Save User to Firestore for Phase 5 discovery
        saveUserToFirestore();

        // 4. Find the Views by ID
        scanCard = findViewById(R.id.cardScanner);
        profileIcon = findViewById(R.id.ivProfile);
        aiCard = findViewById(R.id.cardAiChat);
        chatCard = findViewById(R.id.cardChat);

        // 5. Set Click Listener for Scanner
        scanCard.setOnClickListener(v -> startQRScanner());

        // 6. Set Click Listener for AI Chat
        if (aiCard != null) {
            aiCard.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, AiChatActivity.class));
            });
        }

        // 7. Set Click Listener for Real-Time Chat (Study Group)
        if (chatCard != null) {
            chatCard.setOnClickListener(v -> {
                // Kept your specific Activity class name
                startActivity(new Intent(MainActivity.this, StudyGroupChatActivity.class));
            });
        }

        // 8. Set Click Listener for Profile Icon
        profileIcon.setOnClickListener(this::showPopupMenu);
    }

    // Helper method to load the Interstitial Ad
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        // Test ID for Interstitial: ca-app-pub-3940256099942544/1033173712
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                mInterstitialAd = null;
                                loadInterstitialAd(); // Load the next one
                                Toast.makeText(MainActivity.this, "Ad Closed", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                mInterstitialAd = null;
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
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
                // User cancelled the scan
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                // QR Code detected!
                String scannedData = result.getContents();
                Toast.makeText(this, "Scanned: " + scannedData, Toast.LENGTH_LONG).show();

                // SHOW INTERSTITIAL AD HERE
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.");
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void saveUserToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("uid", currentUser.getUid());
            userMap.put("email", currentUser.getEmail());

            db.collection("users").document(currentUser.getUid())
                    .set(userMap)
                    .addOnFailureListener(e ->
                            Toast.makeText(MainActivity.this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

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