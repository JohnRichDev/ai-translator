package dev.johnrich;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private final TranslatorConfig config;

    private TextFieldWidget apiKeyField;
    private TextFieldWidget chatLangField;
    private TextFieldWidget commandLangField;

    public SettingsScreen(Screen parent, TranslatorConfig config) {
        super(Text.literal("Translator Settings"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        int midX = this.width / 2;
        int midY = this.height / 2;
        int fieldWidth = 250;
        int fieldHeight = 20;
        int y = midY - 50;

        apiKeyField = new TextFieldWidget(this.textRenderer, midX - fieldWidth / 2, y, fieldWidth, fieldHeight, Text.literal("API Key"));
        apiKeyField.setMaxLength(128);
        apiKeyField.setText(config.geminiApiKey);
        this.addDrawableChild(apiKeyField);

        y += 30;
        chatLangField = new TextFieldWidget(this.textRenderer, midX - fieldWidth / 2, y, fieldWidth, fieldHeight, Text.literal("Chat Language"));
        apiKeyField.setMaxLength(128);
        chatLangField.setText(config.chatTranslatorLang);
        this.addDrawableChild(chatLangField);

        y += 30;
        commandLangField = new TextFieldWidget(this.textRenderer, midX - fieldWidth / 2, y, fieldWidth, fieldHeight, Text.literal("Command Language"));
        apiKeyField.setMaxLength(128);
        commandLangField.setText(config.commandLang);
        this.addDrawableChild(commandLangField);

        y += 40;

        int buttonWidth = 80;
        int spacing = 10;
        int totalWidth = buttonWidth * 2 + spacing;
        int startX = midX - totalWidth / 2;

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Save"), btn -> {
                            config.geminiApiKey = apiKeyField.getText();
                            config.chatTranslatorLang = chatLangField.getText();
                            config.commandLang = commandLangField.getText();
                            config.save();
                            assert this.client != null;
                            this.client.setScreen(parent);
                        })
                        .dimensions(startX, y, buttonWidth, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), btn -> {
                            assert this.client != null;
                            this.client.setScreen(parent);
                        })
                        .dimensions(startX + buttonWidth + spacing, y, buttonWidth, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        int midX = this.width / 2;
        int labelX = midX - 255;
        int textY = (this.height / 2) - 50;

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Gemini API Key:"), labelX, textY + 5, 0xA0A0A0);
        textY += 30;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Chat Language:"), labelX, textY + 5, 0xA0A0A0);
        textY += 30;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Command Language:"), labelX, textY + 5, 0xA0A0A0);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(parent);
    }
}
