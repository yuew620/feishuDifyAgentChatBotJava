package com.sdxpub.feishubot.model.dify;

import com.fasterxml.jackson.annotation.JsonProperty;
public class DifyRequest {
    private String query;
    private String user;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("response_mode")
    private String responseMode;
    
    private Inputs inputs;

    public DifyRequest() {
    }

    public DifyRequest(String query, String user, String conversationId, String responseMode, Inputs inputs) {
        this.query = query;
        this.user = user;
        this.conversationId = conversationId;
        this.responseMode = responseMode;
        this.inputs = inputs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public Inputs getInputs() {
        return inputs;
    }

    public void setInputs(Inputs inputs) {
        this.inputs = inputs;
    }

    public static class Inputs {
        private String history;

        public Inputs() {
        }

        public Inputs(String history) {
            this.history = history;
        }

        public String getHistory() {
            return history;
        }

        public void setHistory(String history) {
            this.history = history;
        }
    }

    public static DifyRequest createRequest(String userId, String query, String conversationId) {
        Inputs inputs = new Inputs("null");
        return new DifyRequest(query, userId, conversationId, "streaming", inputs);
    }

    public static DifyRequest createInitialRequest(String userId, String query) {
        return createRequest(userId, query, null);
    }
}
