package com.example.quizletproject2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class StudyGroupChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private FirebaseFirestore db;
    private int messageCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_group_chat);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        listenForMessages();
    }

    private void listenForMessages() {
        db.collection("study_group_chat").orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    messageList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        messageList.add(doc.toObject(Message.class));
                    }
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    // Auto-reply logic
                    if (!snapshots.isEmpty()) {
                        Message lastMessage = snapshots.getDocuments().get(snapshots.size() - 1).toObject(Message.class);
                        if (lastMessage != null && !lastMessage.getSentBy().equals("Study Bot")) {
                            messageCounter++;
                            if (messageCounter % 6 == 1) { // Reply on the 1st, 7th, 13th, etc. message
                                sendAutoReply();
                            }
                        }
                    }
                });
    }

    private void sendMessage(String message) {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Message chatMessage = new Message(message, userEmail);
        db.collection("study_group_chat").add(chatMessage);
    }

    private void sendAutoReply() {
        Message autoReply = new Message("This is a default message from Study Bot.", "Study Bot");
        db.collection("study_group_chat").add(autoReply);
    }
}
