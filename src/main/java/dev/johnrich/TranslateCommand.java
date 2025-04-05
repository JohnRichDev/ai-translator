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

        MinecraftClient.getInstance().execute(() -> {
            if ("en".equals(config.commandLang)) {
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[Translator] §eUsing default translation language (English). Use §a.setlang command <lang> §eto change."));
            }
        });

        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (message.startsWith(".setlang")) {
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

            if (message.startsWith(".t ") || message.startsWith(".translate ")) {
                String[] parts = message.split(" ", 2);
                if (parts.length < 2) return false;

                String textToTranslate = parts[1];
                String lang = config.commandLang;

                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.of("[Translator] §7Translating to " + lang + "..."));

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

