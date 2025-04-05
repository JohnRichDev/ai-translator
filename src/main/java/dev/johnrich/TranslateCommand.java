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
                        .addMessage(Text.of("[Translator] §cTranslation language is not set! §eUse §a.setlang command <lang> §eto change."));
                return false;
            }

            if ("your-gemini-api-key-here".equals(config.geminiApiKey)) {
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[Translator] §cGemini API key is not set! §eUse §a.setkey <your-api-key> §eto set it."));
                return false;
            }

            if (message.equals(".setlang") || message.startsWith(".setlang")) {
                String[] parts = message.split(" ");
                if (parts.length != 3) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("[Translator] §cUsage: .setlang <command|manual|both> <lang>"));
                    return false;
                }

                String target = parts[1].toLowerCase();
                String lang = parts[2];

                switch (target) {
                    case "command" -> config.commandLang = lang;
                    case "manual" -> config.chatTranslatorLang = lang;
                    case "both" -> {
                        config.commandLang = lang;
                        config.chatTranslatorLang = lang;
                    }
                    default -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("[Translator] §cInvalid target. Use command/manual/both."));
                        return false;
                    }
                }

                config.save();
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[Translator] §aLanguage updated for: " + target));
                return false;
            }

            if (message.equals(".setkey") || message.startsWith(".setkey ")) {
                String[] parts = message.split(" ", 2);
                if (parts.length < 2 || parts[1].isBlank()) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.of("[Translator] §cUsage: .setkey <your-api-key>"));
                    return false;
                }

                config.geminiApiKey = parts[1].trim();
                config.save();

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.of("[Translator] §aGemini API key updated successfully."));
                return false;
            }

            if ((message.equals(".t") || message.equals(".translate")) || (message.startsWith(".t ") || message.startsWith(".translate "))) {
                if ("not-set".equals(config.commandLang)) {
                    MinecraftClient.getInstance().inGameHud.getChatHud()
                            .addMessage(Text.of("[Translator] §cTranslation language is not set! §eUse §a.setlang command <lang> §eto change."));
                    return false;
                }
                String[] parts = message.split(" ", 2);
                if (parts.length < 2) return false;

                String textToTranslate = parts[1];
                String lang = config.commandLang;

                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[Translator] §eTranslating to &6" + lang + "&e..."));

                translationService.translate(lang, textToTranslate).thenAccept(translatedText -> {
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            if (MinecraftClient.getInstance().isInSingleplayer()) {
                                MinecraftClient.getInstance().inGameHud.getChatHud()
                                        .addMessage(Text.of("§a[Translator] §r" + translatedText));
                            } else {
                                String sanitizedText = translatedText.replaceAll("[^\\x20-\\x7E\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
                                MinecraftClient.getInstance().player.networkHandler
                                        .sendChatMessage(sanitizedText);
                            }
                        }
                    });
                });
                return false;
            }

            return true;
        });
    }
}

