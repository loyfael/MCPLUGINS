package loyfael.core.mongodb;

import com.mongodb.MongoQueryException;
import com.mongodb.MongoSecurityException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;

import java.net.ConnectException;
import java.net.UnknownHostException;

/**
 * Converts MongoDB driver exceptions into user-friendly French messages.
 */
public final class MongoExceptionHandler {

    private MongoExceptionHandler() {
    }

    public static String toUserMessage(Throwable throwable) {
        if (throwable == null) {
            return "Erreur inconnue";
        }

        Throwable root = unwrap(throwable);

        if (root instanceof MongoSecurityException) {
            return "Authentication failed";
        }
        if (root instanceof MongoTimeoutException) {
            return "Connection timed out";
        }
        if (root instanceof MongoSocketOpenException) {
            return resolveSocketOpenMessage((MongoSocketOpenException) root);
        }
        if (root instanceof MongoSocketException) {
            return resolveSocketMessage((MongoSocketException) root);
        }
        if (root instanceof UnknownHostException) {
            return "Host introuvable";
        }
        if (root instanceof ConnectException) {
            return "Connection refused";
        }
        if (root instanceof MongoQueryException queryException) {
            return resolveQueryMessage(queryException);
        }

        String message = root.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("authentication failed") || lower.contains("auth failed")
                || lower.contains("bad auth") || lower.contains("unauthorized")) {
                return "Authentication failed";
            }
            if (lower.contains("timed out") || lower.contains("timeout")) {
                return "Connection timed out";
            }
            if (lower.contains("connection refused")) {
                return "Connection refused";
            }
            if (lower.contains("unknown host") || lower.contains("nodename nor servname")) {
                return "Host introuvable";
            }
        }

        return root.getClass().getSimpleName();
    }

    public static void logFailure(Throwable throwable) {
        String cause = toUserMessage(throwable);
        MongoLogger.connectionFailed(cause);
        MongoLogger.debug("Détails de l'erreur MongoDB", throwable);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String resolveSocketOpenMessage(MongoSocketOpenException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof ConnectException) {
            return "Connection refused";
        }
        if (cause instanceof UnknownHostException) {
            return "Host introuvable";
        }
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("timed out")) {
            return "Connection timed out";
        }
        return "Connection refused";
    }

    private static String resolveSocketMessage(MongoSocketException exception) {
        Throwable cause = exception.getCause();
        if (cause != null) {
            return toUserMessage(cause);
        }
        return "Connection refused";
    }

    private static String resolveQueryMessage(MongoQueryException exception) {
        if (exception.getErrorCode() == 13 || exception.getErrorCode() == 18) {
            return "Authentication failed";
        }
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("not authorized")) {
            return "Authentication failed";
        }
        return "Erreur de requête MongoDB";
    }
}
