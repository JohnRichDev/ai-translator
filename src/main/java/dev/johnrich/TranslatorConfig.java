package dev.johnrich;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class TranslatorConfig {
    private static final File CONFIG_FILE = new File("config/translator_config.json");

    public String commandLang = "not-set";
    public String chatTranslatorLang = "not-set";
    public String geminiApiKey = "your-gemini-api-key-here";
    public String geminiModelId = "gemini-2.0-flash-lite";

    public static TranslatorConfig load() {
        try {
            if (CONFIG_FILE.exists()) {
                return new Gson().fromJson(new FileReader(CONFIG_FILE), TranslatorConfig.class);
            } else {
                TranslatorConfig defaultConfig = new TranslatorConfig();
                defaultConfig.save();
                return defaultConfig;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new TranslatorConfig();
        }
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
