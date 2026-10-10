package com.emailintel.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Email {
    public String id;
    public String sender;
    public String recipient;
    public String subject;
    public String body;
    public LocalDateTime receivedAt;
    public String category;
    public boolean spam;
    public boolean duplicate;
    public int priority;
    public List<String> similarIds = new ArrayList<>();
    public String reason;

    public Email() {}

    public Email(String id, String sender, String recipient, String subject, String body, LocalDateTime receivedAt) {
        this.id = id; this.sender = sender; this.recipient = recipient; this.subject = subject;
        this.body = body; this.receivedAt = receivedAt;
    }

    public String searchableText() { return (subject + " " + body).toLowerCase(); }
}
