package loyfael.core.mongodb;

import loyfael.utils.Utils;

/**
 * Centralized logging for all MongoDB-related messages.
 * Business messages are always shown; technical details only in debug mode.
 */
public final class MongoLogger {

    private static volatile boolean debug = false;

    private MongoLogger() {
    }

    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void info(String message) {
        Utils.sendConsoleLog(message);
    }

    public static void warn(String message) {
        Utils.sendConsoleLog("&e" + message);
    }

    public static void error(String message) {
        Utils.sendConsoleLog("&c" + message);
    }

    public static void debug(String message) {
        if (debug) {
            Utils.sendConsoleLog("&7[MongoDB Debug] " + message);
        }
    }

    public static void debug(String message, Throwable throwable) {
        if (debug && throwable != null) {
            Utils.sendConsoleLog("&7[MongoDB Debug] " + message + ": " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    public static void connectionFailed(String cause) {
        error("Impossible de se connecter à MongoDB.");
        error("Cause : " + cause);
    }
}
