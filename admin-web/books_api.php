<?php
require 'db.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['ok' => false, 'error' => 'Method not allowed']);
    exit;
}

$action = $_POST['action'] ?? '';
$book_id = $_POST['book_id'] ?? '';

if ($action === 'toggle') {
    $field = $_POST['field'] ?? '';
    $value = isset($_POST['value']) ? (int)$_POST['value'] : 0;

    // Security: Only allow toggling specific boolean fields
    $allowed_fields = ['is_rare', 'is_featured', 'is_staff_pick'];
    if (!in_array($field, $allowed_fields)) {
        echo json_encode(['ok' => false, 'error' => 'Invalid field']);
        exit;
    }

    $stmt = $pdo->prepare("UPDATE books SET {$field} = ? WHERE id = ?");
    $result = $stmt->execute([$value, $book_id]);

    echo json_encode(['ok' => $result]);
    exit;
}

echo json_encode(['ok' => false, 'error' => 'Invalid action']);
