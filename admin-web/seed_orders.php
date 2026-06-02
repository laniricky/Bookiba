<?php
require 'db.php';

echo "Seeding test users and orders...<br>";

// 1. Create a dummy user
$user_id = '11111111-1111-1111-1111-111111111111';
$stmt = $pdo->prepare("INSERT OR IGNORE INTO users (id, name, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([$user_id, 'Jane Doe', 'jane@example.com', 'hash', date('Y-m-d H:i:s')]);

// 2. Create another user
$user_id2 = '22222222-2222-2222-2222-222222222222';
$stmt = $pdo->prepare("INSERT OR IGNORE INTO users (id, name, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([$user_id2, 'John Smith', 'john@example.com', 'hash', date('Y-m-d H:i:s')]);

// Get some books
$books = $pdo->query("SELECT id, title, price_ksh FROM books LIMIT 3")->fetchAll();

if (empty($books)) {
    echo "No books found to attach to orders. Please add books first.";
    exit;
}

// 3. Create Orders
$orders = [
    [
        'id' => 'aaaa0001-0000-0000-0000-000000000000',
        'user_id' => $user_id,
        'status' => 'Pending',
        'payment_method' => 'M-Pesa',
        'address' => '123 Main St, Nairobi',
        'items' => [
            ['book' => $books[0], 'qty' => 1],
        ]
    ],
    [
        'id' => 'aaaa0002-0000-0000-0000-000000000000',
        'user_id' => $user_id2,
        'status' => 'Processing',
        'payment_method' => 'Card',
        'address' => '456 Westlands, Nairobi',
        'items' => [
            ['book' => $books[0], 'qty' => 1],
            ['book' => $books[1] ?? $books[0], 'qty' => 2]
        ]
    ],
    [
        'id' => 'aaaa0003-0000-0000-0000-000000000000',
        'user_id' => $user_id,
        'status' => 'Shipped',
        'payment_method' => 'Cash on Delivery',
        'address' => '789 Karen, Nairobi',
        'items' => [
            ['book' => $books[2] ?? $books[0], 'qty' => 1]
        ]
    ]
];

foreach ($orders as $o) {
    // calculate total
    $total = 0;
    foreach ($o['items'] as $item) {
        $total += $item['qty'] * $item['book']['price_ksh'];
    }

    $stmt = $pdo->prepare("INSERT OR IGNORE INTO orders (id, user_id, total_amount, status, payment_method, shipping_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
    $stmt->execute([$o['id'], $o['user_id'], $total, $o['status'], $o['payment_method'], $o['address'], date('Y-m-d H:i:s')]);

    foreach ($o['items'] as $item) {
        $stmt = $pdo->prepare("INSERT OR IGNORE INTO order_items (order_id, book_id, quantity, price_ksh) VALUES (?, ?, ?, ?)");
        $stmt->execute([$o['id'], $item['book']['id'], $item['qty'], $item['book']['price_ksh']]);
    }
}

echo "Done!";
