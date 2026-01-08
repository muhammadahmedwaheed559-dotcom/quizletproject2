package com.example.quizletproject2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

// AdMob Imports
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

// Firebase & ZXing Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    private CardView scanCard, aiCard, chatCard, visionCard;
    private ImageView profileIcon; // This is now the logout button

    // AdMob Variables
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize AdMob SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // 2. Load Banner Ad
        mAdView = findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        // 3. Pre-load Interstitial Ad
        loadInterstitialAd();

        // 4. Find Views by ID
        scanCard = findViewById(R.id.cardScanner);
        aiCard = findViewById(R.id.cardAiChat);
        chatCard = findViewById(R.id.cardChat);
        visionCard = findViewById(R.id.cardVision);
        profileIcon = findViewById(R.id.ivProfile); // Repurposed for logout

        // 5. Set Click Listeners
        scanCard.setOnClickListener(v -> showInterstitialAndScan()); // Changed this line
        aiCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AiChatActivity.class)));
        chatCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StudyGroupChatActivity.class)));
        visionCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ObjectDetectionActivity.class)));
        
        // Logout logic is now attached to the profile icon
        profileIcon.setOnClickListener(v -> showLogoutMenu(v));
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mInterstitialAd = null;
                                loadInterstitialAd(); // Pre-load the next one
                                // Ad is dismissed, now start the scanner
                                startQRScanner();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                mInterstitialAd = null;
                                // Ad failed to show, start scanner anyway
                                startQRScanner();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    private void showInterstitialAndScan() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
            // Ad not ready, start scanner immediately
            startQRScanner();
        }
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR Code");
        integrator.setOrientationLocked(true);
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showLogoutMenu(View view) {
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, view);
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
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
