<?php
require __DIR__ . '/../../admin-web/db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: http://10.0.2.2');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { exit(0); }

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'Method not allowed']);
    exit;
}

// ── Authenticate request ──────────────────────────────────────────────────────
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

$stmt = $pdo->prepare("SELECT id FROM users WHERE auth_token = ?");
$stmt->execute([$token]);
$authUser = $stmt->fetch();

if (!$authUser) {
    http_response_code(401);
    echo json_encode(['ok' => false, 'error' => 'Invalid or expired token']);
    exit;
}

$user_id = (int)$authUser['id'];

// ── Validate body ─────────────────────────────────────────────────────────────
$input = json_decode(file_get_contents('php://input'), true);

if (!$input || !isset($input['items']) || empty($input['items'])) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'Invalid order data']);
    exit;
}

try {
    $pdo->beginTransaction();

    $total_amount = 0;
    foreach ($input['items'] as $item) {
        $total_amount += ((float)$item['price'] * (int)$item['quantity']);
    }

    // Create order
    $stmt = $pdo->prepare("INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, 'Pending')");
    $stmt->execute([$user_id, $total_amount]);
    $order_id = $pdo->lastInsertId();

    // Insert line items + deduct inventory atomically
    $stmtItem      = $pdo->prepare("INSERT INTO order_items (order_id, book_id, quantity, price_ksh) VALUES (?, ?, ?, ?)");
    $stmtInventory = $pdo->prepare("UPDATE books SET inventory_count = MAX(0, inventory_count - ?) WHERE id = ?");

    foreach ($input['items'] as $item) {
        $bookId   = $item['book_id'];
        $qty      = (int)$item['quantity'];
        $price    = (float)$item['price'];

        // Guard: ensure book exists and has stock
        $stock = $pdo->prepare("SELECT inventory_count FROM books WHERE id = ?");
        $stock->execute([$bookId]);
        $row = $stock->fetch();
        if (!$row) {
            throw new Exception("Book $bookId not found");
        }
        if ((int)$row['inventory_count'] < $qty) {
            throw new Exception("Insufficient stock for book $bookId");
        }

        $stmtItem->execute([$order_id, $bookId, $qty, $price]);
        $stmtInventory->execute([$qty, $bookId]);
    }

    $pdo->commit();

    echo json_encode([
        'ok'       => true,
        'order_id' => $order_id,
        'message'  => 'Order placed successfully'
    ]);

} catch (Exception $e) {
    $pdo->rollBack();
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => $e->getMessage()]);
}
