package io.mudis.mudisserver.utils;

import java.io.IOException;
import java.util.Properties;

public class ConfigProperties {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (var input = ConfigProperties.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Properties file 'config.properties' not found in classpath.");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading properties file", e);
        }
    }

    public static String get(final String key) {
        return PROPERTIES.getProperty(key);
    }
}
