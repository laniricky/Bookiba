<?php
require 'db.php';

function gen_uuid() {
    return sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000,
        mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );
}

$id = gen_uuid();
$seller_id = '00000000-0000-0000-0000-000000000000';
$created_at = date('Y-m-d H:i:s');

$sql = "INSERT INTO books (id, title, author, description, price_ksh, condition, cover_url, category, seller_id, is_rare, is_featured, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

$stmt = $pdo->prepare($sql);
$stmt->execute([
    $id,
    'The Alchemist',
    'Paulo Coelho',
    'A magical story about a young Andalusian shepherd on a journey to find a worldly treasure.',
    800,
    'Good',
    'https://covers.openlibrary.org/b/id/8739161-L.jpg',
    'Fiction',
    $seller_id,
    0,
    1,
    $created_at
]);

echo "Book inserted successfully! ID: $id\n";
