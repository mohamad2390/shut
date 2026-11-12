package com.shut.telegram.client;

import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.config.TelegramProperties;
import com.shut.telegram.model.AnswerCallbackQueryRequest;
import com.shut.telegram.model.AnswerWidgetQueryRequest;
import com.shut.telegram.model.ApiResponse;
import com.shut.telegram.model.ChatMember;
import com.shut.telegram.model.CopyMessageRequest;
import com.shut.telegram.model.DeleteMessageRequest;
import com.shut.telegram.model.EditMessageReplyMarkupRequest;
import com.shut.telegram.model.EditMessageTextRequest;
import com.shut.telegram.model.GetChatMemberRequest;
import com.shut.telegram.model.GetUpdatesRequest;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.MessageId;
import com.shut.telegram.model.SendMediaRequest;
import com.shut.telegram.model.SendMessageRequest;
import com.shut.telegram.model.SendWidgetRequest;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@TelegramBotEnabled
class RestTelegramApiClient implements TelegramApiClient {

    private static final List<String> ALLOWED_UPDATES = List.of("message", "callback_query", "widget_query");

    private final RestClient restClient;
    private final String token;

    RestTelegramApiClient(RestClient telegramRestClient, TelegramProperties properties) {
        this.restClient = telegramRestClient;
        this.token = properties.token();
    }

    @Override
    public TelegramUser getMe() {
        return call("getMe", null, new ParameterizedTypeReference<ApiResponse<TelegramUser>>() { });
    }

    @Override
    public List<Update> getUpdates(long offset, int timeoutSeconds) {
        GetUpdatesRequest body = new GetUpdatesRequest(offset, timeoutSeconds, ALLOWED_UPDATES);
        return call("getUpdates", body, new ParameterizedTypeReference<ApiResponse<List<Update>>>() { });
    }

    @Override
    public ChatMember getChatMember(String chatId, long userId) {
        GetChatMemberRequest body = new GetChatMemberRequest(chatId, userId);
        return call("getChatMember", body, new ParameterizedTypeReference<ApiResponse<ChatMember>>() { });
    }

    @Override
    public void sendMessage(SendMessageRequest request) {
        call("sendMessage", request, new ParameterizedTypeReference<ApiResponse<Message>>() { });
    }

    @Override
    public void editMessageText(EditMessageTextRequest request) {
        call("editMessageText", request, new ParameterizedTypeReference<ApiResponse<Message>>() { });
    }

    @Override
    public void sendMedia(SendMediaRequest request) {
        call(request.apiMethod(), request, new ParameterizedTypeReference<ApiResponse<Message>>() { });
    }

    @Override
    public long sendWidget(SendWidgetRequest request) {
        return call("sendWidget", request, new ParameterizedTypeReference<ApiResponse<Message>>() { }).messageId();
    }

    @Override
    public void answerWidgetQuery(String widgetQueryId, String data) {
        call("answerWidgetQuery", new AnswerWidgetQueryRequest(widgetQueryId, data),
                new ParameterizedTypeReference<ApiResponse<Boolean>>() { });
    }

    @Override
    public void editMessageReplyMarkup(EditMessageReplyMarkupRequest request) {
        call("editMessageReplyMarkup", request, new ParameterizedTypeReference<ApiResponse<Message>>() { });
    }

    @Override
    public long copyMessage(String toChatId, long fromChatId, long messageId) {
        MessageId copy = call("copyMessage", new CopyMessageRequest(toChatId, fromChatId, messageId),
                new ParameterizedTypeReference<ApiResponse<MessageId>>() { });
        return copy.messageId();
    }

    @Override
    public void deleteMessage(String chatId, long messageId) {
        call("deleteMessage", new DeleteMessageRequest(chatId, messageId),
                new ParameterizedTypeReference<ApiResponse<Boolean>>() { });
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        call("answerCallbackQuery", new AnswerCallbackQueryRequest(callbackQueryId),
                new ParameterizedTypeReference<ApiResponse<Boolean>>() { });
    }

    @Override
    public void deleteWebhook() {
        call("deleteWebhook", null, new ParameterizedTypeReference<ApiResponse<Boolean>>() { });
    }

    private <T> T call(String method, Object body, ParameterizedTypeReference<ApiResponse<T>> responseType) {
        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri("/{method}", method)
                    .contentType(MediaType.APPLICATION_JSON);
            if (body != null) {
                spec.body(body);
            }
            ApiResponse<T> response = spec.retrieve().body(responseType);

            if (response == null || !response.ok()) {
                String reason = response == null ? "empty response" : response.description();
                throw new TelegramApiException("Telegram " + method + " failed: " + reason);
            }
            return response.result();
        } catch (RestClientException e) {
            // The request URL contains the bot token, so it must be scrubbed before it reaches any log.
            throw new TelegramApiException("Telegram " + method + " failed: " + scrub(e.getMessage()));
        }
    }

    private String scrub(String message) {
        return message == null ? "no details" : message.replace(token, "***");
    }
}
