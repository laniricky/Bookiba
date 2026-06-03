<?php
require __DIR__ . '/../../admin-web/db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'Method not allowed']);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true);

if (!$input || !isset($input['items']) || empty($input['items'])) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'Invalid order data']);
    exit;
}

try {
    $pdo->beginTransaction();

    // In a real app, user_id comes from auth token. Mocking user 1 for now.
    $user_id = 1; 
    
    // Calculate total
    $total_amount = 0;
    foreach ($input['items'] as $item) {
        $total_amount += ($item['price'] * $item['quantity']);
    }

    // Create Order
    $stmt = $pdo->prepare("INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, 'Pending')");
    $stmt->execute([$user_id, $total_amount]);
    $order_id = $pdo->lastInsertId();

    // Insert Order Items and Update Inventory
    $stmtItem = $pdo->prepare("INSERT INTO order_items (order_id, book_id, quantity, price_ksh) VALUES (?, ?, ?, ?)");
    $stmtInventory = $pdo->prepare("UPDATE books SET inventory_count = MAX(0, inventory_count - ?) WHERE id = ?");

    foreach ($input['items'] as $item) {
        $stmtItem->execute([$order_id, $item['book_id'], $item['quantity'], $item['price']]);
        $stmtInventory->execute([$item['quantity'], $item['book_id']]);
    }

    $pdo->commit();

    echo json_encode([
        'ok' => true,
        'order_id' => $order_id,
        'message' => 'Order placed successfully'
    ]);

} catch (Exception $e) {
    $pdo->rollBack();
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => $e->getMessage()]);
}
