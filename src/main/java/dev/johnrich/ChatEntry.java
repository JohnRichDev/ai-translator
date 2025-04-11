package dev.johnrich;

import net.minecraft.text.Text;

public class ChatEntry {
    private final Text original;
    private  Text translated;

    public ChatEntry(Text original) {
        this.original = original;
        this.translated = null;
    }

    public Text getOriginal() {
        return original;
    }

    public Text getTranslated() {
        return translated;
    }

    public void setTranslated(Text translated) {
        this.translated = translated;
    }
}
