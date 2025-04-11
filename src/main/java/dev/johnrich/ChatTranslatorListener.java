package dev.johnrich;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ChatTranslatorListener implements ClientModInitializer {
    public static AITranslationService translationService;

    public static final Map<UUID, ChatEntry> messageCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, ChatEntry> eldest) {
            return size() > 100;
        }
    };
    
    public static TranslatorConfig config;

    public static String sanitizeForMinecraft(String input) {
        return input.replaceAll("[^\\x20-\\x7E\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
    }

    @Override
    public void onInitializeClient() {
        translationService = new AITranslationService();
        config = TranslatorConfig.load();

        System.out.println("[T] Translator listener initialized");

        // Intercept all incoming messages.
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (message != null) {
                UUID id = UUID.randomUUID();
                messageCache.put(id, new ChatEntry(message));
            }
            return true;
        });

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            UUID id = UUID.randomUUID();

            messageCache.put(id, new ChatEntry(message));

            return message;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> messageCache.clear());
    }
}
