package dev.johnrich;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatTranslatorListener implements ClientModInitializer {
    private AITranslationService translationService;
    private final Map<UUID, String> messageCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, String> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private static final int MAX_CACHE_SIZE = 100;
    private TranslatorConfig config;

    private static final Pattern TEXT_CONTENT_PATTERN = Pattern.compile("[\\p{L}\\p{N}\\p{P}]+");

    private String sanitizeForMinecraft(String input) {
        return input.replaceAll("[^\\x20-\\x7E\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
    }

    private boolean isValidForTranslation(String message) {
        return message != null && !message.trim().isEmpty() && TEXT_CONTENT_PATTERN.matcher(message).find();
    }

    @Override
    public void onInitializeClient() {
        translationService = new AITranslationService();
        config = TranslatorConfig.load();

        System.out.println("[T] Translator listener initialized");

        // Intercept all incoming messages.
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            String originalMessage = message.getString();
            if (isValidForTranslation(originalMessage)) {
                UUID messageId = UUID.randomUUID();
                messageCache.put(messageId, originalMessage);
                if (messageCache.size() > MAX_CACHE_SIZE) {
                    messageCache.remove(messageCache.keySet().iterator().next());
                }

                MutableText translateButton = Text.literal(" [T]")
                        .setStyle(Style.EMPTY
                                .withColor(Formatting.GREEN)
                                .withClickEvent(new ClickEvent.RunCommand("/translate-message " + messageId))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to translate this message"))));
                MutableText modifiedMessage = Text.empty().append(message).append(translateButton);
                client.inGameHud.getChatHud().addMessage(modifiedMessage);

            } else {
                client.inGameHud.getChatHud().addMessage(message);
            }
            return false;
        });

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            String originalMessage = message.getString();
            if (isValidForTranslation(originalMessage)) {
                UUID messageId = UUID.randomUUID();

                messageCache.put(messageId, originalMessage);

                MutableText translateButton = Text.literal(" [T]")
                        .setStyle(Style.EMPTY
                                .withColor(Formatting.GREEN)
                                .withClickEvent(new ClickEvent.RunCommand("/translate-message " + messageId))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to translate this message"))));

                return Text.empty().append(message).append(translateButton);
            } else {
                return message;
            }
        });

        // Register the translation command.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("translate-message")
                        .then(ClientCommandManager.argument("messageId", UuidArgumentType.uuid())
                                .executes(context -> {

                                    if ("your-gemini-api-key-here".equals(config.geminiApiKey)) {
                                        MinecraftClient.getInstance().inGameHud.getChatHud()
                                                .addMessage(Text.of("[T] §cGemini API key is not set! §eUse §a.setkey <your-api-key> §eto set it."));
                                        return 1;
                                    }

                                    if ("not-set".equals(config.chatTranslatorLang)) {
                                        MinecraftClient.getInstance().inGameHud.getChatHud()
                                                .addMessage(Text.of("[T] §cTranslation language is not set! §eUse §a.setlang command <lang> §eto change."));
                                        return 1;
                                    }

                                    UUID messageId = context.getArgument("messageId", UUID.class);
                                    String originalMessage = messageCache.get(messageId);

                                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));

                                    if (originalMessage == null) {
                                        MinecraftClient client = MinecraftClient.getInstance();
                                        client.inGameHud.getChatHud().addMessage(Text.of("[T] §cTranslate Error: Message not found in cache"));
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
                                                        Text translatedMessage = Text.literal("[T] ")
                                                                .formatted(Formatting.GREEN)
                                                                .append(Text.literal(sanitizedText).formatted(Formatting.WHITE));

                                                        client.inGameHud.getChatHud().addMessage(translatedMessage);
                                                    }
                                                });
                                            }).exceptionally(ex -> {
                                                MinecraftClient.getInstance().execute(() -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    client.inGameHud.getChatHud().addMessage(Text.of("[T] §cTranslate Error: " + ex.getMessage()));
                                                });
                                                return null;
                                            });

                                    return 1;
                                })
                        )
        ));
    }
}
