package loyfael.core.mongodb;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configures MongoDB driver logging so it only appears in debug mode.
 * Handles both development (org.mongodb) and shaded (loyfael.libs.mongodb) class names.
 */
public final class MongoDriverLogging {

    private static final String[] LOGGER_NAMES = {
        "org.mongodb",
        "org.mongodb.driver",
        "com.mongodb",
        "com.mongodb.driver",
        "loyfael.libs.mongodb",
        "loyfael.libs.mongodb.driver"
    };

    private static final String[] SLF4J_LOGGER_KEYS = {
        "org.slf4j.simpleLogger.log.org.mongodb",
        "org.slf4j.simpleLogger.log.org.mongodb.driver",
        "org.slf4j.simpleLogger.log.com.mongodb",
        "org.slf4j.simpleLogger.log.loyfael.libs.mongodb",
        "org.slf4j.simpleLogger.log.loyfael.libs.mongodb.driver"
    };

    private MongoDriverLogging() {
    }

    public static void configure(boolean debug) {
        Level julLevel = debug ? Level.INFO : Level.OFF;
        String slf4jLevel = debug ? "info" : "off";

        for (String loggerName : LOGGER_NAMES) {
            Logger.getLogger(loggerName).setLevel(julLevel);
        }

        for (String propertyKey : SLF4J_LOGGER_KEYS) {
            System.setProperty(propertyKey, slf4jLevel);
        }

        MongoLogger.debug("Journalisation du driver MongoDB : " + (debug ? "activée" : "désactivée"));
    }
}
