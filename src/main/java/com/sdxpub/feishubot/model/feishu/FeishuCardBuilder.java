package com.sdxpub.feishubot.model.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sdxpub.feishubot.model.message.Message;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;

@Slf4j
public class FeishuCardBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static RequestBody buildEmptyCard() {
        try {
            ObjectNode cardNode = objectMapper.createObjectNode();
            cardNode.put("msg_type", "interactive");

            ObjectNode cardContent = objectMapper.createObjectNode();
            cardContent.put("config", objectMapper.createObjectNode()
                .put("wide_screen_mode", true)
                .put("update_multi", true));

            ObjectNode header = objectMapper.createObjectNode();
            header.put("title", objectMapper.createObjectNode()
                .put("tag", "plain_text")
                .put("content", "思考中..."));
            cardContent.set("header", header);

            ObjectNode elements = objectMapper.createObjectNode();
            elements.put("tag", "div");
            elements.set("text", objectMapper.createObjectNode()
                .put("tag", "plain_text")
                .put("content", "正在处理您的请求..."));
            cardContent.set("elements", objectMapper.createArrayNode().add(elements));

            cardNode.set("card", cardContent);

            return RequestBody.create(objectMapper.writeValueAsString(cardNode), JSON);
        } catch (Exception e) {
            log.error("Failed to build empty card", e);
            return null;
        }
    }

    public static RequestBody buildSendCard(Message message, FeishuCard feishuCard) {
        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("receive_id", message.getUserId());
            requestNode.put("msg_type", "interactive");
            requestNode.put("card_id", feishuCard.getCardId());

            ObjectNode cardContent = objectMapper.createObjectNode();
            cardContent.put("config", objectMapper.createObjectNode()
                .put("wide_screen_mode", true)
                .put("update_multi", true));

            ObjectNode header = objectMapper.createObjectNode();
            header.put("title", objectMapper.createObjectNode()
                .put("tag", "plain_text")
                .put("content", "对话中..."));
            cardContent.set("header", header);

            ObjectNode elements = objectMapper.createObjectNode();
            elements.put("tag", "div");
            elements.set("text", objectMapper.createObjectNode()
                .put("tag", "plain_text")
                .put("content", "等待回复..."));
            cardContent.set("elements", objectMapper.createArrayNode().add(elements));

            requestNode.set("card", cardContent);

            return RequestBody.create(objectMapper.writeValueAsString(requestNode), JSON);
        } catch (Exception e) {
            log.error("Failed to build send card request", e);
            return null;
        }
    }

    public static RequestBody buildUpdateCard(String cardId, String content) {
        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("card_id", cardId);

            ObjectNode cardContent = objectMapper.createObjectNode();
            cardContent.put("config", objectMapper.createObjectNode()
                .put("wide_screen_mode", true)
                .put("update_multi", true));

            ObjectNode header = objectMapper.createObjectNode();
            header.put("title", objectMapper.createObjectNode()
                .put("tag", "plain_text")
                .put("content", "回复"));
            cardContent.set("header", header);

            ObjectNode elements = objectMapper.createObjectNode();
            elements.put("tag", "div");
            elements.set("text", objectMapper.createObjectNode()
                .put("tag", "lark_md")
                .put("content", content));
            cardContent.set("elements", objectMapper.createArrayNode().add(elements));

            requestNode.set("card", cardContent);

            return RequestBody.create(objectMapper.writeValueAsString(requestNode), JSON);
        } catch (Exception e) {
            log.error("Failed to build update card request", e);
            return null;
        }
    }
}
