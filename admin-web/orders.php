<?php
require 'db.php';

// Fetch Orders
$status_filter = $_GET['status'] ?? '';
$where = []; $params = [];
if ($status_filter) { $where[] = "o.status = ?"; $params[] = $status_filter; }
$where_sql = $where ? "WHERE " . implode(' AND ', $where) : "";

// Fetch orders with item counts
$sql = "SELECT o.*, u.name as user_name, u.email as user_email,
        (SELECT COUNT(*) FROM order_items oi WHERE oi.order_id = o.id) as item_count
        FROM orders o 
        JOIN users u ON o.user_id = u.id 
        $where_sql 
        ORDER BY o.created_at DESC";
$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$orders = $stmt->fetchAll(PDO::FETCH_ASSOC);

// Fetch items for thumbnails
$order_items = [];
foreach ($orders as $o) {
    $stmt = $pdo->prepare("SELECT b.cover_url FROM order_items oi JOIN books b ON oi.book_id = b.id WHERE oi.order_id = ? LIMIT 4");
    $stmt->execute([$o['id']]);
    $order_items[$o['id']] = $stmt->fetchAll(PDO::FETCH_COLUMN);
}

$statuses = ['Pending', 'Processing', 'Shipped', 'Delivered', 'Cancelled'];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Orders | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .feed-container { max-width: 700px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; padding-bottom: 60px; }
        .feed-item { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 24px; display: flex; gap: 16px; transition: transform 0.2s, box-shadow 0.2s; }
        .feed-item:hover { box-shadow: 0 12px 24px rgba(0,0,0,0.06); }
        .feed-avatar { width: 44px; height: 44px; border-radius: 50%; background: var(--bg-cream); color: var(--accent-green); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; flex-shrink: 0; }
        .feed-content { flex: 1; }
        .feed-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
        .feed-user { font-weight: 700; font-size: 16px; color: var(--text-main); }
        .feed-time { font-size: 13px; color: var(--text-muted); }
        .feed-text { font-size: 15px; color: var(--text-main); line-height: 1.5; margin-bottom: 16px; }
        .feed-images { display: flex; gap: 12px; margin-bottom: 16px; }
        .feed-img { width: 80px; height: 110px; border-radius: 8px; object-fit: cover; background: var(--bg-cream); border: 1px solid var(--border-color); }
        .feed-actions { display: flex; gap: 12px; border-top: 1px solid var(--border-color); padding-top: 16px; }
        .feed-action-btn { background: none; border: none; font-weight: 600; font-size: 14px; cursor: pointer; display: flex; align-items: center; gap: 6px; color: var(--text-muted); transition: color 0.2s; }
        .feed-action-btn:hover { color: var(--text-main); }
        .feed-action-btn.primary { color: var(--accent-green); }
        .status-badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase; }
        .status-Pending { background: #FFF3E0; color: #E65100; }
        .status-Processing { background: #E3F2FD; color: #1565C0; }
        .status-Shipped { background: #E8F5E9; color: #2E7D32; }
        .status-Delivered { background: #F5F5F5; color: #616161; }
        .status-Cancelled { background: #FFEBEE; color: #C62828; }
    </style>
</head>
<body>

    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; max-width: 700px; margin-left: auto; margin-right: auto;">
            <h1 style="font-size: 24px; font-weight: 700;">Order Activity</h1>
            <form method="GET">
                <select name="status" onchange="this.form.submit()" style="padding: 8px 16px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 600; border: 1px solid var(--border-color); outline: none; background: var(--card-white);">
                    <option value="">All Activity</option>
                    <?php foreach($statuses as $s): ?>
                        <option value="<?= $s ?>" <?= $status_filter === $s ? 'selected' : '' ?>><?= $s ?></option>
                    <?php endforeach; ?>
                </select>
            </form>
        </div>

        <div class="feed-container">
            <?php if (empty($orders)): ?>
                <div style="text-align: center; padding: 60px; color: var(--text-muted);">
                    <svg width="48" height="48" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="margin-bottom:16px; opacity:0.5;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"></path></svg>
                    <div style="font-weight: 700; font-size:18px;">No activity found</div>
                </div>
            <?php endif; ?>

            <?php foreach ($orders as $o): 
                $covers = $order_items[$o['id']];
                $time_ago = date('M j, g:i a', strtotime($o['created_at']));
                $status = $o['status'];
            ?>
            <div class="feed-item" id="order-<?= $o['id'] ?>">
                <div class="feed-avatar">
                    <?= strtoupper(substr($o['user_name'], 0, 1)) ?>
                </div>
                <div class="feed-content">
                    <div class="feed-header">
                        <div class="feed-user"><?= htmlspecialchars($o['user_name']) ?> <span style="font-weight:400; color:var(--text-muted); font-size:14px;">@<?= strtolower(str_replace(' ', '', $o['user_name'])) ?></span></div>
                        <div class="feed-time"><?= $time_ago ?></div>
                    </div>
                    
                    <div class="feed-text">
                        Placed a new order for <strong><?= $o['item_count'] ?> item(s)</strong> totaling <strong>Ksh <?= number_format($o['total_amount']) ?></strong>.
                        <br>
                        <span style="font-size:13px; color:var(--text-muted); margin-top:8px; display:inline-block;">Current status: <span class="status-badge status-<?= $status ?> id="status-text-<?= $o['id'] ?>"><?= $status ?></span></span>
                    </div>

                    <?php if (!empty($covers)): ?>
                        <div class="feed-images">
                            <?php foreach($covers as $c): ?>
                                <?php if($c): ?><img src="<?= htmlspecialchars($c) ?>" class="feed-img"><?php endif; ?>
                            <?php endforeach; ?>
                        </div>
                    <?php endif; ?>

                    <div class="feed-actions">
                        <?php if($status === 'Pending'): ?>
                            <button class="feed-action-btn primary" onclick="updateStatus('<?= $o['id'] ?>', 'Processing')">📦 Process</button>
                            <button class="feed-action-btn" onclick="updateStatus('<?= $o['id'] ?>', 'Cancelled')">❌ Cancel</button>
                        <?php elseif($status === 'Processing'): ?>
                            <button class="feed-action-btn primary" onclick="updateStatus('<?= $o['id'] ?>', 'Shipped')">🚚 Ship Order</button>
                        <?php elseif($status === 'Shipped'): ?>
                            <button class="feed-action-btn primary" onclick="updateStatus('<?= $o['id'] ?>', 'Delivered')">✅ Mark Delivered</button>
                        <?php else: ?>
                            <button class="feed-action-btn" disabled>No actions available</button>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
    </main>

    <script>
        function updateStatus(id, newStatus) {
            fetch('orders_api.php', {
                method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `action=update_status&id=${id}&status=${newStatus}`
            }).then(r=>r.json()).then(d=>{
                if(d.ok) window.location.reload(); 
            });
        }
    </script>
</body>
</html>
