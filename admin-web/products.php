<?php
require 'db.php';

// Handle AJAX inline edit
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';
    if ($action === 'update_field') {
        $field = $_POST['field'];
        $value = $_POST['value'];
        $id = $_POST['id'];
        $allowed = ['title', 'author', 'price_ksh', 'inventory_count'];
        if (in_array($field, $allowed)) {
            $stmt = $pdo->prepare("UPDATE books SET $field = ? WHERE id = ?");
            $stmt->execute([$value, $id]);
        }
        echo json_encode(['ok' => true]);
    } elseif ($action === 'delete') {
        $stmt = $pdo->prepare("DELETE FROM books WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);
    } elseif ($action === 'add') {
        $id = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x', mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000, mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff));
        $stmt = $pdo->prepare("INSERT INTO books (id, title, author, price_ksh, category, inventory_count, cover_url, seller_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$id, $_POST['title'], $_POST['author'], (int)$_POST['price_ksh'], $_POST['category'], (int)$_POST['inventory_count'], $_POST['cover_url'], '00000000-0000-0000-0000-000000000000', date('Y-m-d H:i:s')]);
        echo json_encode(['ok' => true, 'id' => $id]);
    }
    exit;
}

// Fetch with filters
$search = $_GET['q'] ?? '';
$category_filter = $_GET['category'] ?? '';
$stock_filter = $_GET['stock'] ?? '';
$sort = $_GET['sort'] ?? 'created_at_desc';
$view = $_GET['view'] ?? 'table';

$where = []; $params = [];
if ($search) { $where[] = "(title LIKE ? OR author LIKE ?)"; $params[] = "%$search%"; $params[] = "%$search%"; }
if ($category_filter) { $where[] = "category = ?"; $params[] = $category_filter; }
if ($stock_filter === 'low') { $where[] = "inventory_count > 0 AND inventory_count <= 10"; }
elseif ($stock_filter === 'out') { $where[] = "inventory_count = 0"; }
elseif ($stock_filter === 'good') { $where[] = "inventory_count > 10"; }

$sorts = [
    'created_at_desc' => 'created_at DESC',
    'price_desc' => 'price_ksh DESC',
    'price_asc' => 'price_ksh ASC',
    'title_asc' => 'title ASC',
    'stock_asc' => 'inventory_count ASC',
];
$order_by = $sorts[$sort] ?? 'created_at DESC';
$where_sql = $where ? "WHERE " . implode(' AND ', $where) : "";

$stmt = $pdo->prepare("
    SELECT b.*, 
           COALESCE(SUM(oi.quantity), 0) as units_sold,
           COALESCE(SUM(oi.quantity * oi.price_ksh), 0) as total_revenue
    FROM books b
    LEFT JOIN order_items oi ON b.id = oi.book_id
    LEFT JOIN orders o ON oi.order_id = o.id AND o.status != 'Cancelled'
    $where_sql
    GROUP BY b.id
    ORDER BY $order_by
");
$stmt->execute($params);
$books = $stmt->fetchAll(PDO::FETCH_ASSOC);

$categories = $pdo->query("SELECT DISTINCT category FROM books WHERE category != '' ORDER BY category")->fetchAll(PDO::FETCH_COLUMN);
$total_books = count($books);
$total_value = array_sum(array_column($books, 'price_ksh'));
$low_stock_count = count(array_filter($books, fn($b) => $b['inventory_count'] > 0 && $b['inventory_count'] <= 10));
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .filter-strip { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 20px; }
        .filter-chip { padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; border: 1px solid var(--border-color); cursor: pointer; background: white; color: var(--text-muted); transition: all 0.15s; }
        .filter-chip.active { background: var(--text-main); color: white; border-color: var(--text-main); }
        .filter-chip.danger { background: #FFEBEE; color: #C62828; border-color: #FFCDD2; }
        .view-toggle { display: flex; border: 1px solid var(--border-color); border-radius: var(--radius-sm); overflow: hidden; margin-left: auto; }
        .view-toggle-btn { padding: 7px 12px; cursor: pointer; background: white; border: none; color: var(--text-muted); }
        .view-toggle-btn.active { background: var(--text-main); color: white; }
        .products-table { width: 100%; border-collapse: collapse; }
        .products-table th { padding: 12px 16px; text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: 0.8px; color: var(--text-muted); font-weight: 700; border-bottom: 2px solid var(--border-color); cursor: pointer; user-select: none; }
        .products-table th:hover { color: var(--text-main); }
        .products-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-color); vertical-align: middle; font-size: 14px; }
        .products-table tbody tr:hover { background: #FAFAFA; }
        .products-table tbody tr:hover .row-actions { opacity: 1; }
        .editable { cursor: text; border-radius: 4px; padding: 2px 6px; transition: background 0.15s; }
        .editable:hover { background: var(--bg-cream); }
        .editable:focus { outline: 2px solid var(--accent-green); background: white; }
        .row-actions { opacity: 0; display: flex; gap: 6px; transition: opacity 0.2s; }
        .row-action-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); padding: 4px; border-radius: 4px; }
        .row-action-btn:hover { background: var(--bg-cream); color: var(--text-main); }
        .row-action-btn.del:hover { background: #FFEBEE; color: #C62828; }
        .stock-bar-wrap { display: flex; align-items: center; gap: 8px; }
        .stock-bar { height: 6px; border-radius: 3px; background: var(--border-color); flex: 1; overflow: hidden; }
        .stock-bar-fill { height: 100%; border-radius: 3px; }
        .mini-badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 10px; font-weight: 700; text-transform: uppercase; }
        .mini-badge-green { background: #E8F5E9; color: #2E7D32; }
        .mini-badge-amber { background: #FFF3E0; color: #E65100; }
        .mini-badge-red { background: #FFEBEE; color: #C62828; }

        /* Grid View */
        .products-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; }
        .grid-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); overflow: hidden; transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; }
        .grid-card:hover { transform: translateY(-4px); box-shadow: 0 16px 40px rgba(0,0,0,0.08); }
        .grid-card-cover { height: 200px; width: 100%; object-fit: cover; background: var(--bg-cream); display: block; }
        .grid-card-body { padding: 14px; }
        .grid-card-title { font-weight: 700; font-size: 14px; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .grid-card-author { font-size: 12px; color: var(--text-muted); margin-bottom: 10px; }
        .grid-card-footer { display: flex; justify-content: space-between; align-items: center; }

        /* Add form slide-over */
        .slide-over { position: fixed; right: -500px; top: 0; width: 480px; height: 100vh; background: white; box-shadow: -8px 0 40px rgba(0,0,0,0.12); z-index: 1000; transition: right 0.3s cubic-bezier(0.4,0,0.2,1); display: flex; flex-direction: column; }
        .slide-over.open { right: 0; }
        .slide-over-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 999; display: none; }
        .slide-over-overlay.show { display: block; }
        .slide-over-header { padding: 24px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
        .slide-over-body { padding: 24px; overflow-y: auto; flex: 1; }
        .slide-over-footer { padding: 20px 24px; border-top: 1px solid var(--border-color); display: flex; gap: 12px; justify-content: flex-end; }
        .form-group { margin-bottom: 20px; }
        .form-group label { display: block; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.6px; color: var(--text-muted); margin-bottom: 6px; }
        .form-input { width: 100%; padding: 10px 14px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 14px; font-family: inherit; outline: none; box-sizing: border-box; }
        .form-input:focus { border-color: var(--accent-green); box-shadow: 0 0 0 3px rgba(54,81,52,0.1); }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <div class="slide-over-overlay" id="overlay" onclick="closeSlideOver()"></div>
    <div class="slide-over" id="slideOver">
        <div class="slide-over-header">
            <div style="font-size:18px; font-weight:800;">Add New Product</div>
            <button onclick="closeSlideOver()" style="background:none; border:none; cursor:pointer; color:var(--text-muted);">
                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
        </div>
        <div class="slide-over-body">
            <div class="form-group"><label>Title *</label><input type="text" id="f_title" class="form-input" placeholder="e.g. Atomic Habits"></div>
            <div class="form-group"><label>Author *</label><input type="text" id="f_author" class="form-input" placeholder="e.g. James Clear"></div>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px;">
                <div class="form-group"><label>Price (Ksh) *</label><input type="number" id="f_price" class="form-input" placeholder="850"></div>
                <div class="form-group"><label>Initial Stock *</label><input type="number" id="f_stock" class="form-input" value="15"></div>
            </div>
            <div class="form-group"><label>Category</label><input type="text" id="f_category" class="form-input" placeholder="Fiction, Self-Help..."></div>
            <div class="form-group"><label>Cover Image URL</label><input type="url" id="f_cover" class="form-input" placeholder="https://..."></div>
        </div>
        <div class="slide-over-footer">
            <button class="btn btn-outline" onclick="closeSlideOver()">Cancel</button>
            <button class="btn btn-primary" onclick="saveProduct()">Save Product</button>
        </div>
    </div>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:20px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Products</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= $total_books ?> products · <?= $low_stock_count ?> low stock</p>
            </div>
            <button class="btn btn-primary" onclick="openSlideOver()" style="display:flex; align-items:center; gap:8px;">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"/></svg>
                Add Product
            </button>
        </div>

        <!-- Filters -->
        <div class="filter-strip">
            <form method="GET" id="filterForm" style="display:contents;">
                <input type="hidden" name="view" value="<?= htmlspecialchars($view) ?>">
                <input type="hidden" name="sort" value="<?= htmlspecialchars($sort) ?>">
                <div style="display:flex; align-items:center; gap:8px; border:1px solid var(--border-color); border-radius:var(--radius-sm); padding:7px 12px; background:white;">
                    <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                    <input type="text" name="q" value="<?= htmlspecialchars($search) ?>" placeholder="Search products…" style="border:none; outline:none; font-size:13px; font-family:inherit; width:180px;" onchange="this.form.submit()">
                </div>
                <select name="category" class="form-input" style="width:auto; padding:7px 14px;" onchange="this.form.submit()">
                    <option value="">All Categories</option>
                    <?php foreach($categories as $c): ?>
                    <option value="<?= htmlspecialchars($c) ?>" <?= $category_filter === $c ? 'selected' : '' ?>><?= htmlspecialchars($c) ?></option>
                    <?php endforeach; ?>
                </select>
                <span class="filter-chip <?= !$stock_filter ? 'active' : '' ?>" onclick="setStock('')">All Stock</span>
                <span class="filter-chip <?= $stock_filter === 'good' ? 'active' : '' ?>" onclick="setStock('good')">Healthy</span>
                <span class="filter-chip <?= $stock_filter === 'low' ? 'active' : '' ?> danger" onclick="setStock('low')">Low Stock</span>
                <span class="filter-chip <?= $stock_filter === 'out' ? 'active' : '' ?> danger" onclick="setStock('out')">Out of Stock</span>
                <input type="hidden" name="stock" id="stockInput" value="<?= htmlspecialchars($stock_filter) ?>">
                <select name="sort" class="form-input" style="width:auto; padding:7px 14px; margin-left:auto;" onchange="this.form.submit()">
                    <option value="created_at_desc" <?= $sort === 'created_at_desc' ? 'selected' : '' ?>>Newest First</option>
                    <option value="price_desc" <?= $sort === 'price_desc' ? 'selected' : '' ?>>Price: High to Low</option>
                    <option value="price_asc" <?= $sort === 'price_asc' ? 'selected' : '' ?>>Price: Low to High</option>
                    <option value="title_asc" <?= $sort === 'title_asc' ? 'selected' : '' ?>>Title A–Z</option>
                    <option value="stock_asc" <?= $sort === 'stock_asc' ? 'selected' : '' ?>>Lowest Stock</option>
                </select>
            </form>
            <div class="view-toggle">
                <button class="view-toggle-btn <?= $view === 'table' ? 'active' : '' ?>" onclick="setView('table')">
                    <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M3 6h18M3 14h18M3 18h18"/></svg>
                </button>
                <button class="view-toggle-btn <?= $view === 'grid' ? 'active' : '' ?>" onclick="setView('grid')">
                    <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
                </button>
            </div>
        </div>

        <!-- TABLE VIEW -->
        <?php if ($view === 'table'): ?>
        <div class="card" style="padding:0; overflow:hidden;">
            <table class="products-table">
                <thead>
                    <tr>
                        <th>Product</th>
                        <th onclick="setSort('price_desc')">Price ↕</th>
                        <th onclick="setSort('stock_asc')">Stock ↕</th>
                        <th>Units Sold</th>
                        <th>Revenue</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach($books as $b):
                        $stock = (int)$b['inventory_count'];
                        $maxStock = 50;
                        $pct = min(100, round($stock / $maxStock * 100));
                        $barColor = $stock > 10 ? '#2E7D32' : ($stock > 0 ? '#F57F17' : '#C62828');
                        $badgeClass = $stock > 10 ? 'mini-badge-green' : ($stock > 0 ? 'mini-badge-amber' : 'mini-badge-red');
                        $badgeText = $stock > 10 ? 'In Stock' : ($stock > 0 ? 'Low' : 'Out');
                    ?>
                    <tr>
                        <td>
                            <div style="display:flex; align-items:center; gap:14px;">
                                <?php if($b['cover_url']): ?><img src="<?= $b['cover_url'] ?>" style="width:36px; height:50px; object-fit:cover; border-radius:4px;">
                                <?php else: ?><div style="width:36px; height:50px; background:var(--bg-cream); border-radius:4px;"></div><?php endif; ?>
                                <div>
                                    <div class="editable" contenteditable="true" data-field="title" data-id="<?= $b['id'] ?>" style="font-weight:700;"><?= htmlspecialchars($b['title']) ?></div>
                                    <div class="editable" contenteditable="true" data-field="author" data-id="<?= $b['id'] ?>" style="font-size:12px; color:var(--text-muted);"><?= htmlspecialchars($b['author']) ?></div>
                                    <div style="font-size:11px; color:var(--text-muted); margin-top:2px;"><?= htmlspecialchars($b['category'] ?: 'Uncategorised') ?></div>
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="editable" contenteditable="true" data-field="price_ksh" data-id="<?= $b['id'] ?>" style="font-weight:700;">Ksh <?= number_format($b['price_ksh']) ?></span>
                        </td>
                        <td>
                            <div class="stock-bar-wrap">
                                <span class="mini-badge <?= $badgeClass ?>"><?= $badgeText ?></span>
                                <div class="stock-bar"><div class="stock-bar-fill" style="width:<?= $pct ?>%; background:<?= $barColor ?>;"></div></div>
                                <span style="font-size:12px; font-weight:700; color:var(--text-muted); flex-shrink:0;"><?= $stock ?></span>
                            </div>
                        </td>
                        <td style="font-weight:600;"><?= number_format($b['units_sold']) ?></td>
                        <td style="font-weight:600;">Ksh <?= number_format($b['total_revenue']) ?></td>
                        <td>
                            <div class="row-actions">
                                <button class="row-action-btn" title="Duplicate">
                                    <svg width="15" height="15" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
                                </button>
                                <button class="row-action-btn del" title="Delete" onclick="deleteProduct('<?= $b['id'] ?>', this)">
                                    <svg width="15" height="15" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                    <?php if(empty($books)): ?>
                    <tr><td colspan="6" style="padding:40px; text-align:center; color:var(--text-muted);">No products found.</td></tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>

        <!-- GRID VIEW -->
        <?php else: ?>
        <div class="products-grid">
            <?php foreach($books as $b): $stock = (int)$b['inventory_count']; ?>
            <div class="grid-card">
                <?php if($b['cover_url']): ?>
                <img src="<?= $b['cover_url'] ?>" class="grid-card-cover">
                <?php else: ?>
                <div class="grid-card-cover" style="display:flex; align-items:center; justify-content:center;">
                    <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="opacity:0.2;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>
                </div>
                <?php endif; ?>
                <div class="grid-card-body">
                    <div class="grid-card-title"><?= htmlspecialchars($b['title']) ?></div>
                    <div class="grid-card-author"><?= htmlspecialchars($b['author']) ?></div>
                    <div class="grid-card-footer">
                        <strong style="font-size:15px;">Ksh <?= number_format($b['price_ksh']) ?></strong>
                        <span class="mini-badge <?= $stock > 10 ? 'mini-badge-green' : ($stock > 0 ? 'mini-badge-amber' : 'mini-badge-red') ?>"><?= $stock ?> left</span>
                    </div>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
        <?php endif; ?>
    </main>

    <script>
        // Inline editing
        document.querySelectorAll('.editable').forEach(el => {
            el.addEventListener('blur', function() {
                const field = this.dataset.field;
                const id = this.dataset.id;
                let value = this.textContent.trim();
                if (field === 'price_ksh') value = value.replace(/[^0-9]/g, '');
                fetch('products.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                    body: `action=update_field&field=${field}&id=${id}&value=${encodeURIComponent(value)}`
                });
            });
            el.addEventListener('keydown', e => { if(e.key === 'Enter') { e.preventDefault(); e.target.blur(); } });
        });

        function deleteProduct(id, btn) {
            if (!confirm('Permanently delete this product?')) return;
            fetch('products.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=delete&id=${id}`
            }).then(r => r.json()).then(d => {
                if(d.ok) btn.closest('tr').remove();
            });
        }

        function openSlideOver() { document.getElementById('slideOver').classList.add('open'); document.getElementById('overlay').classList.add('show'); }
        function closeSlideOver() { document.getElementById('slideOver').classList.remove('open'); document.getElementById('overlay').classList.remove('show'); }

        function saveProduct() {
            const data = {
                action: 'add',
                title: document.getElementById('f_title').value,
                author: document.getElementById('f_author').value,
                price_ksh: document.getElementById('f_price').value,
                inventory_count: document.getElementById('f_stock').value,
                category: document.getElementById('f_category').value,
                cover_url: document.getElementById('f_cover').value,
            };
            fetch('products.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: new URLSearchParams(data)
            }).then(r => r.json()).then(d => { if(d.ok) location.reload(); });
        }

        function setView(v) {
            const url = new URL(window.location); url.searchParams.set('view', v); window.location.href = url.toString();
        }
        function setSort(s) {
            const url = new URL(window.location); url.searchParams.set('sort', s); window.location.href = url.toString();
        }
        function setStock(s) {
            document.getElementById('stockInput').value = s;
            document.getElementById('filterForm').submit();
        }
    </script>
</body>
</html>
