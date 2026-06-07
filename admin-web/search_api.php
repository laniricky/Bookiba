<?php
require 'db.php';
header('Content-Type: application/json');

$q = trim($_GET['q'] ?? '');
if (strlen($q) < 2) { echo json_encode(['results' => []]); exit; }

$like = "%$q%";
$results = [];

// Search orders
$stmt = $pdo->prepare("
    SELECT o.id, u.name as customer_name, o.total_amount, o.status, o.created_at
    FROM orders o JOIN users u ON o.user_id = u.id
    WHERE u.name LIKE ? OR o.id LIKE ?
    ORDER BY o.created_at DESC LIMIT 4
");
$stmt->execute([$like, $like]);
foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $r) {
    $results[] = [
        'type'     => 'Orders',
        'title'    => $r['customer_name'],
        'subtitle' => 'Ksh ' . number_format($r['total_amount']) . ' · ' . $r['status'] . ' · ' . date('M j, Y', strtotime($r['created_at'])),
        'url'      => 'orders.php?view=' . $r['id'],
        'icon'     => strtoupper(substr($r['customer_name'], 0, 1)),
    ];
}

// Search books
$stmt = $pdo->prepare("
    SELECT id, title, author, price_ksh, inventory_count
    FROM books WHERE title LIKE ? OR author LIKE ?
    ORDER BY title ASC LIMIT 4
");
$stmt->execute([$like, $like]);
foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $r) {
    $results[] = [
        'type'     => 'Products',
        'title'    => $r['title'],
        'subtitle' => $r['author'] . ' · Ksh ' . number_format($r['price_ksh']) . ' · ' . $r['inventory_count'] . ' in stock',
        'url'      => 'products.php?q=' . urlencode($q),
        'icon'     => strtoupper(substr($r['title'], 0, 1)),
    ];
}

// Search customers
$stmt = $pdo->prepare("
    SELECT u.id, u.name, u.email, COUNT(o.id) as order_count
    FROM users u LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.name LIKE ? OR u.email LIKE ?
    GROUP BY u.id ORDER BY order_count DESC LIMIT 3
");
$stmt->execute([$like, $like]);
foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $r) {
    $results[] = [
        'type'     => 'Customers',
        'title'    => $r['name'],
        'subtitle' => $r['email'] . ' · ' . $r['order_count'] . ' orders',
        'url'      => 'customers.php?q=' . urlencode($r['name']),
        'icon'     => strtoupper(substr($r['name'], 0, 1)),
    ];
}

echo json_encode(['results' => $results, 'query' => $q]);
