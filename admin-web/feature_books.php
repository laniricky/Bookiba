<?php
require_once 'db.php';

try {
    $stmt = $pdo->prepare("UPDATE books SET is_featured = true, is_staff_pick = true WHERE id IN (SELECT id FROM books LIMIT 3)");
    $stmt->execute();
    echo "Successfully featured books.\n";
} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>
