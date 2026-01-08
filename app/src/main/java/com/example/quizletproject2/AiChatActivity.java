package com.example.quizletproject2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

    RecyclerView recyclerView;
    EditText messageEditText;
    Button sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;

    // YOUR API KEY
    public static final String API_KEY = "YOUR_API_KEY";

    // Google Gemini API URL
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        messageList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerViewChat);
        messageEditText = findViewById(R.id.etMessageInput);
        sendButton = findViewById(R.id.btnSend);

        // Setup RecyclerView
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true); // Scroll to bottom
        recyclerView.setLayoutManager(llm);

        client = new OkHttpClient();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = messageEditText.getText().toString().trim();
                if (question.isEmpty()) {
                    return;
                }
                addToChat(question, Message.SENT_BY_ME);
                messageEditText.setText("");
                callGeminiAPI(question);
            }
        });
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void callGeminiAPI(String question) {
        JSONObject jsonBody = new JSONObject();
        try {
            JSONObject part = new JSONObject();
            part.put("text", "You are a helpful study assistant. Answer this: " + question);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            jsonBody.put("contents", contents);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                addToChat("Failed to connect: " + e.getMessage(), Message.SENT_BY_BOT);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String aiResponse = parts.getJSONObject(0).getString("text");

                        addToChat(aiResponse, Message.SENT_BY_BOT);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        addToChat("Error parsing AI response", Message.SENT_BY_BOT);
                    }
                } else {
                    addToChat("Server Error: " + response.code(), Message.SENT_BY_BOT);
                }
            }
        });
    }
}
