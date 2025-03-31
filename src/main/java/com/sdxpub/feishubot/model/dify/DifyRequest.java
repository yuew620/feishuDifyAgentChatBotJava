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
public class DifyRequest {
    private String query;
    private String user;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("response_mode")
    private String responseMode;
    
    private Inputs inputs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inputs {
        private String history;
    }

    public static DifyRequest createRequest(String userId, String query, String conversationId) {
        return DifyRequest.builder()
                .query(query)
                .user(userId)
                .conversationId(conversationId)
                .responseMode("streaming")
                .inputs(Inputs.builder()
                        .history("null")
                        .build())
                .build();
    }

    public static DifyRequest createInitialRequest(String userId, String query) {
        return createRequest(userId, query, null);
    }
}
