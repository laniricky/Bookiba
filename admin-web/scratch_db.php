<?php
require 'db.php';
$stmt = $pdo->query("SELECT column_name FROM information_schema.columns WHERE table_name = 'books'");
$cols = $stmt->fetchAll(PDO::FETCH_COLUMN);
print_r($cols);
