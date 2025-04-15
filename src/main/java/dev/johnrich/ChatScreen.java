package dev.johnrich;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartScroll = 0;
    private boolean autoScrollToBottom = true;

    public ChatScreen() {
        super(Text.literal("Translated Chat"));
    }

    @Override
    protected void applyBlur() {

    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 70;
        int buttonHeight = 20;
        int margin = 10;

        ButtonWidget settingsButton = ButtonWidget.builder(
                        Text.literal("Settings"),
                        btn -> {
                            assert this.client != null;
                            this.client.setScreen(new SettingsScreen(this, ChatTranslatorListener.config));
                        }
                )
                .dimensions(margin, this.height - buttonHeight - margin, buttonWidth, buttonHeight)
                .build();

        ButtonWidget scrollBottomButton = ButtonWidget.builder(
                        Text.literal("Bottom"),
                        btn -> {
                            scroll = maxScroll;
                            autoScrollToBottom = true;
                        }
                )
                .dimensions(margin + buttonWidth + 5, this.height - buttonHeight - margin, buttonWidth, buttonHeight)
                .build();

        this.addDrawableChild(settingsButton);
        this.addDrawableChild(scrollBottomButton);
    }

    @Override
    public void tick() {
        super.tick();
        if (autoScrollToBottom) {
            scroll = maxScroll;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        int margin = 10;
        int contentWidth = this.width - margin * 2 - 10;
        int contentHeight = 0;
        int y;

        for (ChatEntry entry : ChatTranslatorListener.messageCache.values()) {
            Text fullText = entry.getTranslated() == null ? entry.getOriginal() : Text.empty().append(entry.getOriginal()).append(Text.literal(" → ")).append(entry.getTranslated());

            List<OrderedText> wrapped = font.wrapLines(fullText, contentWidth);
            contentHeight += wrapped.size() * (font.fontHeight + 2) + 4;
        }

        int reservedBottomSpace = 40;
        maxScroll = Math.max(0, contentHeight - (this.height - margin * 2 - reservedBottomSpace));

        context.enableScissor(margin, margin, this.width - margin - 10, this.height - margin - reservedBottomSpace);

        y = margin - scroll;
        for (ChatEntry entry : ChatTranslatorListener.messageCache.values()) {
            Text fullText = entry.getTranslated() == null ? entry.getOriginal() : Text.empty().append(entry.getOriginal()).append(Text.literal(" → ")).append(entry.getTranslated());

            List<OrderedText> wrapped = font.wrapLines(fullText, contentWidth);
            for (OrderedText line : wrapped) {
                if (y + font.fontHeight > margin && y < this.height - margin - reservedBottomSpace + margin) {
                    TextColor color = entry.getOriginal().getStyle().getColor();
                    int rgb = (color != null) ? color.getRgb() : 0xFFFFFF;
                    context.drawText(font, line, margin, y, rgb, true);
                }
                y += font.fontHeight + 2;
            }
            y += 4;
        }

        context.disableScissor();

        if (maxScroll > 0) {
            int scrollbarHeight = this.height - margin * 2 - reservedBottomSpace;
            float scrollbarThumbHeight = Math.max(30, scrollbarHeight * (this.height - margin * 2) / (float) (contentHeight));
            int scrollbarThumbY = margin + (int) ((scrollbarHeight - scrollbarThumbHeight) * (scroll / (float) maxScroll));

            context.fill(this.width - margin - 8, margin, this.width - margin, this.height - margin - reservedBottomSpace, 0x33FFFFFF);

            context.fill(this.width - margin - 8, scrollbarThumbY, this.width - margin, scrollbarThumbY + (int) scrollbarThumbHeight, mouseX >= this.width - margin - 8 && mouseX <= this.width - margin && mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight ? 0xFFAAAAAA : 0xFF888888);
        }

        String scrollInfo = scroll + "/" + maxScroll;
        int scrollInfoWidth = font.getWidth(scrollInfo);
        context.drawText(font, scrollInfo, this.width - margin - 8 - scrollInfoWidth - 5, this.height - margin - reservedBottomSpace + 5, 0xCCCCCC, true);

        String autoScrollText = "Auto: " + (autoScrollToBottom ? "ON" : "OFF");
        context.drawText(font, autoScrollText, this.width - margin - font.getWidth(autoScrollText) - 10, this.height - margin - 15, autoScrollToBottom ? 0x55FF55 : 0xFFAAAA, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int oldScroll = scroll;
        scroll = MathHelper.clamp(scroll - (int) (verticalAmount * 20), 0, maxScroll);

        if (scroll < oldScroll && autoScrollToBottom) {
            autoScrollToBottom = false;
        }

        if (scroll >= maxScroll && !autoScrollToBottom) {
            autoScrollToBottom = true;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) {
            if (maxScroll > 0) {
                int margin = 10;
                int reservedBottomSpace = 40;
                int scrollbarHeight = this.height - margin * 2 - reservedBottomSpace;
                float scrollbarThumbHeight = Math.max(30, scrollbarHeight * (this.height - margin * 2) / (float) (scrollbarHeight + maxScroll));
                int scrollbarThumbY = margin + (int) ((scrollbarHeight - scrollbarThumbHeight) * (scroll / (float) maxScroll));

                if (mouseX >= this.width - margin - 8 && mouseX <= this.width - margin && mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight) {
                    isDraggingScrollbar = true;
                    dragStartY = (int) mouseY;
                    dragStartScroll = scroll;
                    return true;
                }

                if (mouseX >= this.width - margin - 8 && mouseX <= this.width - margin && mouseY <= this.height - margin - reservedBottomSpace) {
                    float percentPosition = (float) (mouseY - margin) / (this.height - margin * 2 - reservedBottomSpace);
                    scroll = (int) (maxScroll * percentPosition);
                    scroll = MathHelper.clamp(scroll, 0, maxScroll);

                    autoScrollToBottom = (scroll >= maxScroll);
                    return true;
                }
            }

            int reservedBottomSpace = 40;
            if (mouseY < this.height - reservedBottomSpace) {
                MinecraftClient client = MinecraftClient.getInstance();
                TextRenderer font = client.textRenderer;
                int margin = 10;
                int contentWidth = this.width - margin * 2 - 10;
                int y = margin - scroll;

                for (ChatEntry entry : ChatTranslatorListener.messageCache.values()) {
                    Text fullText = entry.getTranslated() == null ? entry.getOriginal() : Text.empty().append(entry.getOriginal()).append(Text.literal(" → ")).append(entry.getTranslated());

                    List<OrderedText> wrapped = font.wrapLines(fullText, contentWidth);
                    int entryHeight = wrapped.size() * (font.fontHeight + 2) + 4;

                    if (mouseX >= margin && mouseX <= this.width - margin - 10 && mouseY >= y && mouseY <= y + entryHeight) {
                        if (entry.getTranslated() == null) {
                            String cleaned = entry.getOriginal().getString().replaceAll("<.*?>", "<>");

                            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));

                            ChatTranslatorListener.translationService.translateChat(ChatTranslatorListener.config.chatTranslatorLang, cleaned).thenAccept(translated -> client.execute(() -> {
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
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && button == 0) {
            int margin = 10;
            int reservedBottomSpace = 40;
            int scrollbarHeight = this.height - margin * 2 - reservedBottomSpace;
            float dragPercentage = (float) ((mouseY - dragStartY) / scrollbarHeight);
            int scrollDelta = (int) (dragPercentage * maxScroll);
            scroll = MathHelper.clamp(dragStartScroll + scrollDelta, 0, maxScroll);

            autoScrollToBottom = (scroll >= maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}