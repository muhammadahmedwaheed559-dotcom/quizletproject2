package com.example.quizletproject2;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String message;
    private String sentBy;
    private Date timestamp;

    public static final String SENT_BY_ME = "user";
    public static final String SENT_BY_BOT = "bot";

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
