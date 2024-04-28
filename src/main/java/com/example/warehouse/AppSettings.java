package com.example.warehouse;

import java.io.*;
import java.util.Properties;

public class AppSettings {
    private static final String FILE_NAME = "app.properties";
    private static AppSettings instance;
    public String server;
    public int quantityDecimals;
    public int priceDecimals;
    public int totalDecimals;
    private AppSettings() {
        init();
    }

    public static synchronized AppSettings getInstance() {
        return new AppSettings();
    }

    private void init() {
        server = AppSettings.loadSetting("server");
        quantityDecimals = Integer.parseInt(AppSettings.loadSetting("QuantityDecimals"));
        priceDecimals = Integer.parseInt(AppSettings.loadSetting("PriceDecimals"));
        totalDecimals = Integer.parseInt(AppSettings.loadSetting("TotalDecimals"));
    }
    public static void saveSetting(String key, String value) {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(FILE_NAME)) {
            prop.load(input);
        } catch (IOException ignored) {}

        try (OutputStream output = new FileOutputStream(FILE_NAME)) {
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
