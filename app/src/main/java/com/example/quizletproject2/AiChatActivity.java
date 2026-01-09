package com.example.quizletproject2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private Spinner languageSpinner;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    // IMPORTANT: PASTE YOUR GEMINI API KEY HERE
    private static final String API_KEY = "MY API KEY";

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final OkHttpClient client = new OkHttpClient();

    private SharedPreferences sharedPreferences;
    private String selectedLanguage = "English"; // Default language

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        messageList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerViewChat);
        messageEditText = findViewById(R.id.etMessageInput);
        sendButton = findViewById(R.id.btnSend);
        languageSpinner = findViewById(R.id.languageSpinner);

        // --- Setup Language Spinner ---
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        loadLanguagePreference(); // Load saved language

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = parent.getItemAtPosition(position).toString();
                saveLanguagePreference(selectedLanguage);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // --- Setup RecyclerView ---
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener(v -> {
            String question = messageEditText.getText().toString().trim();
            if (question.isEmpty()) return;

            if (API_KEY.isEmpty()) {
                Toast.makeText(this, "Please add your Gemini API key", Toast.LENGTH_LONG).show();
                addToChat("API Key is missing.", Message.SENT_BY_BOT);
                return;
            }

            addToChat(question, Message.SENT_BY_ME);
            messageEditText.setText("");
            callGeminiAPI(question);
        });
    }

    private void loadLanguagePreference() {
        String savedLanguage = sharedPreferences.getString("selected_language", "English");
        selectedLanguage = savedLanguage;
        for (int i = 0; i < languageSpinner.getCount(); i++) {
            if (languageSpinner.getItemAtPosition(i).toString().equalsIgnoreCase(savedLanguage)) {
                languageSpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveLanguagePreference(String language) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selected_language", language);
        editor.apply();
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void callGeminiAPI(String question) {
        addToChat("Thinking...", Message.SENT_BY_BOT);

        // --- Build the prompt with language instruction ---
        String prompt = String.format("You are a helpful study assistant. Answer this in %s: %s", selectedLanguage, question);

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            jsonBody.put("contents", contents);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(URL).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to connect: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String aiResponse = parts.getJSONObject(0).getString("text");
                        addResponse(aiResponse.trim());
                    } catch (JSONException e) {
                        addResponse("Error parsing AI response");
                    }
                } else {
                    addResponse("Server Error: " + response.code());
                }
            }
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1); // Remove "Thinking..."
        addToChat(response, Message.SENT_BY_BOT);
    }
}
