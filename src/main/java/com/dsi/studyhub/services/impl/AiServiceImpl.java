package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.services.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    public AiServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String message, String systemPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();
    }
    public String ImproveDescription(String message) {
        String languageConstraint = " Respond strictly in the same language as the input message.";
        String finalSystemPrompt =
                "Always improve the clarity and flow of the following text, no matter how well-written it already is."
                        + " There is always room for improvement — make it sharper, more engaging, and more natural."
                        + " Keep the original meaning intact."
                        + " Return only the improved text with no explanations, comments, or formatting changes."
                        + " Do not describe what you changed or add anything extra."
                        + languageConstraint;

        return chatClient.prompt()
                .system(finalSystemPrompt)
                .user(message)
                .call()
                .content();
    }
    public  boolean isContentSafe(String message) {
        String systemPrompt = """
        You are a content moderation AI. Your task is to analyze the user's input for:
        1. Hate speech or discrimination.
        2. Violence or threats.
        3. Severe profanity or harassment.
        4. Explicit or suggestive content.
        This check must be language-agnostic (works for any language).
        If the content is SAFE, return only the word "true".
        If the content is HARMFUL or VIOLATES these rules, return only the word "false".
        Do not provide explanations or punctuation.
        """;

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content()
                .trim()
                .toLowerCase();

        return response.contains("true");
    }

}
