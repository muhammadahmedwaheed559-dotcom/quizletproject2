package com.example.quizletproject2;

public class Message {
    private final String message;
    private final String sentBy; // "user" or "bot"

    public static final String SENT_BY_ME = "user";
    public static final String SENT_BY_BOT = "bot";

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
    }

    public String getMessage() {
        return message;
    }

    public String getSentBy() {
        return sentBy;
    }
}
