<?php
require 'db.php';

// Handle inline stock updates
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['update_stock'])) {
    $book_id = $_POST['book_id'];
    $new_stock = (int)$_POST['inventory_count'];
    $stmt = $pdo->prepare("UPDATE books SET inventory_count = ? WHERE id = ?");
    $stmt->execute([$new_stock, $book_id]);
    
    // Return JSON if AJAX
    if (!empty($_SERVER['HTTP_X_REQUESTED_WITH']) && strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) == 'xmlhttprequest') {
        echo json_encode(['success' => true]);
        exit;
    }
    header("Location: inventory.php?updated=1");
    exit;
}

// Fetch Inventory Stats
$total_items = $pdo->query("SELECT SUM(inventory_count) FROM books")->fetchColumn() ?: 0;
$low_stock_count = $pdo->query("SELECT COUNT(*) FROM books WHERE inventory_count > 0 AND inventory_count <= 10")->fetchColumn() ?: 0;
$out_of_stock_count = $pdo->query("SELECT COUNT(*) FROM books WHERE inventory_count = 0")->fetchColumn() ?: 0;

// Fetch books ordered by lowest stock first
$stmt = $pdo->query("SELECT id, title, author, cover_url, inventory_count FROM books ORDER BY inventory_count ASC");
$books = $stmt->fetchAll(PDO::FETCH_ASSOC);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inventory | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .stock-input { width: 80px; padding: 6px 10px; border: 1px solid var(--border-color); border-radius: 4px; text-align: center; font-weight: 600; font-family: inherit; }
        .stock-input:focus { border-color: var(--accent-green); outline: none; }
        .status-dot-lg { width: 10px; height: 10px; border-radius: 50%; display: inline-block; margin-right: 6px; }
        .bg-red { background: var(--danger); }
        .bg-yellow { background: #F57F17; }
        .bg-green { background: var(--accent-green); }
        .save-btn { padding: 6px 12px; background: var(--accent-light); color: var(--accent-green); border: none; border-radius: 4px; font-weight: 600; cursor: pointer; display: none; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
            <h1 style="font-size: 24px; font-weight: 700;">Stock Management</h1>
        </div>

        <?php if ($out_of_stock_count > 0 || $low_stock_count > 0): ?>
        <div style="background: #FFF3E0; border: 1px solid #FFE0B2; padding: 16px; border-radius: var(--radius-md); margin-bottom: 24px; display: flex; align-items: center; gap: 12px;">
            <svg width="24" height="24" fill="none" stroke="#E65100" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
            <div>
                <strong style="color: #E65100;">Action Required:</strong>
                <span style="color: #E65100;">You have <?= $out_of_stock_count ?> items out of stock and <?= $low_stock_count ?> items running low.</span>
            </div>
        </div>
        <?php endif; ?>

        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th>Product</th>
                        <th>Status</th>
                        <th>Current Stock</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach($books as $b): 
                        $stock = $b['inventory_count'];
                        $dotClass = $stock > 10 ? 'bg-green' : ($stock > 0 ? 'bg-yellow' : 'bg-red');
                        $statusText = $stock > 10 ? 'Healthy' : ($stock > 0 ? 'Low Stock' : 'Out of Stock');
                    ?>
                    <tr>
                        <td>
                            <div style="display:flex; align-items:center; gap: 12px;">
                                <?php if($b['cover_url']): ?><img src="<?= $b['cover_url'] ?>" style="width:32px; height:44px; object-fit:cover; border-radius:4px;"><?php else: ?><div style="width:32px; height:44px; background:var(--bg-cream); border-radius:4px;"></div><?php endif; ?>
                                <div>
                                    <div style="font-weight: 600; color: var(--text-main);"><?= htmlspecialchars($b['title']) ?></div>
                                    <div style="font-size: 12px; color: var(--text-muted);"><?= htmlspecialchars($b['author']) ?></div>
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="status-dot-lg <?= $dotClass ?>"></span> <?= $statusText ?>
                        </td>
                        <td>
                            <form method="POST" class="stock-form" style="display:flex; align-items:center; gap:8px;" onsubmit="handleSave(event, this)">
                                <input type="hidden" name="update_stock" value="1">
                                <input type="hidden" name="book_id" value="<?= $b['id'] ?>">
                                <input type="number" name="inventory_count" value="<?= $stock ?>" class="stock-input" oninput="this.nextElementSibling.style.display='block'">
                                <button type="submit" class="save-btn">Save</button>
                            </form>
                        </td>
                        <td>
                            <button class="action-btn" style="color:var(--text-muted);" title="Order more stock">
                                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                            </button>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </main>

    <script>
        function handleSave(e, form) {
            e.preventDefault();
            const formData = new FormData(form);
            const btn = form.querySelector('.save-btn');
            btn.textContent = 'Saving...';
            
            fetch('inventory.php', {
                method: 'POST',
                body: formData,
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            }).then(r => r.json()).then(d => {
                if(d.success) {
                    btn.textContent = 'Saved!';
                    setTimeout(() => btn.style.display = 'none', 1000);
                }
            });
        }
    </script>
</body>
</html>
