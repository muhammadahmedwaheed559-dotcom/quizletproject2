package com.example.quizletproject2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private FirebaseFirestore db;
    private String chatroomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String otherUserId = getIntent().getStringExtra("other_user_id");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatroomId = getChatroomId(currentUserId, otherUserId);

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
        db.collection("chatrooms").document(chatroomId).collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    messageList.clear();
                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            messageList.add(dc.getDocument().toObject(Message.class));
                        }
                    }
                    messageAdapter.notifyDataSetChanged();
                });
    }

    private void sendMessage(String message) {
        Message chatMessage = new Message(message, FirebaseAuth.getInstance().getCurrentUser().getUid());
        db.collection("chatrooms").document(chatroomId).collection("messages").add(chatMessage);
    }

    private String getChatroomId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) > 0) {
            return userId1 + userId2;
        }
        return userId2 + userId1;
    }
}
