package com.sdxpub.feishubot.model.dify;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyMessage {
    private String conversationId;
    private String userId;
    private String query;
    private String answer;
    private MessageType type;
    private String messageId;

    public enum MessageType {
        AGENT,      // Agent消息，包含answer
        ASSISTANT,  // Assistant消息，不包含answer
        USER        // 用户消息
    }

    public boolean isAgentMessage() {
        return type == MessageType.AGENT;
    }

    public boolean hasAnswer() {
        return answer != null && !answer.isEmpty();
    }

    public static DifyMessage createUserMessage(String userId, String query) {
        return DifyMessage.builder()
                .userId(userId)
                .query(query)
                .type(MessageType.USER)
                .build();
    }

    public static DifyMessage createAgentMessage(String userId, String answer) {
        return DifyMessage.builder()
                .userId(userId)
                .answer(answer)
                .type(MessageType.AGENT)
                .build();
    }

    public static DifyMessage createAssistantMessage(String userId, String messageId) {
        return DifyMessage.builder()
                .userId(userId)
                .messageId(messageId)
                .type(MessageType.ASSISTANT)
                .build();
    }
}
