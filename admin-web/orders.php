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
    <title>Activity Feed</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>

    <!-- STICKY SIDEBAR -->
    <div class="sidebar-container">
        <aside class="sidebar">
            <div class="sidebar-logo">
                <span style="font-size:28px">📚</span> Bookiba
            </div>
            <nav>
                <a href="index.php"><span class="icon">🏠</span> Home Feed</a>
                <a href="orders.php" class="active"><span class="icon">🔔</span> Activity</a>
                <a href="analytics.php"><span class="icon">📈</span> Insights</a>
            </nav>
            <div class="user-profile-mini">
                <div class="user-avatar">A</div>
                <div>
                    <div style="font-weight:700; font-size:14px; color:var(--dark-brown)">Admin</div>
                    <div style="font-size:12px; color:var(--warm-brown)">@bookiba_hq</div>
                </div>
            </div>
        </aside>
    </div>

    <!-- MAIN FEED -->
    <main class="main-content">
        <div class="page-title">
            Activity
            <div style="display:flex; gap: 8px;">
                <form method="GET">
                    <select name="status" onchange="this.form.submit()" style="padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; border: 1px solid var(--border); outline: none;">
                        <option value="">All Activity</option>
                        <?php foreach($statuses as $s): ?>
                            <option value="<?= $s ?>" <?= $status_filter === $s ? 'selected' : '' ?>><?= $s ?></option>
                        <?php endforeach; ?>
                    </select>
                </form>
            </div>
        </div>

        <div class="feed-container">
            <?php if (empty($orders)): ?>
                <div style="text-align: center; padding: 40px; color: var(--warm-brown);">
                    <div style="font-size: 40px; margin-bottom: 12px;">📭</div>
                    <div style="font-weight: 700;">No activity found</div>
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
                        <div class="feed-user"><?= htmlspecialchars($o['user_name']) ?> <span style="font-weight:400; color:var(--warm-brown); font-size:14px;">@<?= strtolower(str_replace(' ', '', $o['user_name'])) ?></span></div>
                        <div class="feed-time"><?= $time_ago ?></div>
                    </div>
                    
                    <div class="feed-text">
                        Placed a new order for <strong><?= $o['item_count'] ?> item(s)</strong> totaling <strong>Ksh <?= number_format($o['total_amount']) ?></strong>.
                        <br>
                        <span style="font-size:13px; color:var(--warm-brown);">Current status: <span class="status-badge status-<?= $status ?> id="status-text-<?= $o['id'] ?>"><?= $status ?></span></span>
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
                if(d.ok) window.location.reload(); // Quickest way to refresh feed state
            });
        }
    </script>
</body>
</html>
