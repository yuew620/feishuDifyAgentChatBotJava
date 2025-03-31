package com.sdxpub.feishubot.model.dify;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyResponse {
    private String event;
    private String task;
    private String id;
    private String answer;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    private Usage usage;
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    public boolean isAgentMessage() {
        return "agent_message".equals(event);
    }

    public boolean isMessage() {
        return "message".equals(event);
    }

    public boolean hasAnswer() {
        return answer != null && !answer.isEmpty();
    }

    public DifyMessage toDifyMessage(String userId) {
        return DifyMessage.builder()
                .userId(userId)
                .conversationId(conversationId)
                .answer(answer)
                .messageId(id)
                .type(isAgentMessage() ? DifyMessage.MessageType.AGENT : DifyMessage.MessageType.ASSISTANT)
                .build();
    }
}
