package dev.johnrich;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Formatting;

import java.util.List;

import static dev.johnrich.ChatTranslatorListener.sanitizeForMinecraft;

public class ChatScreen extends Screen {
    private int scroll = 0;

    public ChatScreen() {
        super(Text.literal("Translated Chat"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        int margin = 10;
        int y = margin - scroll;
        int maxWidth = this.width - margin * 2;

        for (ChatEntry entry : ChatTranslatorListener.messageCache.values()) {
            Text fullText = entry.getTranslated() == null
                    ? entry.getOriginal()
                    : Text.empty()
                    .append(entry.getOriginal())
                    .append(Text.literal(" → "))
                    .append(entry.getTranslated());

            List<OrderedText> wrapped = font.wrapLines(fullText, maxWidth);
            for (OrderedText line : wrapped) {
                if (y + font.fontHeight > 0 && y < this.height - margin) {
                    TextColor color = entry.getOriginal().getStyle().getColor();
                    int rgb = (color != null) ? color.getRgb() : 0xFFFFFF;
                    context.drawText(font, line, margin, y, rgb, false);
                }
                y += font.fontHeight + 2;
            }
            y += 4;
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(
                scroll - (int)(verticalAmount * 10),
                0,
                10000
        );
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer font = client.textRenderer;
            int margin = 10;
            int x1 = this.width - margin;
            int y = margin - scroll;
            int maxWidth = this.width - margin * 2;

            for (ChatEntry entry : ChatTranslatorListener.messageCache.values()) {
                Text fullText = entry.getTranslated() == null
                        ? entry.getOriginal()
                        : Text.empty()
                        .append(entry.getOriginal())
                        .append(Text.literal(" → "))
                        .append(entry.getTranslated());

                List<OrderedText> wrapped = font.wrapLines(fullText, maxWidth);
                int entryHeight = wrapped.size() * (font.fontHeight + 2) + 4;

                if (mouseX >= margin && mouseX <= x1 && mouseY >= y && mouseY <= y + entryHeight) {
                    if (entry.getTranslated() == null) {
                        String cleaned = entry.getOriginal().getString().replaceAll("<.*?>", "<>");

                        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));

                        ChatTranslatorListener.translationService
                                .translateChat(ChatTranslatorListener.config.chatTranslatorLang, cleaned)
                                .thenAccept(translated -> client.execute(() -> {
                                    String sanitizedText = sanitizeForMinecraft(translated);

                                    entry.setTranslated(Text.literal(sanitizedText).formatted(Formatting.WHITE));

                                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f));
                                }));
                    }
                    return true;
                }
                y += entryHeight;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
