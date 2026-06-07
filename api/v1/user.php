<?php
require __DIR__ . '/../../admin-web/db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: http://10.0.2.2');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { exit(0); }

// Authenticate
$authHeader = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
$token = '';
if (preg_match('/^Bearer\s+(.+)$/i', $authHeader, $m)) {
    $token = $m[1];
}

if (empty($token)) {
    http_response_code(401);
    echo json_encode(['ok' => false, 'error' => 'Authorisation required']);
    exit;
}

$stmt = $pdo->prepare("SELECT id, name, email FROM users WHERE auth_token = ?");
$stmt->execute([$token]);
$user = $stmt->fetch();

if (!$user) {
    http_response_code(401);
    echo json_encode(['ok' => false, 'error' => 'Invalid token']);
    exit;
}

// Fetch stats
$orderCount = $pdo->prepare("SELECT COUNT(*) FROM orders WHERE user_id = ?")->execute([$user['id']]) ? (int)$pdo->query("SELECT COUNT(*) FROM orders WHERE user_id = {$user['id']}")->fetchColumn() : 0;

echo json_encode([
    'ok' => true,
    'data' => [
        'name' => $user['name'],
        'email' => $user['email'],
        'ordersCount' => $orderCount,
        'wishlistCount' => 0, // Mock for now until wishlist table exists
        'reviewsCount' => 0
    ]
]);
