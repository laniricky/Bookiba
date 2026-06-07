<?php
$dbPath = __DIR__ . '/../database.sqlite';
$dsn = "sqlite:$dbPath";
$options = [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
];

try {
    $pdo = new PDO($dsn, null, null, $options);
} catch (\PDOException $e) {
    throw new \PDOException($e->getMessage(), (int)$e->getCode());
}

// ── Auto-migrations (idempotent) ─────────────────────────────────────────────

// 1. auth_token column on users
$cols = $pdo->query("PRAGMA table_info(users)")->fetchAll(PDO::FETCH_COLUMN, 1);
if (!in_array('auth_token', $cols)) {
    $pdo->exec("ALTER TABLE users ADD COLUMN auth_token TEXT");
}
if (!in_array('password_hash', $cols)) {
    $pdo->exec("ALTER TABLE users ADD COLUMN password_hash TEXT");
}

// 2. store_settings key-value table
$pdo->exec("
    CREATE TABLE IF NOT EXISTS store_settings (
        key   TEXT PRIMARY KEY,
        value TEXT
    )
");

// 3. admin_sessions table
$pdo->exec("
    CREATE TABLE IF NOT EXISTS admin_sessions (
        token      TEXT PRIMARY KEY,
        expires_at INTEGER
    )
");

