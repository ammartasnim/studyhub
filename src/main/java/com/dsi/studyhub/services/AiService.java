package com.dsi.studyhub.services;

public interface AiService {
    String chat(String message, String systemPrompt);
    String ImproveDescription(String message);
    boolean isContentSafe(String message);
}
