package dev.johnrich;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;

public class TranslatorModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TranslatorConfig config = TranslatorConfig.load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if ("not-set".equalsIgnoreCase(config.commandLang) || "not-set".equalsIgnoreCase(config.chatTranslatorLang)) {
                client.execute(() -> client.inGameHud.getChatHud().addMessage(
                        Text.of("§6[T] §cLanguage is not set! Use §a.setlang <command/chat/both> <lang> §eto change it.")
                ));
            }

            if ("your-gemini-api-key-here".equals(config.geminiApiKey)) {
                client.execute(() -> client.inGameHud.getChatHud().addMessage(
                        Text.of("§6[T] §cGemini API key not set! Edit §7.minecraft/config/translator_config.json §cin config folder or use §7.setkey <key>§c!")
                ));
            }
        });
    }
}
