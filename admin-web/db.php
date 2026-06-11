<?php
$dbUrl = getenv('DATABASE_URL');
$isPostgres = false;

if ($dbUrl && strpos($dbUrl, 'postgres') === 0) {
    $isPostgres = true;
    $parsedUrl = parse_url($dbUrl);
    $host = $parsedUrl['host'];
    $port = isset($parsedUrl['port']) ? $parsedUrl['port'] : 5432;
    $user = $parsedUrl['user'];
    $pass = $parsedUrl['pass'];
    $db = ltrim($parsedUrl['path'], '/');
    $dsn = "pgsql:host=$host;port=$port;dbname=$db;user=$user;password=$pass";
} else {
    $dbPath = __DIR__ . '/../database.sqlite';
    $dsn = "sqlite:$dbPath";
}

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
try {
    $pdo->exec("ALTER TABLE users ADD COLUMN auth_token TEXT");
} catch (\Exception $e) { /* Ignore if exists */ }

try {
    $pdo->exec("ALTER TABLE users ADD COLUMN password_hash TEXT");
} catch (\Exception $e) { /* Ignore if exists */ }

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

// 4. banners table
$pdo->exec("
    CREATE TABLE IF NOT EXISTS banners (
        id          TEXT PRIMARY KEY,
        image_url   TEXT NOT NULL,
        title       TEXT,
        subtitle    TEXT,
        sort_order  INTEGER DEFAULT 0,
        is_active   INTEGER DEFAULT 1,
        created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
");

