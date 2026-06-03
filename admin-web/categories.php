<?php
require 'db.php';

// Fetch Categories Data
$stmt = $pdo->query("
    SELECT b.category, 
           COUNT(DISTINCT b.id) as book_count, 
           SUM(oi.quantity) as items_sold
    FROM books b
    LEFT JOIN order_items oi ON b.id = oi.book_id
    LEFT JOIN orders o ON oi.order_id = o.id AND o.status != 'Cancelled'
    GROUP BY b.category
    ORDER BY book_count DESC
");
$categories = $stmt->fetchAll(PDO::FETCH_ASSOC);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Categories | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .category-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px; }
        .cat-card { background: var(--card-white); border-radius: var(--radius-lg); padding: 24px; border: 1px solid var(--border-color); cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
        .cat-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.06); }
        .cat-icon-wrap { width: 48px; height: 48px; border-radius: 12px; background: #F8F5F0; display: flex; align-items: center; justify-content: center; color: var(--accent-green); margin-bottom: 16px; }
        .cat-title { font-weight: 700; font-size: 18px; margin-bottom: 8px; }
        .cat-meta { color: var(--text-muted); font-size: 14px; display: flex; justify-content: space-between; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
            <h1 style="font-size: 24px; font-weight: 700;">Categories</h1>
            <button class="btn btn-primary" onclick="alert('Add Category coming soon!')">+ New Category</button>
        </div>

        <div class="category-grid">
            <?php foreach($categories as $c): 
                $name = $c['category'] ?: 'Uncategorised';
            ?>
            <div class="cat-card">
                <div class="cat-icon-wrap">
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
                </div>
                <div class="cat-title"><?= htmlspecialchars($name) ?></div>
                <div class="cat-meta">
                    <span><?= $c['book_count'] ?> Books</span>
                    <span><?= (int)$c['items_sold'] ?> Sold</span>
                </div>
            </div>
            <?php endforeach; ?>
        </div>

    </main>
</body>
</html>
