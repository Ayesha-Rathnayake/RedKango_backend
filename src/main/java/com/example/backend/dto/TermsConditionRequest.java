package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermsConditionRequest {

    private String title;

    private String version;

    private String content;

    private boolean active;
}