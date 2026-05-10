package com.dsi.studyhub.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    private AiServiceImpl aiService;

    // Builds the service with a mocked chat client chain.
    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        aiService = new AiServiceImpl(chatClientBuilder);
    }

    // Returns the model content for the main chat path.
    @Test
    void chat_returnsContent() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("hello");

        String result = aiService.chat("hi", "system");

        assertThat(result).isEqualTo("hello");
    }

    // Returns the model content for description improvement.
    @Test
    void improveDescription_returnsContent() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("improved");

        String result = aiService.ImproveDescription("rough text");

        assertThat(result).isEqualTo("improved");
    }

    // Interprets a true response as safe content.
    @Test
    void isContentSafe_trueResponse_returnsTrue() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("TRUE");

        boolean result = aiService.isContentSafe("ok");

        assertThat(result).isTrue();
    }

    // Interprets a false response as unsafe content.
    @Test
    void isContentSafe_falseResponse_returnsFalse() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("false");

        boolean result = aiService.isContentSafe("bad");

        assertThat(result).isFalse();
    }
}
