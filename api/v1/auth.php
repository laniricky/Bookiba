<?php
require __DIR__ . '/../../admin-web/db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: http://10.0.2.2');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { exit(0); }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'Method not allowed']);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true);
$action = $input['action'] ?? '';

// ── helpers ──────────────────────────────────────────────────────────────────

function generateToken(): string {
    return bin2hex(random_bytes(32));
}

// ── REGISTER ─────────────────────────────────────────────────────────────────
if ($action === 'register') {
    $name     = trim($input['name'] ?? '');
    $email    = strtolower(trim($input['email'] ?? ''));
    $password = $input['password'] ?? '';

    if (!$name || !$email || !$password) {
        http_response_code(400);
        echo json_encode(['ok' => false, 'error' => 'Name, email and password are required']);
        exit;
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        http_response_code(422);
        echo json_encode(['ok' => false, 'error' => 'Invalid email address']);
        exit;
    }
    if (strlen($password) < 6) {
        http_response_code(422);
        echo json_encode(['ok' => false, 'error' => 'Password must be at least 6 characters']);
        exit;
    }

    // Check duplicate
    $dup = $pdo->prepare("SELECT id FROM users WHERE email = ?");
    $dup->execute([$email]);
    if ($dup->fetch()) {
        http_response_code(409);
        echo json_encode(['ok' => false, 'error' => 'An account with this email already exists']);
        exit;
    }

    $hash  = password_hash($password, PASSWORD_BCRYPT);
    $token = generateToken();

    $stmt = $pdo->prepare(
        "INSERT INTO users (name, email, password_hash, auth_token, created_at) VALUES (?, ?, ?, ?, ?)"
    );
    $stmt->execute([$name, $email, $hash, $token, date('Y-m-d H:i:s')]);
    $userId = $pdo->lastInsertId();

    echo json_encode([
        'ok'    => true,
        'token' => $token,
        'user'  => ['id' => $userId, 'name' => $name, 'email' => $email]
    ]);
    exit;
}

// ── LOGIN ─────────────────────────────────────────────────────────────────────
if ($action === 'login') {
    $email    = strtolower(trim($input['email'] ?? ''));
    $password = $input['password'] ?? '';

    if (!$email || !$password) {
        http_response_code(400);
        echo json_encode(['ok' => false, 'error' => 'Email and password are required']);
        exit;
    }

    $stmt = $pdo->prepare("SELECT id, name, email, password_hash FROM users WHERE email = ?");
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password_hash'])) {
        http_response_code(401);
        echo json_encode(['ok' => false, 'error' => 'Invalid email or password']);
        exit;
    }

    // Rotate token on every login
    $token = generateToken();
    $pdo->prepare("UPDATE users SET auth_token = ? WHERE id = ?")->execute([$token, $user['id']]);

    echo json_encode([
        'ok'    => true,
        'token' => $token,
        'user'  => ['id' => $user['id'], 'name' => $user['name'], 'email' => $user['email']]
    ]);
    exit;
}

http_response_code(400);
echo json_encode(['ok' => false, 'error' => 'Unknown action. Use "login" or "register"']);
