package dev.johnrich;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TranslateCommand implements ClientModInitializer {
    private AITranslationService translationService;
    private TranslatorConfig config;

    @Override
    public void onInitializeClient() {
        translationService = new AITranslationService();
        config = TranslatorConfig.load();

        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if ("not-set".equals(config.commandLang)) {
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[T] §cTranslation language is not set! §eUse §a.setlang command <lang> §eto change."));
                return false;
            }

            if ("your-gemini-api-key-here".equals(config.geminiApiKey)) {
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[T] §cGemini API key is not set! §eUse §a.setkey <your-api-key> §eto set it."));
                return false;
            }

            config = TranslatorConfig.load();

            if (message.equals(".setlang") || message.startsWith(".setlang ")) {
                String[] parts = message.split(" ");
                if (parts.length != 3) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("[T] §cUsage: .setlang <command|chat|both> <lang>"));
                    return false;
                }

                String target = parts[1].toLowerCase();
                String lang = parts[2];

                switch (target) {
                    case "command" -> config.commandLang = lang;
                    case "chat" -> config.chatTranslatorLang = lang;
                    case "both" -> {
                        config.commandLang = lang;
                        config.chatTranslatorLang = lang;
                    }
                    default -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("[T] §cInvalid target. Use command/chat/both."));
                        return false;
                    }
                }

                config.save();
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[T] §aLanguage updated for: " + target));
                return false;
            }

            if (message.equals(".setkey") || message.startsWith(".setkey ")) {
                String[] parts = message.split(" ", 2);
                if (parts.length < 2 || parts[1].isBlank()) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.of("[T] §cUsage: .setkey <your-api-key>"));
                    return false;
                }

                config.geminiApiKey = parts[1].trim();
                config.save();

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.of("[T] §aGemini API key updated successfully."));
                return false;
            }

            if ((message.equals(".t") || message.equals(".translate")) || (message.startsWith(".t ") || message.startsWith(".translate "))) {
                String[] parts = message.split(" ", 2);
                if (parts.length < 2) return false;

                String textToTranslate = parts[1];
                String lang = config.commandLang;

                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[T] §eTranslating to §6" + lang + "§e..."));

                translationService.translate(lang, textToTranslate).thenAccept(translatedText -> MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        if (MinecraftClient.getInstance().isInSingleplayer()) {
                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                    .addMessage(Text.of("§a[T] §r" + translatedText));
                        } else {
                            String sanitizedText = translatedText.replaceAll("[^\\x20-\\x7E\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
                            MinecraftClient.getInstance().player.networkHandler
                                    .sendChatMessage(sanitizedText);
                        }
                    }
                }));
                return false;
            }

            if ((message.equals(".ta") || message.equals(".translateas")) || (message.startsWith(".ta ") || message.startsWith(".translateas "))) {
                String[] parts = message.split(" ", 3);
                if (parts.length < 3) return false;

                String textToTranslate = parts[2];
                String lang = parts[1];

                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[T] §eTranslating to §6" + lang + "§e..."));

                translationService.translate(lang, textToTranslate).thenAccept(translatedText -> MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        if (MinecraftClient.getInstance().isInSingleplayer()) {
                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                    .addMessage(Text.of("§a[T] §r" + translatedText));
                        } else {
                            String sanitizedText = translatedText.replaceAll("[^\\x20-\\x7E\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
                            MinecraftClient.getInstance().player.networkHandler
                                    .sendChatMessage(sanitizedText);
                        }
                    }
                }));
                return false;
            }

            return true;
        });
    }
}

