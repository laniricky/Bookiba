<?php
require 'db.php';

// Handle AJAX stock updates
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';
    if ($action === 'update_stock') {
        $stmt = $pdo->prepare("UPDATE books SET inventory_count = inventory_count + ? WHERE id = ?");
        $stmt->execute([(int)$_POST['delta'], $_POST['id']]);
        $new = $pdo->prepare("SELECT inventory_count FROM books WHERE id = ?")->execute([$_POST['id']]);
        $row = $pdo->prepare("SELECT inventory_count FROM books WHERE id = ?");
        $row->execute([$_POST['id']]);
        echo json_encode(['ok' => true, 'new_count' => $row->fetchColumn()]);
    } elseif ($action === 'set_stock') {
        $stmt = $pdo->prepare("UPDATE books SET inventory_count = ? WHERE id = ?");
        $stmt->execute([(int)$_POST['value'], $_POST['id']]);
        echo json_encode(['ok' => true]);
    }
    exit;
}

// Compute 30-day sales velocity per book
if ($isPostgres) {
    $vel_query = "SELECT book_id, SUM(quantity) as sold FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.status != 'Cancelled' AND o.created_at >= CURRENT_DATE - INTERVAL '30 days' GROUP BY book_id";
} else {
    $vel_query = "SELECT book_id, SUM(quantity) as sold FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.status != 'Cancelled' AND o.created_at >= date('now', '-30 days') GROUP BY book_id";
}
$velocityRows = $pdo->query($vel_query)->fetchAll(PDO::FETCH_ASSOC);
$velocity = [];
foreach ($velocityRows as $vrow) {
    $velocity[$vrow['book_id']] = (float)$vrow['sold'];
}

// Fetch all books with stock info
$books = $pdo->query("SELECT id, title, author, cover_url, inventory_count FROM books ORDER BY inventory_count ASC")->fetchAll(PDO::FETCH_ASSOC);

// Summary stats
$total_stock = array_sum(array_column($books, 'inventory_count'));
$out_of_stock = count(array_filter($books, fn($b) => $b['inventory_count'] == 0));
$low_stock = count(array_filter($books, fn($b) => $b['inventory_count'] > 0 && $b['inventory_count'] <= 10));
$healthy = count($books) - $out_of_stock - $low_stock;

