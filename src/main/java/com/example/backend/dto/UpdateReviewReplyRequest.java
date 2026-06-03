package com.example.backend.dto;

import jakarta.validation.constraints.Size;

public class UpdateReviewReplyRequest {

    @Size(max = 2000)
    private String reply;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}