<?php
require __DIR__ . '/../../admin-web/db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *'); // For local dev

try {
    // 1. Featured Books
    $featured = $pdo->query("
        SELECT id, title, author, price_ksh, cover_url 
        FROM books 
        WHERE is_featured = 1 
        ORDER BY created_at DESC 
        LIMIT 5
    ")->fetchAll(PDO::FETCH_ASSOC);

    // 2. Staff Picks (Just picking one for now, or the latest)
    $staffPick = $pdo->query("
        SELECT id, title, author, price_ksh, cover_url 
        FROM books 
        WHERE is_staff_pick = 1 
        ORDER BY created_at DESC 
        LIMIT 1
    ")->fetch(PDO::FETCH_ASSOC);

    // 3. New Arrivals
    $newArrivals = $pdo->query("
        SELECT id, title, author, price_ksh, cover_url 
        FROM books 
        ORDER BY created_at DESC 
        LIMIT 6
    ")->fetchAll(PDO::FETCH_ASSOC);

    // 4. Story Trays (Mock data for now, could be categories or specific promotions)
    $stories = [
        ['id' => 's1', 'label' => 'Rare Finds'],
        ['id' => 's2', 'label' => 'Staff Favs'],
        ['id' => 's3', 'label' => 'African Lit'],
        ['id' => 's4', 'label' => 'Sci-Fi'],
        ['id' => 's5', 'label' => 'Classics']
    ];

    $response = [
        'ok' => true,
        'data' => [
            'featured' => $featured,
            'staffPick' => $staffPick ?: null,
            'newArrivals' => $newArrivals,
            'stories' => $stories
        ]
    ];

    echo json_encode($response);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => $e->getMessage()]);
}