// Reorder threshold
define('REORDER_POINT', 10);
define('MAX_STOCK', 50);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inventory | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .inv-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
        .inv-stat-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 20px; }
        .inv-stat-label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.6px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px; }
        .inv-stat-val { font-size: 28px; font-weight: 800; color: var(--text-main); line-height: 1; }
        .inv-stat-sub { font-size: 12px; color: var(--text-muted); margin-top: 6px; }

        .inv-table { width: 100%; border-collapse: collapse; }
        .inv-table th { padding: 10px 16px; font-size: 11px; text-transform: uppercase; letter-spacing: 0.8px; color: var(--text-muted); font-weight: 700; border-bottom: 2px solid var(--border-color); text-align: left; }
        .inv-table td { padding: 12px 16px; border-bottom: 1px solid var(--border-color); vertical-align: middle; }

        /* Health bar */
        .health-bar-outer { height: 8px; border-radius: 4px; background: var(--border-color); position: relative; overflow: hidden; }
        .health-bar-fill { height: 100%; border-radius: 4px; transition: width 0.4s; }
        .reorder-marker { position: absolute; top: -2px; bottom: -2px; width: 2px; background: #F57F17; border-radius: 2px; }

        /* Stepper */
        .stepper { display: flex; align-items: center; gap: 0; border: 1px solid var(--border-color); border-radius: var(--radius-sm); overflow: hidden; width: fit-content; }
        .stepper-btn { background: white; border: none; width: 32px; height: 32px; cursor: pointer; font-size: 18px; font-weight: 300; display: flex; align-items: center; justify-content: center; color: var(--text-main); transition: background 0.15s; }
        .stepper-btn:hover { background: var(--bg-cream); }
        .stepper-val { width: 48px; text-align: center; font-weight: 800; font-size: 15px; border: none; border-left: 1px solid var(--border-color); border-right: 1px solid var(--border-color); outline: none; font-family: inherit; padding: 0; background: white; }

        /* Days forecast badge */
        .forecast-badge { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: 700; }
        .forecast-ok { background: #E8F5E9; color: #2E7D32; }
        .forecast-warn { background: #FFF3E0; color: #E65100; }
        .forecast-crit { background: #FFEBEE; color: #C62828; }
        .forecast-none { background: var(--bg-cream); color: var(--text-muted); }

        .batch-bar { background: var(--text-main); color: white; padding: 12px 20px; display: none; align-items: center; gap: 16px; border-radius: var(--radius-md); margin-bottom: 16px; font-size: 14px; }
        .batch-bar.show { display: flex; }
        .batch-input { background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.3); color: white; padding: 6px 12px; border-radius: 4px; font-size: 14px; width: 80px; font-family: inherit; outline: none; }
        .batch-input::placeholder { color: rgba(255,255,255,0.5); }
        .batch-btn { background: white; color: var(--text-main); border: none; padding: 6px 14px; border-radius: 4px; font-weight: 700; cursor: pointer; font-size: 13px; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:20px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Stock Management</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= count($books) ?> products · <?= number_format($total_stock) ?> total units</p>
            </div>
            <div style="display:flex; gap:12px;">
                <button id="batchToggle" class="btn btn-outline" style="display:flex; align-items:center; gap:8px;" onclick="toggleBatch()">
                    <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/></svg>
                    Batch Receive
                </button>
            </div>
        </div>

        <!-- Stats Row -->
        <div class="inv-stats">
            <div class="inv-stat-card">
                <div class="inv-stat-label">Total Units</div>
                <div class="inv-stat-val"><?= number_format($total_stock) ?></div>
            </div>
            <div class="inv-stat-card" style="border-color: #C8E6C9;">
                <div class="inv-stat-label">Healthy Stock</div>
                <div class="inv-stat-val" style="color: #2E7D32;"><?= $healthy ?></div>
                <div class="inv-stat-sub">products</div>
            </div>
            <div class="inv-stat-card" style="border-color: #FFCC80;">
                <div class="inv-stat-label">Low Stock</div>
                <div class="inv-stat-val" style="color: #E65100;"><?= $low_stock ?></div>
                <div class="inv-stat-sub">products — reorder soon</div>
            </div>
            <div class="inv-stat-card" style="border-color: #FFCDD2;">
                <div class="inv-stat-label">Out of Stock</div>
                <div class="inv-stat-val" style="color: #C62828;"><?= $out_of_stock ?></div>
                <div class="inv-stat-sub">products — action needed</div>
            </div>
        </div>

        <!-- Batch Receive Bar -->
        <div class="batch-bar" id="batchBar">
            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>
            <span>Batch Receive PO — add stock to all selected items:</span>
            <input type="number" class="batch-input" id="batchQty" placeholder="Qty" min="1">
            <button class="batch-btn" onclick="applyBatch()">Apply to Selected</button>
            <button style="background:transparent; border:none; color:rgba(255,255,255,0.7); cursor:pointer; margin-left:auto;" onclick="toggleBatch()">Dismiss</button>
        </div>

        <!-- Inventory Table -->
        <div class="card" style="padding:0; overflow:hidden;">
            <table class="inv-table">
                <thead>
                    <tr>
                        <th><input type="checkbox" id="selectAll" onchange="toggleAll(this)"></th>
                        <th>Product</th>
                        <th>Stock Health</th>
                        <th>Current Stock</th>
                        <th>30-Day Velocity</th>
                        <th>Days of Stock Left</th>
                        <th>Quick Adjust</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach($books as $b):
                        $stock = (int)$b['inventory_count'];
                        $sold30 = (float)($velocity[$b['id']] ?? 0);
                        $dailyRate = $sold30 / 30;
                        $daysLeft = $dailyRate > 0 ? floor($stock / $dailyRate) : null;
                        $pct = min(100, round($stock / MAX_STOCK * 100));
                        $reorderPct = round(REORDER_POINT / MAX_STOCK * 100);
                        $barColor = $stock > REORDER_POINT ? '#2E7D32' : ($stock > 0 ? '#F57F17' : '#C62828');
                        if ($daysLeft === null) { $fClass = 'forecast-none'; $fText = 'No sales data'; }
                        elseif ($daysLeft > 30) { $fClass = 'forecast-ok'; $fText = "> 30 days"; }
                        elseif ($daysLeft > 7) { $fClass = 'forecast-warn'; $fText = "$daysLeft days"; }
                        else { $fClass = 'forecast-crit'; $fText = "$daysLeft days"; }
                    ?>
                    <tr id="row-<?= $b['id'] ?>">
                        <td><input type="checkbox" class="row-select" value="<?= $b['id'] ?>"></td>
                        <td>
                            <div style="display:flex; align-items:center; gap:12px;">
                                <?php if($b['cover_url']): ?>
                                <img src="<?= $b['cover_url'] ?>" style="width:32px; height:44px; object-fit:cover; border-radius:4px;">
                                <?php else: ?>
                                <div style="width:32px; height:44px; background:var(--bg-cream); border-radius:4px;"></div>
                                <?php endif; ?>
                                <div>
                                    <div style="font-weight:700; font-size:14px; max-width:220px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;"><?= htmlspecialchars($b['title']) ?></div>
                                    <div style="font-size:12px; color:var(--text-muted);"><?= htmlspecialchars($b['author']) ?></div>
                                </div>
                            </div>
                        </td>
                        <td style="min-width:180px;">
                            <div class="health-bar-outer">
                                <div class="health-bar-fill" style="width:<?= $pct ?>%; background:<?= $barColor ?>;"></div>
                                <div class="reorder-marker" style="left:<?= $reorderPct ?>%;"></div>
                            </div>
                            <div style="font-size:10px; color:var(--text-muted); margin-top:4px;">Reorder at <?= REORDER_POINT ?> units</div>
                        </td>
                        <td>
                            <span id="count-<?= $b['id'] ?>" style="font-size:20px; font-weight:800; color:<?= $stock <= 0 ? '#C62828' : ($stock <= REORDER_POINT ? '#E65100' : 'var(--text-main)') ?>;"><?= $stock ?></span>
                        </td>
                        <td style="font-size:14px; color:var(--text-muted);">
                            <?= $sold30 > 0 ? number_format($sold30, 1) . ' / month' : '—' ?>
                        </td>
                        <td>
                            <span class="forecast-badge <?= $fClass ?>"><?= $fText ?></span>
                        </td>
                        <td>
                            <div class="stepper">
                                <button class="stepper-btn" onclick="adjustStock('<?= $b['id'] ?>', -1)">−</button>
                                <input type="number" class="stepper-val" id="stepper-<?= $b['id'] ?>" value="<?= $stock ?>" onchange="setStock('<?= $b['id'] ?>', this.value)">
                                <button class="stepper-btn" onclick="adjustStock('<?= $b['id'] ?>', 1)">+</button>
                            </div>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </main>

    <script>
        let selectedItems = new Set();
        let batchVisible = false;

        function toggleAll(cb) {
            document.querySelectorAll('.row-select').forEach(c => {
                c.checked = cb.checked;
                if(cb.checked) selectedItems.add(c.value);
                else selectedItems.delete(c.value);
            });
        }

        document.querySelectorAll('.row-select').forEach(c => {
            c.addEventListener('change', () => {
                if(c.checked) selectedItems.add(c.value);
                else selectedItems.delete(c.value);
            });
        });

        function adjustStock(id, delta) {
            const display = document.getElementById('count-' + id);
            const stepper = document.getElementById('stepper-' + id);
            fetch('inventory.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=update_stock&id=${id}&delta=${delta}`
            }).then(r => r.json()).then(d => {
                if(d.ok) { display.textContent = d.new_count; stepper.value = d.new_count; }
            });
        }

        function setStock(id, value) {
            fetch('inventory.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=set_stock&id=${id}&value=${value}`
            });
        }

        function toggleBatch() {
            batchVisible = !batchVisible;
            document.getElementById('batchBar').classList.toggle('show', batchVisible);
        }

        function applyBatch() {
            const qty = parseInt(document.getElementById('batchQty').value);
            if (!qty || !selectedItems.size) { alert('Select items and enter a quantity.'); return; }
            const promises = [...selectedItems].map(id =>
                fetch('inventory.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                    body: `action=update_stock&id=${id}&delta=${qty}`
                }).then(r => r.json())
            );
            Promise.all(promises).then(() => location.reload());
        }
    </script>
</body>
</html>
