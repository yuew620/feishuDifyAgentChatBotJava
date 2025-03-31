package com.sdxpub.feishubot.model.dify;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
public class DifyResponse {
    private String event;
    private String task;
    private String id;
    private String answer;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    private Usage usage;
    private String message;
    
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Usage() {
        }

        public Usage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DifyResponse fromJson(String json) {
        try {
            return objectMapper.readValue(json, DifyResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing Dify response: " + e.getMessage(), e);
        }
    }

    public DifyResponse() {
    }

    public DifyResponse(String event, String task, String id, String answer, String conversationId, Usage usage, String message) {
        this.event = event;
        this.task = task;
        this.id = id;
        this.answer = answer;
        this.conversationId = conversationId;
        this.usage = usage;
        this.message = message;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DifyMessage toDifyMessage(String userId) {
        return new DifyMessage(
            conversationId,
            userId,
            null,  // query
            answer,
            isAgentMessage() ? DifyMessage.MessageType.AGENT : DifyMessage.MessageType.ASSISTANT,
            id
        );
    }

}
