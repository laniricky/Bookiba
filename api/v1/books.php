<?php
require __DIR__ . '/../../admin-web/db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$id = $_GET['id'] ?? null;
$q = $_GET['q'] ?? '';
$category = $_GET['category'] ?? '';

try {
    if ($id) {
        // Get single book details
        $stmt = $pdo->prepare("SELECT * FROM books WHERE id = ?");
        $stmt->execute([$id]);
        $book = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($book) {
            echo json_encode(['ok' => true, 'data' => $book]);
        } else {
            http_response_code(404);
            echo json_encode(['ok' => false, 'error' => 'Book not found']);
        }
    } else {
        // List/Search books
        $where = [];
        $params = [];
        
        if ($q) {
            $where[] = "(title LIKE ? OR author LIKE ?)";
            $params[] = "%$q%";
            $params[] = "%$q%";
        }
        
        if ($category) {
            $where[] = "category = ?";
            $params[] = $category;
        }
        
        $whereSql = $where ? "WHERE " . implode(' AND ', $where) : "";
        
        $stmt = $pdo->prepare("SELECT id, title, author, price_ksh, cover_url, category FROM books $whereSql ORDER BY created_at DESC LIMIT 50");
        $stmt->execute($params);
        $books = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        echo json_encode(['ok' => true, 'data' => $books]);
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => $e->getMessage()]);
}
