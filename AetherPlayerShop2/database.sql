-- ============================================
-- Script de création de la base de données
-- AetherPlayerShop
-- ============================================

-- Création de la base de données
CREATE DATABASE IF NOT EXISTS aetherplayershop 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- Sélection de la base
USE aetherplayershop;

-- Table des shops
CREATE TABLE IF NOT EXISTS shops (
    id VARCHAR(36) PRIMARY KEY,
    owner_uuid VARCHAR(36) NOT NULL,
    owner_name VARCHAR(32) NOT NULL,
    world VARCHAR(64) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    type VARCHAR(16) NOT NULL,
    price DOUBLE NOT NULL,
    stock INT NOT NULL,
    teleport_policy VARCHAR(32) NOT NULL DEFAULT 'ALLOW_TP',
    item_material VARCHAR(64) NOT NULL,
    item_data LONGBLOB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active TINYINT(1) NOT NULL DEFAULT 1,
    KEY idx_owner (owner_uuid),
    KEY idx_world_xyz (world, x, y, z),
    KEY idx_material (item_material),
    KEY idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table des transactions
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_uuid VARCHAR(36) NOT NULL,
    seller_uuid VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    item_key VARCHAR(128) NOT NULL,
    unit_price DOUBLE NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_buyer (buyer_uuid),
    KEY idx_seller (seller_uuid),
    KEY idx_item (item_key),
    KEY idx_created (created_at),
    CONSTRAINT fk_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Affichage des tables créées
SHOW TABLES;

-- Affichage de la structure
DESCRIBE shops;
DESCRIBE transactions;

-- Message de succès
SELECT '✅ Base de données AetherPlayerShop créée avec succès!' AS Status;
