package com.sdxpub.feishubot.model.dify;

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

    public DifyMessage() {
    }

    public DifyMessage(String conversationId, String userId, String query, String answer, MessageType type, String messageId) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.query = query;
        this.answer = answer;
        this.type = type;
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public static DifyMessage createUserMessage(String userId, String query) {
        DifyMessage message = new DifyMessage();
        message.setUserId(userId);
        message.setQuery(query);
        message.setType(MessageType.USER);
        return message;
    }

    public static DifyMessage createAgentMessage(String userId, String answer) {
        DifyMessage message = new DifyMessage();
        message.setUserId(userId);
        message.setAnswer(answer);
        message.setType(MessageType.AGENT);
        return message;
    }

    public static DifyMessage createAssistantMessage(String userId, String messageId) {
        DifyMessage message = new DifyMessage();
        message.setUserId(userId);
        message.setMessageId(messageId);
        message.setType(MessageType.ASSISTANT);
        return message;
    }
}
