<?php
require 'db.php';

$search = $_GET['q'] ?? '';
$category = $_GET['category'] ?? '';

$where = [];
$params = [];
if ($search) {
    $where[] = "(title LIKE ? OR author LIKE ?)";
    $params[] = "%$search%";
    $params[] = "%$search%";
}
if ($category) {
    $where[] = "category = ?";
    $params[] = $category;
}
$where_sql = $where ? "WHERE " . implode(' AND ', $where) : "";

$stmt = $pdo->prepare("SELECT * FROM books $where_sql ORDER BY created_at DESC");
$stmt->execute($params);
$books = $stmt->fetchAll(PDO::FETCH_ASSOC);

$categories = $pdo->query("SELECT DISTINCT category FROM books WHERE category != ''")->fetchAll(PDO::FETCH_COLUMN);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .filter-bar { display: flex; gap: 12px; margin-bottom: 24px; }
        .filter-select { padding: 8px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 13px; outline: none; }
        .product-cover { width: 40px; height: 56px; border-radius: 4px; object-fit: cover; background: var(--bg-cream); }
        .actions { display: flex; gap: 8px; }
        .action-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); }
        .action-btn:hover { color: var(--accent-green); }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
            <h1 style="font-size: 24px; font-weight: 700;">Products Catalog</h1>
            <button class="btn btn-primary" onclick="alert('Add Product dialog coming soon!')">+ Add Product</button>
        </div>

        <div class="card">
            <div class="filter-bar">
                <form method="GET" style="display: flex; gap: 12px; width: 100%;">
                    <input type="text" name="q" value="<?= htmlspecialchars($search) ?>" placeholder="Search books..." class="filter-select" style="flex: 1;">
                    <select name="category" class="filter-select" onchange="this.form.submit()">
                        <option value="">All Categories</option>
                        <?php foreach($categories as $c): ?>
                            <option value="<?= htmlspecialchars($c) ?>" <?= $category === $c ? 'selected' : '' ?>><?= htmlspecialchars($c) ?></option>
                        <?php endforeach; ?>
                    </select>
                </form>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>Book</th>
                        <th>Category</th>
                        <th>Price (Ksh)</th>
                        <th>Stock</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($books)): ?>
                        <tr><td colspan="6" style="text-align:center; padding: 32px; color:var(--text-muted);">No products found.</td></tr>
                    <?php endif; ?>
                    <?php foreach($books as $b): 
                        $stock = $b['inventory_count'];
                        $status = $stock > 10 ? 'In Stock' : ($stock > 0 ? 'Low Stock' : 'Out of Stock');
                        $badgeClass = $stock > 10 ? 'badge-shipped' : ($stock > 0 ? 'badge-processing' : 'badge-delivered'); // repurposing delivered class for red if we tweak css, but processing is orange
                    ?>
                    <tr>
                        <td>
                            <div style="display:flex; align-items:center; gap: 12px;">
                                <?php if($b['cover_url']): ?><img src="<?= $b['cover_url'] ?>" class="product-cover"><?php else: ?><div class="product-cover"></div><?php endif; ?>
                                <div>
                                    <div style="font-weight: 600; color: var(--text-main);"><?= htmlspecialchars($b['title']) ?></div>
                                    <div style="font-size: 12px; color: var(--text-muted);"><?= htmlspecialchars($b['author']) ?></div>
                                </div>
                            </div>
                        </td>
                        <td><?= htmlspecialchars($b['category'] ?: 'Uncategorised') ?></td>
                        <td style="font-weight: 600;">Ksh <?= number_format($b['price_ksh']) ?></td>
                        <td><?= $stock ?></td>
                        <td><span class="badge <?= $badgeClass ?>"><?= $status ?></span></td>
                        <td>
                            <div class="actions">
                                <button class="action-btn" title="Edit">
                                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                                </button>
                                <button class="action-btn" title="Delete" style="color:var(--danger);">
                                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </main>
</body>
</html>
