package loyfael.core.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;
import loyfael.api.interfaces.IConfigurationService;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Immutable MongoDB configuration loaded from config.yml.
 * Supports URI mode (recommended) or discrete host/port/credentials mode — never both.
 */
public final class MongoConfig {

    public enum Mode {
        URI,
        DISCRETE
    }

    private final Mode mode;
    private final boolean debug;
    private final int connectTimeoutMs;
    private final int serverSelectionTimeoutMs;

    private final String uri;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String authSource;

    private MongoConfig(Mode mode, boolean debug, int connectTimeoutMs, int serverSelectionTimeoutMs,
                        String uri, String host, int port, String database,
                        String username, String password, String authSource) {
        this.mode = mode;
        this.debug = debug;
        this.connectTimeoutMs = connectTimeoutMs;
        this.serverSelectionTimeoutMs = serverSelectionTimeoutMs;
        this.uri = uri;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.authSource = authSource;
    }

    public static MongoConfig from(IConfigurationService configService) {
        FileConfiguration config = configService.getConfig();

        boolean debug = config.getBoolean("debug",
            config.getBoolean("debug.enabled",
                config.getBoolean("system.debug", false)));

        int connectTimeout = config.getInt("mongodb.connect-timeout",
            config.getInt("mongodb.connection.timeout", 5000));
        int serverSelectionTimeout = config.getInt("mongodb.server-selection-timeout", connectTimeout);

        String uri = nullToEmpty(config.getString("mongodb.uri")).trim();

        if (!uri.isEmpty()) {
            warnIfDiscreteFieldsPresent(config);
            return fromUri(uri, debug, connectTimeout, serverSelectionTimeout);
        }

        return fromDiscrete(config, debug, connectTimeout, serverSelectionTimeout);
    }

    private static MongoConfig fromUri(String uri, boolean debug, int connectTimeout, int serverSelectionTimeout) {
        ConnectionString connectionString = new ConnectionString(uri);
        String database = connectionString.getDatabase();
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException(
                "L'URI MongoDB doit contenir un nom de base de données (ex: mongodb://host:27017/nuvalis)");
        }

        return new MongoConfig(Mode.URI, debug, connectTimeout, serverSelectionTimeout,
            uri, null, 0, database, null, null, null);
    }

    private static MongoConfig fromDiscrete(FileConfiguration config, boolean debug,
                                            int connectTimeout, int serverSelectionTimeout) {
        String host = config.getString("mongodb.host", "localhost");
        int port = config.getInt("mongodb.port", 27017);
        String database = config.getString("mongodb.database", "krakenlevels");
        String username = nullToEmpty(config.getString("mongodb.username"));
        String password = nullToEmpty(config.getString("mongodb.password"));
        String authSource = config.getString("mongodb.auth-source", "admin");

        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("mongodb.database ne peut pas être vide");
        }

        return new MongoConfig(Mode.DISCRETE, debug, connectTimeout, serverSelectionTimeout,
            null, host, port, database, username, password, authSource);
    }

    private static void warnIfDiscreteFieldsPresent(FileConfiguration config) {
        boolean hasDiscreteOverrides =
            !nullToEmpty(config.getString("mongodb.username")).isEmpty()
                || !nullToEmpty(config.getString("mongodb.password")).isEmpty()
                || !"localhost".equals(config.getString("mongodb.host", "localhost"))
                || config.getInt("mongodb.port", 27017) != 27017;

        if (hasDiscreteOverrides) {
            MongoLogger.debug(
                "mongodb.uri est défini : les paramètres host/port/username/password sont ignorés.");
        }
    }

    /**
     * Builds the connection string used by the driver (without credentials in discrete auth mode).
     */
    public ConnectionString toConnectionString() {
        if (mode == Mode.URI) {
            return new ConnectionString(appendTimeoutParams(uri));
        }

        String baseUri = String.format("mongodb://%s:%d/%s", host, port, database);
        return new ConnectionString(appendTimeoutParams(baseUri));
    }

    /**
     * Returns explicit credentials for discrete mode, or null when authentication is not configured.
     */
    public MongoCredential getCredential() {
        if (mode == Mode.URI) {
            return toConnectionString().getCredential();
        }
        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }
        return MongoCredential.createCredential(username, authSource, password.toCharArray());
    }

    public boolean hasAuthentication() {
        return getCredential() != null;
    }

    public String getDatabaseName() {
        return database;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getServerSelectionTimeoutMs() {
        return serverSelectionTimeoutMs;
    }

    /**
     * Human-readable connection target for success logs.
     */
    public String getConnectionLabel() {
        if (mode == Mode.URI) {
            ConnectionString cs = toConnectionString();
            List<String> hosts = cs.getHosts();
            String hostLabel = hosts.isEmpty() ? "unknown" : hosts.get(0);
            return hostLabel + " / " + database;
        }
        return host + ":" + port + " / " + database;
    }

    private String appendTimeoutParams(String rawUri) {
        String separator = rawUri.contains("?") ? "&" : "?";
        return rawUri + separator
            + "connectTimeoutMS=" + connectTimeoutMs
            + "&serverSelectionTimeoutMS=" + serverSelectionTimeoutMs;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * URL-encodes credentials for URI construction (discrete mode fallback only).
     */
    static String encodeUriComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
