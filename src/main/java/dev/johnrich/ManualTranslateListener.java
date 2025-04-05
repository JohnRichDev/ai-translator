package dev.johnrich;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManualTranslateListener implements ClientModInitializer {
    private AITranslationService translationService;
    private final Map<UUID, String> messageCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    private TranslatorConfig config;

    private String sanitizeForMinecraft(String input) {
        return input.replaceAll("[^\\x20-\\x7E\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
    }

    @Override
    public void onInitializeClient() {
        translationService = new AITranslationService();
        config = TranslatorConfig.load();

        System.out.println("[Translator] Translator listener initialized");

        // Intercept all incoming chat messages.
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (sender.getId().equals(client.player.getUuid())) {
                return true;
            }

            String originalMessage = message.getString();
            UUID messageId = UUID.randomUUID();
            messageCache.put(messageId, originalMessage);
            if (messageCache.size() > MAX_CACHE_SIZE) {
                messageCache.remove(messageCache.keySet().iterator().next());
            }

            MutableText translateButton = Text.literal(" [T]")
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN)
                            .withClickEvent(new ClickEvent.RunCommand("/translate-message " + messageId))
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to translate this message"))));
            MutableText modifiedMessage = Text.empty().append(message).append(translateButton);

            client.execute(() -> {
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(modifiedMessage);
                }
            });
            return false;
        });

        // Register the translate command.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("translate-message")
                            .then(ClientCommandManager.argument("messageId", UuidArgumentType.uuid())
                                    .executes(context -> {
                                        if ("your-gemini-api-key-here".equals(config.geminiApiKey)) {
                                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                                    .addMessage(Text.of("[Translator] §cGemini API key is not set! §eUse §a.setkey <your-api-key> §eto set it."));
                                            return 1;
                                        }
                                        if ("not-set".equals(config.chatTranslatorLang)) {
                                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                                    .addMessage(Text.of("[Translator] §cTranslation language is not set! §eUse §a.setlang command <lang> §eto change."));
                                            return 1;
                                        }
                                        UUID messageId = context.getArgument("messageId", UUID.class);
                                        String originalMessage = messageCache.get(messageId);

                                        if (originalMessage == null) {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            client.inGameHud.getChatHud().addMessage(Text.of("[Translator] §cTranslate Error: Message not found in cache"));
                                            return 1;
                                        }

                                        String modifiedString = originalMessage.replaceAll("<.*?>", "<>");
                                        String lang = config.chatTranslatorLang;

                                        translationService.translateChat(lang, modifiedString)
                                                .thenAccept(translatedText -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    client.execute(() -> {
                                                        if (client.inGameHud != null) {
                                                            String sanitizedText = sanitizeForMinecraft(translatedText);
                                                            Text translatedMessage = Text.literal("[Translator] ")
                                                                    .formatted(Formatting.GREEN)
                                                                    .append(Text.literal(sanitizedText).formatted(Formatting.WHITE));

                                                            client.inGameHud.getChatHud().addMessage(translatedMessage);
                                                        }
                                                    });
                                                }).exceptionally(ex -> {
                                                    MinecraftClient.getInstance().execute(() -> {
                                                        MinecraftClient client = MinecraftClient.getInstance();
                                                        client.inGameHud.getChatHud().addMessage(Text.of("[Translator] §cTranslate Error: " + ex.getMessage()));
                                                    });
                                                    return null;
                                                });

                                        return 1;
                                    })
                            )
            );
        });
    }
}
