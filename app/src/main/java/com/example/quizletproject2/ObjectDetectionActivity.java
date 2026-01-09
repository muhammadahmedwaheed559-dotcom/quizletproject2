package com.example.quizletproject2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ObjectDetectionActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    // IMPORTANT: PASTE YOUR HUGGING FACE TOKEN HERE
    private static final String HF_API_TOKEN = "MY API KEY";
    // UPDATED to a more stable model
    private static final String HUGGING_FACE_API_URL = "https://router.huggingface.co/hf-inference/models/google/vit-base-patch16-224";


    private ImageView imageView;
    private TextView resultTextView;
    private Button selectButton;
    private Button analyzeButton;
    private Bitmap selectedBitmap;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detection);

        imageView = findViewById(R.id.ivPreview);
        resultTextView = findViewById(R.id.tvResult);
        selectButton = findViewById(R.id.btnSelect);
        analyzeButton = findViewById(R.id.btnAnalyze);

        selectButton.setOnClickListener(v -> openGallery());
        analyzeButton.setOnClickListener(v -> analyzeImage());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedBitmap);
                resultTextView.setText("Image selected. Ready to analyze.");
                analyzeButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeImage() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (HF_API_TOKEN.isEmpty()) {
            Toast.makeText(this, "Please add your Hugging Face API token", Toast.LENGTH_LONG).show();
            resultTextView.setText("API Token is missing.");
            return;
        }

        resultTextView.setText("Analyzing...");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = RequestBody.create(byteArray, MediaType.parse("image/jpeg"));

        Request request = new Request.Builder()
                .url(HUGGING_FACE_API_URL)
                .header("Authorization", "Bearer " + HF_API_TOKEN)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    resultTextView.setText("Analysis Failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        if (jsonArray.length() > 0) {
                            JSONObject topResult = jsonArray.getJSONObject(0);
                            String label = topResult.getString("label");
                            double score = topResult.getDouble("score");
                            String formattedResult = String.format("Object: %s\nConfidence: %.2f%%", label, score * 100);
                            runOnUiThread(() -> resultTextView.setText(formattedResult));
                        } else {
                            runOnUiThread(() -> resultTextView.setText("No objects detected."));
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> resultTextView.setText("Error parsing AI response."));
                    }
                } else {
                    runOnUiThread(() -> resultTextView.setText("Server Error: " + response.code()));
                }
            }
        });
    }
}
