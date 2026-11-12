package com.trcm.qa.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads settings for the current environment (local / dev / cert).
 *
 * Lookup order for every key (first non-empty value wins):
 *   1. JVM system property      -Dbrowser=firefox
 *   2. Environment variable     BROWSER=firefox  (dots -> underscores, upper case)
 *   3. config/{env}.properties  on the classpath
 *
 * This is how CI injects secrets without committing them: Jenkins credentials or
 * Azure DevOps secret variables become environment variables like APIM_SUBSCRIPTION_KEY.
 */
public final class ConfigReader {

    private static final Logger LOG = LogManager.getLogger(ConfigReader.class);
    private static final Properties PROPS = new Properties();
    private static final String ENV;

    static {
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) {
            env = "local";
        }
        ENV = env.trim().toLowerCase();
        String file = "config/" + ENV + ".properties";
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) {
                throw new IllegalStateException("Config file not found on classpath: " + file);
            }
            PROPS.load(in);
            LOG.info("Loaded configuration for environment '{}'", ENV);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
    }

    private ConfigReader() {
    }

    public static String env() {
        return ENV;
    }

    public static String get(String key) {
        String value = System.getProperty(key);
        if (isUsable(value)) {
            return value.trim();
        }
        value = System.getenv(key.replace('.', '_').toUpperCase());
        if (isUsable(value)) {
            return value.trim();
        }
        value = PROPS.getProperty(key);
        if (isUsable(value)) {
            return value.trim();
        }
        throw new IllegalStateException("Missing config value for '" + key + "' in env '" + ENV + "'");
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Azure DevOps leaves an undefined variable as the literal text "$(name)".
     * Treat that as "not set" so we fall back to the properties file.
     */
    private static boolean isUsable(String value) {
        return value != null && !value.isBlank() && !value.trim().startsWith("$(");
    }
}
