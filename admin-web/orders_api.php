<?php
require 'db.php';
require 'includes/auth_gate.php';
require_once 'includes/webhook.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';
    if ($action === 'update_status') {
        $id = $_POST['id'] ?? '';
        $status = $_POST['status'] ?? '';
        $allowed = ['Pending','Processing','Shipped','Delivered','Cancelled'];
        if ($id && in_array($status, $allowed)) {
            $stmt = $pdo->prepare("UPDATE orders SET status = ? WHERE id = ?");
            $stmt->execute([$status, $id]);
            notifyKtorWebhook($id, $status);
            echo json_encode(['ok' => true]);
            exit;
        }
    }
}
echo json_encode(['ok' => false, 'error' => 'Invalid request']);
