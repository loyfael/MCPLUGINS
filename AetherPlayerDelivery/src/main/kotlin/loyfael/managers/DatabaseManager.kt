package loyfael.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import loyfael.entities.*
import org.bukkit.Material
import java.sql.*
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

/**
 * Gestionnaire de la base de données avec support MySQL et SQLite
 * Utilise les coroutines Kotlin pour les opérations asynchrones
 */
class DatabaseManager(private val plugin: AetherPlayerDelivery) : CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    
    private lateinit var dataSource: HikariDataSource
    private val config = plugin.config
    private var databaseType: DatabaseType
    
    enum class DatabaseType {
        MYSQL, SQLITE
    }
    
    init {
        databaseType = when (config.getString("database.type", "sqlite")?.lowercase()) {
            "mysql" -> DatabaseType.MYSQL
            "sqlite" -> DatabaseType.SQLITE
            else -> {
                plugin.logger.warning("Type de base de données non reconnu, utilisation de SQLite par défaut")
                DatabaseType.SQLITE
            }
        }
    }
    
    /**
     * Initialise la connexion à la base de données avec fallback automatique
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            setupHikariDataSource()
            createTables()
            plugin.logger.info("§aDatabaseManager initialisé avec succès ! (Type: ${databaseType.name})")
        } catch (e: Exception) {
            if (databaseType == DatabaseType.MYSQL) {
                plugin.logger.warning("§eÉchec de la connexion MySQL, basculement vers SQLite...")
                plugin.logger.warning("§eErreur MySQL: ${e.message}")
                try {
                    databaseType = DatabaseType.SQLITE
                    if (::dataSource.isInitialized) {
                        dataSource.close()
                    }
                    setupHikariDataSource()
                    createTables()
                    plugin.logger.info("§aDatabaseManager initialisé avec succès en SQLite (fallback) !")
                } catch (sqliteException: Exception) {
                    plugin.logger.log(Level.SEVERE, "Erreur critique: impossible d'initialiser SQLite !", sqliteException)
                    throw sqliteException
                }
            } else {
                plugin.logger.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données SQLite !", e)
                throw e
            }
        }
    }
    
    /**
     * Configure HikariCP avec les paramètres du config.yml selon le type de base de données
     */
    private fun setupHikariDataSource() {
        val hikariConfig = HikariConfig().apply {
            when (databaseType) {
                DatabaseType.MYSQL -> {
                    jdbcUrl = "jdbc:mysql://${config.getString("database.mysql.host", "localhost")}:${config.getInt("database.mysql.port", 3306)}/${config.getString("database.mysql.database", "aetherdelivery")}?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"
                    username = config.getString("database.mysql.username", "delivery_user")
                    password = config.getString("database.mysql.password", "changeme")
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                }
                DatabaseType.SQLITE -> {
                    val dbFile = plugin.dataFolder.resolve(config.getString("database.sqlite.filename") ?: "aetherdelivery.db")
                    if (!plugin.dataFolder.exists()) {
                        plugin.dataFolder.mkdirs()
                    }
                    jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                    driverClassName = "org.sqlite.JDBC"
                    // SQLite n'a pas besoin d'username/password
                }
            }
            
            // Paramètres du pool
            maximumPoolSize = config.getInt("database.hikari.maximum-pool-size", 10)
            minimumIdle = config.getInt("database.hikari.minimum-idle", 3)
            connectionTimeout = config.getLong("database.hikari.connection-timeout", 30000)
            idleTimeout = config.getLong("database.hikari.idle-timeout", 600000)
            maxLifetime = config.getLong("database.hikari.max-lifetime", 1800000)
            
            // Optimisations selon le type de base de données
            when (databaseType) {
                DatabaseType.MYSQL -> {
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                    addDataSourceProperty("useServerPrepStmts", "true")
                    addDataSourceProperty("useLocalSessionState", "true")
                    addDataSourceProperty("rewriteBatchedStatements", "true")
                    addDataSourceProperty("cacheResultSetMetadata", "true")
                    addDataSourceProperty("cacheServerConfiguration", "true")
                    addDataSourceProperty("elideSetAutoCommits", "true")
                    addDataSourceProperty("maintainTimeStats", "false")
                }
                DatabaseType.SQLITE -> {
                    // Pour SQLite, limitons le pool à 1 connexion pour éviter les conflits
                    maximumPoolSize = 1
                    minimumIdle = 1
                }
            }
        }
        
        dataSource = HikariDataSource(hikariConfig)
    }
    
    /**
     * Crée les tables nécessaires si elles n'existent pas
     */
    private suspend fun createTables() = withContext(Dispatchers.IO) {
        when (databaseType) {
            DatabaseType.MYSQL -> createMySQLTables()
            DatabaseType.SQLITE -> createSQLiteTables()
        }
    }
    
    /**
     * Crée les tables pour MySQL avec support complet des types avancés
     */
    private fun createMySQLTables() {
        dataSource.connection.use { connection ->
            // Table des commandes
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS commandes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    uuid_client VARCHAR(36) NOT NULL,
                    nom_client VARCHAR(50) NOT NULL,
                    material VARCHAR(100) NOT NULL,
                    nom_item VARCHAR(100) NOT NULL,
                    quantite INT NOT NULL,
                    prix_total DECIMAL(10,2) NOT NULL,
                    statut ENUM('EN_ATTENTE', 'ACCEPTEE', 'PRETE', 'LIVREE', 'ANNULEE_CLIENT', 'ANNULEE_LIVREUR', 'EXPIREE', 'ERREUR_TECHNIQUE') NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    deadline TIMESTAMP NOT NULL,
                    description TEXT,
                    bonus DECIMAL(10,2) DEFAULT 0.00,
                    INDEX idx_client (uuid_client),
                    INDEX idx_statut (statut),
                    INDEX idx_deadline (deadline)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent()).executeUpdate()
            
            // Table des livraisons
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS livraisons (
                    id_commande BIGINT PRIMARY KEY,
                    uuid_livreur VARCHAR(36) NOT NULL,
                    nom_livreur VARCHAR(50) NOT NULL,
                    statut ENUM('ACCEPTEE', 'PRETE', 'LIVREE', 'ANNULEE_LIVREUR', 'EXPIREE') NOT NULL,
                    accepted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    delivered_at TIMESTAMP NULL,
                    delai_recuperation TIMESTAMP NOT NULL,
                    items_deposes BOOLEAN DEFAULT FALSE,
                    paiement_effectue BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (id_commande) REFERENCES commandes(id) ON DELETE CASCADE,
                    INDEX idx_livreur (uuid_livreur),
                    INDEX idx_statut_livraison (statut)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent()).executeUpdate()
            
            // Table de l'historique
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS historique (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type_evenement ENUM('COMMANDE_CREEE', 'COMMANDE_ACCEPTEE', 'COMMANDE_LIVREE', 'COMMANDE_RECUPEREE', 'COMMANDE_ANNULEE', 'COMMANDE_EXPIREE', 'REMBOURSEMENT_EFFECTUE', 'PAIEMENT_EFFECTUE') NOT NULL,
                    uuid_joueur VARCHAR(36) NOT NULL,
                    nom_joueur VARCHAR(50) NOT NULL,
                    id_commande BIGINT,
                    details TEXT NOT NULL,
                    montant DECIMAL(10,2),
                    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_joueur (uuid_joueur),
                    INDEX idx_type (type_evenement),
                    INDEX idx_commande (id_commande),
                    INDEX idx_date (date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent()).executeUpdate()
            
            // Table des réputations
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS reputation_joueurs (
                    uuid VARCHAR(36) PRIMARY KEY,
                    nom VARCHAR(50) NOT NULL,
                    reputation_livreur DECIMAL(3,2) DEFAULT 1.00,
                    total_livraisons_effectuees INT DEFAULT 0,
                    total_livraisons_echouees INT DEFAULT 0,
                    total_commandes_passees INT DEFAULT 0,
                    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent()).executeUpdate()
        }
    }
    
    /**
     * Crée les tables pour SQLite avec types compatibles
     */
    private fun createSQLiteTables() {
        dataSource.connection.use { connection ->
            // Active les contraintes de clés étrangères
            connection.prepareStatement("PRAGMA foreign_keys = ON").execute()
            
            // Table des commandes - utilise TEXT au lieu d'ENUM
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS commandes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid_client TEXT NOT NULL,
                    nom_client TEXT NOT NULL,
                    material TEXT NOT NULL,
                    nom_item TEXT NOT NULL,
                    quantite INTEGER NOT NULL,
                    prix_total REAL NOT NULL,
                    statut TEXT NOT NULL CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'PRETE', 'LIVREE', 'ANNULEE_CLIENT', 'ANNULEE_LIVREUR', 'EXPIREE', 'ERREUR_TECHNIQUE')),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    deadline DATETIME NOT NULL,
                    description TEXT,
                    bonus REAL DEFAULT 0.0
                )
            """.trimIndent()).executeUpdate()
            
            // Index pour SQLite
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_client ON commandes(uuid_client)").execute()
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_statut ON commandes(statut)").execute()
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_deadline ON commandes(deadline)").execute()
            
            // Table des livraisons
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS livraisons (
                    id_commande INTEGER PRIMARY KEY,
                    uuid_livreur TEXT NOT NULL,
                    nom_livreur TEXT NOT NULL,
                    statut TEXT NOT NULL CHECK (statut IN ('ACCEPTEE', 'PRETE', 'LIVREE', 'ANNULEE_LIVREUR', 'EXPIREE')),
                    accepted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    delivered_at DATETIME NULL,
                    delai_recuperation DATETIME NOT NULL,
                    items_deposes INTEGER DEFAULT 0,
                    paiement_effectue INTEGER DEFAULT 0,
                    FOREIGN KEY (id_commande) REFERENCES commandes(id) ON DELETE CASCADE
                )
            """.trimIndent()).executeUpdate()
            
            // Index pour les livraisons
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_livreur ON livraisons(uuid_livreur)").execute()
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_statut_livraison ON livraisons(statut)").execute()
            
            // Table de l'historique
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS historique (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type_evenement TEXT NOT NULL CHECK (type_evenement IN ('COMMANDE_CREEE', 'COMMANDE_ACCEPTEE', 'COMMANDE_LIVREE', 'COMMANDE_RECUPEREE', 'COMMANDE_ANNULEE', 'COMMANDE_EXPIREE', 'REMBOURSEMENT_EFFECTUE', 'PAIEMENT_EFFECTUE')),
                    uuid_joueur TEXT NOT NULL,
                    nom_joueur TEXT NOT NULL,
                    id_commande INTEGER,
                    details TEXT NOT NULL,
                    montant REAL,
                    date DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent()).executeUpdate()
            
            // Index pour l'historique
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_joueur ON historique(uuid_joueur)").execute()
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_type ON historique(type_evenement)").execute()
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_commande ON historique(id_commande)").execute()
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_date ON historique(date)").execute()
            
            // Table des réputations
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS reputation_joueurs (
                    uuid TEXT PRIMARY KEY,
                    nom TEXT NOT NULL,
                    reputation_livreur REAL DEFAULT 1.0,
                    total_livraisons_effectuees INTEGER DEFAULT 0,
                    total_livraisons_echouees INTEGER DEFAULT 0,
                    total_commandes_passees INTEGER DEFAULT 0,
                    last_update DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent()).executeUpdate()
        }
    }
    
    // === OPÉRATIONS CRUD POUR LES COMMANDES ===
    
    /**
     * Crée une nouvelle commande dans la base de données
     */
    suspend fun createCommande(commande: Commande): Long = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("""
                INSERT INTO commandes (uuid_client, nom_client, material, nom_item, quantite, prix_total, statut, deadline, description, bonus)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(), Statement.RETURN_GENERATED_KEYS)
            
            stmt.setString(1, commande.uuidClient.toString())
            stmt.setString(2, commande.nomClient)
            stmt.setString(3, commande.material.name)
            stmt.setString(4, commande.nomItem)
            stmt.setInt(5, commande.quantite)
            stmt.setDouble(6, commande.prixTotal)
            stmt.setString(7, commande.statut.name)
            stmt.setTimestamp(8, Timestamp.valueOf(commande.deadline))
            stmt.setString(9, commande.description)
            stmt.setDouble(10, commande.bonus)
            
            stmt.executeUpdate()
            
            val generatedKeys = stmt.generatedKeys
            if (generatedKeys.next()) {
                generatedKeys.getLong(1)
            } else {
                throw SQLException("Échec de la création de la commande, aucun ID généré")
            }
        }
    }
    
    /**
     * Récupère une commande par son ID
     */
    suspend fun getCommande(id: Long): Commande? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("SELECT * FROM commandes WHERE id = ?")
            stmt.setLong(1, id)
            
            val resultSet = stmt.executeQuery()
            if (resultSet.next()) {
                mapRowToCommande(resultSet)
            } else {
                null
            }
        }
    }
    
    /**
     * Récupère toutes les commandes d'un client
     */
    suspend fun getCommandesClient(uuidClient: UUID): List<Commande> = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("SELECT * FROM commandes WHERE uuid_client = ? ORDER BY created_at DESC")
            stmt.setString(1, uuidClient.toString())
            
            val resultSet = stmt.executeQuery()
            val commandes = mutableListOf<Commande>()
            
            while (resultSet.next()) {
                commandes.add(mapRowToCommande(resultSet))
            }
            
            commandes
        }
    }
    
    /**
     * Récupère toutes les commandes en attente
     */
    suspend fun getCommandesEnAttente(): List<Commande> = withContext(Dispatchers.IO) {
        val sql = when (databaseType) {
            DatabaseType.MYSQL -> "SELECT * FROM commandes WHERE statut = 'EN_ATTENTE' AND deadline > NOW() ORDER BY created_at ASC"
            DatabaseType.SQLITE -> "SELECT * FROM commandes WHERE statut = 'EN_ATTENTE' AND deadline > datetime('now') ORDER BY created_at ASC"
        }
        
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(sql)
            
            val resultSet = stmt.executeQuery()
            val commandes = mutableListOf<Commande>()
            
            while (resultSet.next()) {
                commandes.add(mapRowToCommande(resultSet))
            }
            
            commandes
        }
    }
    
    /**
     * Met à jour le statut d'une commande
     */
    suspend fun updateStatutCommande(id: Long, nouveauStatut: StatutCommande): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("UPDATE commandes SET statut = ? WHERE id = ?")
            stmt.setString(1, nouveauStatut.name)
            stmt.setLong(2, id)
            
            stmt.executeUpdate() > 0
        }
    }
    
    // === OPÉRATIONS CRUD POUR LES LIVRAISONS ===
    
    /**
     * Crée une nouvelle livraison
     */
    suspend fun createLivraison(livraison: Livraison): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            
            try {
                // Créer la livraison
                val stmtLivraison = connection.prepareStatement("""
                    INSERT INTO livraisons (id_commande, uuid_livreur, nom_livreur, statut, delai_recuperation)
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent())
                
                stmtLivraison.setLong(1, livraison.idCommande)
                stmtLivraison.setString(2, livraison.uuidLivreur.toString())
                stmtLivraison.setString(3, livraison.nomLivreur)
                stmtLivraison.setString(4, livraison.statut.name)
                stmtLivraison.setTimestamp(5, Timestamp.valueOf(livraison.delaiRecuperation))
                
                stmtLivraison.executeUpdate()
                
                // Mettre à jour le statut de la commande
                val stmtCommande = connection.prepareStatement("UPDATE commandes SET statut = 'ACCEPTEE' WHERE id = ?")
                stmtCommande.setLong(1, livraison.idCommande)
                stmtCommande.executeUpdate()
                
                connection.commit()
                true
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }
    
    /**
     * Récupère les livraisons d'un livreur
     */
    suspend fun getLivraisonsLivreur(uuidLivreur: UUID): List<Pair<Commande, Livraison>> = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("""
                SELECT c.*, l.* FROM commandes c
                INNER JOIN livraisons l ON c.id = l.id_commande
                WHERE l.uuid_livreur = ?
                ORDER BY l.accepted_at DESC
            """.trimIndent())
            
            stmt.setString(1, uuidLivreur.toString())
            val resultSet = stmt.executeQuery()
            val results = mutableListOf<Pair<Commande, Livraison>>()
            
            while (resultSet.next()) {
                val commande = mapRowToCommande(resultSet)
                val livraison = mapRowToLivraison(resultSet)
                results.add(Pair(commande, livraison))
            }
            
            results
        }
    }
    
    // === OPÉRATIONS POUR L'HISTORIQUE ===
    
    /**
     * Ajoute un événement à l'historique
     */
    suspend fun addHistoriqueEvenement(evenement: HistoriqueEvenement): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("""
                INSERT INTO historique (type_evenement, uuid_joueur, nom_joueur, id_commande, details, montant)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent())
            
            stmt.setString(1, evenement.typeEvenement.name)
            stmt.setString(2, evenement.uuidJoueur.toString())
            stmt.setString(3, evenement.nomJoueur)
            stmt.setObject(4, evenement.idCommande)
            stmt.setString(5, evenement.details)
            stmt.setObject(6, evenement.montant)
            
            stmt.executeUpdate() > 0
        }
    }
    
    // === OPÉRATIONS POUR LES RÉPUTATIONS ===
    
    /**
     * Met à jour la réputation d'un joueur
     */
    suspend fun updateReputation(reputation: ReputationJoueur): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("""
                INSERT INTO reputation_joueurs (uuid, nom, reputation_livreur, total_livraisons_effectuees, total_livraisons_echouees, total_commandes_passees)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                    nom = VALUES(nom),
                    reputation_livreur = VALUES(reputation_livreur),
                    total_livraisons_effectuees = VALUES(total_livraisons_effectuees),
                    total_livraisons_echouees = VALUES(total_livraisons_echouees),
                    total_commandes_passees = VALUES(total_commandes_passees)
            """.trimIndent())
            
            stmt.setString(1, reputation.uuid.toString())
            stmt.setString(2, reputation.nom)
            stmt.setDouble(3, reputation.reputationLivreur)
            stmt.setInt(4, reputation.totalLivraisonsEffectuees)
            stmt.setInt(5, reputation.totalLivraisonsEchouees)
            stmt.setInt(6, reputation.totalCommandesPassees)
            
            stmt.executeUpdate() > 0
        }
    }
    
    /**
     * Récupère les commandes expirées qui doivent être remboursées
     */
    suspend fun getCommandesExpirees(): List<Commande> = withContext(Dispatchers.IO) {
        val sql = when (databaseType) {
            DatabaseType.MYSQL -> """
                SELECT * FROM commandes 
                WHERE statut = 'EN_ATTENTE' AND deadline < NOW()
            """.trimIndent()
            DatabaseType.SQLITE -> """
                SELECT * FROM commandes 
                WHERE statut = 'EN_ATTENTE' AND deadline < datetime('now')
            """.trimIndent()
        }
        
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(sql)
            
            val resultSet = stmt.executeQuery()
            val commandes = mutableListOf<Commande>()
            
            while (resultSet.next()) {
                commandes.add(mapRowToCommande(resultSet))
            }
            
            commandes
        }
    }
    
    /**
     * Récupère les livraisons expirées (non récupérées)
     */
    suspend fun getLivraisonsExpirees(): List<Pair<Commande, Livraison>> = withContext(Dispatchers.IO) {
        val sql = when (databaseType) {
            DatabaseType.MYSQL -> """
                SELECT c.*, l.* FROM commandes c
                INNER JOIN livraisons l ON c.id = l.id_commande
                WHERE c.statut = 'PRETE' AND l.delai_recuperation < NOW()
            """.trimIndent()
            DatabaseType.SQLITE -> """
                SELECT c.*, l.* FROM commandes c
                INNER JOIN livraisons l ON c.id = l.id_commande
                WHERE c.statut = 'PRETE' AND l.delai_recuperation < datetime('now')
            """.trimIndent()
        }
        
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(sql)
            
            val resultSet = stmt.executeQuery()
            val results = mutableListOf<Pair<Commande, Livraison>>()
            
            while (resultSet.next()) {
                val commande = mapRowToCommande(resultSet)
                val livraison = mapRowToLivraison(resultSet)
                results.add(Pair(commande, livraison))
            }
            
            results
        }
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Mappe une ligne de ResultSet vers un objet Commande
     */
    private fun mapRowToCommande(rs: ResultSet): Commande {
        return Commande(
            id = rs.getLong("id"),
            uuidClient = UUID.fromString(rs.getString("uuid_client")),
            nomClient = rs.getString("nom_client"),
            material = Material.valueOf(rs.getString("material")),
            nomItem = rs.getString("nom_item"),
            quantite = rs.getInt("quantite"),
            prixTotal = rs.getDouble("prix_total"),
            statut = StatutCommande.valueOf(rs.getString("statut")),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            deadline = rs.getTimestamp("deadline").toLocalDateTime(),
            description = rs.getString("description"),
            bonus = rs.getDouble("bonus")
        )
    }
    
    /**
     * Mappe une ligne de ResultSet vers un objet Livraison
     */
    private fun mapRowToLivraison(rs: ResultSet): Livraison {
        return Livraison(
            idCommande = rs.getLong("id_commande"),
            uuidLivreur = UUID.fromString(rs.getString("uuid_livreur")),
            nomLivreur = rs.getString("nom_livreur"),
            statut = StatutCommande.valueOf(rs.getString("statut")),
            acceptedAt = rs.getTimestamp("accepted_at").toLocalDateTime(),
            deliveredAt = rs.getTimestamp("delivered_at")?.toLocalDateTime(),
            delaiRecuperation = rs.getTimestamp("delai_recuperation").toLocalDateTime(),
            itemsDeposes = rs.getBoolean("items_deposes"),
            paiementEffectue = rs.getBoolean("paiement_effectue")
        )
    }
    
    /**
     * Ferme les connexions et libère les ressources
     */
    fun shutdown() {
        job.cancel()
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
        plugin.logger.info("§cDatabaseManager fermé")
    }
    
    /**
     * Méthodes helper pour les différences SQL entre MySQL et SQLite
     */
    
    /**
     * Retourne la fonction NOW() appropriée selon le type de base de données
     */
    private fun nowFunction(): String {
        return when (databaseType) {
            DatabaseType.MYSQL -> "NOW()"
            DatabaseType.SQLITE -> "datetime('now', 'localtime')"
        }
    }
    
    /**
     * Retourne le type TIMESTAMP approprié selon le type de base de données
     */
    private fun timestampType(): String {
        return when (databaseType) {
            DatabaseType.MYSQL -> "TIMESTAMP"
            DatabaseType.SQLITE -> "TEXT"
        }
    }
    
    /**
     * Retourne le type BOOLEAN approprié selon le type de base de données
     */
    private fun booleanType(): String {
        return when (databaseType) {
            DatabaseType.MYSQL -> "BOOLEAN"
            DatabaseType.SQLITE -> "INTEGER"
        }
    }
    
    /**
     * Convertit une valeur booléenne pour insertion dans la base
     */
    private fun booleanValue(value: Boolean): Any {
        return when (databaseType) {
            DatabaseType.MYSQL -> value
            DatabaseType.SQLITE -> if (value) 1 else 0
        }
    }
    
    /**
     * Lit une valeur booléenne depuis la base de données
     */
    private fun getBoolean(rs: ResultSet, columnName: String): Boolean {
        return when (databaseType) {
            DatabaseType.MYSQL -> rs.getBoolean(columnName)
            DatabaseType.SQLITE -> rs.getInt(columnName) == 1
        }
    }
    
    /**
     * Convertit un LocalDateTime en valeur pour la base de données
     */
    private fun dateTimeValue(dateTime: LocalDateTime): Any {
        return when (databaseType) {
            DatabaseType.MYSQL -> Timestamp.valueOf(dateTime)
            DatabaseType.SQLITE -> dateTime.toString().replace('T', ' ')
        }
    }
    
    /**
     * Lit un LocalDateTime depuis la base de données
     */
    private fun getDateTime(rs: ResultSet, columnName: String): LocalDateTime? {
        return when (databaseType) {
            DatabaseType.MYSQL -> rs.getTimestamp(columnName)?.toLocalDateTime()
            DatabaseType.SQLITE -> {
                val dateStr = rs.getString(columnName)
                if (dateStr != null) {
                    LocalDateTime.parse(dateStr.replace(' ', 'T'))
                } else null
            }
        }
    }
    
    /**
     * Récupère les commandes expirées qui n'ont pas encore été traitées
     */
    suspend fun getExpiredOrders(): List<Commande> = withContext(Dispatchers.IO) {
        val commandes = mutableListOf<Commande>()
        val sql = when (databaseType) {
            DatabaseType.MYSQL -> """
                SELECT * FROM commandes 
                WHERE statut = 'EN_ATTENTE' AND deadline < NOW()
            """
            DatabaseType.SQLITE -> """
                SELECT * FROM commandes 
                WHERE statut = 'EN_ATTENTE' AND deadline < datetime('now')
            """
        }
        
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(sql)
            val resultSet = stmt.executeQuery()
            
            while (resultSet.next()) {
                commandes.add(mapRowToCommande(resultSet))
            }
        }
        
        commandes
    }
    
    /**
     * Récupère les commandes prêtes qui n'ont pas été récupérées
     */
    suspend fun getReadyOrdersNotCollected(): List<Commande> = withContext(Dispatchers.IO) {
        val commandes = mutableListOf<Commande>()
        val sql = """
            SELECT c.* FROM commandes c
            INNER JOIN livraisons l ON c.id = l.id_commande
            WHERE c.statut = 'PRETE' AND l.items_deposes = ${if (databaseType == DatabaseType.MYSQL) "TRUE" else "1"}
        """
        
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(sql)
            val resultSet = stmt.executeQuery()
            
            while (resultSet.next()) {
                commandes.add(mapRowToCommande(resultSet))
            }
        }
        
        commandes
    }
}
