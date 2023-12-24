package com.example.warehouse;

import java.io.*;
import java.util.Properties;

public class AppSettings {
    private static final String FILE_NAME = "app.properties";

    public static void saveSetting(String key, String value) {
        try (OutputStream output = new FileOutputStream(FILE_NAME)) {
            Properties prop = new Properties();
            prop.setProperty(key, value);
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static String loadSetting(String key) {
        try (InputStream input = new FileInputStream(FILE_NAME)) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty(key);
        } catch (IOException | NullPointerException io) {
            io.printStackTrace();
            return null;
        }
    }
}
