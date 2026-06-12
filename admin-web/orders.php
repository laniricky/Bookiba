<?php
require 'db.php';
require 'includes/auth_gate.php';

// Handle AJAX status update
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    header('Content-Type: application/json');
    if ($_POST['action'] === 'update_status') {
        $stmt = $pdo->prepare("UPDATE orders SET status = ? WHERE id = ?");
        $stmt->execute([$_POST['status'], $_POST['id']]);
        echo json_encode(['ok' => true]);
    } elseif ($_POST['action'] === 'bulk_update') {
        $ids = json_decode($_POST['ids'], true) ?: [];
        $status = $_POST['status'];
        foreach ($ids as $id) {
            $stmt = $pdo->prepare("UPDATE orders SET status = ? WHERE id = ?");
            $stmt->execute([$status, $id]);
        }
        echo json_encode(['ok' => true, 'updated' => count($ids)]);
    }
    exit;
}

// Fetch all orders with aggregated data
$search = $_GET['q'] ?? '';
$status_filter = $_GET['status'] ?? '';

$where = [];
$params = [];
if ($search) { $where[] = "(u.name LIKE ? OR o.id LIKE ?)"; $params[] = "%$search%"; $params[] = "%$search%"; }
if ($status_filter) { $where[] = "o.status = ?"; $params[] = $status_filter; }
$where_sql = $where ? "WHERE " . implode(' AND ', $where) : "";

$stmt = $pdo->prepare("
    SELECT o.*, u.name as customer_name, u.email as customer_email,
           COUNT(DISTINCT oi.book_id) as item_count,
           SUM(oi.quantity) as total_qty
    FROM orders o
    JOIN users u ON o.user_id = u.id
    LEFT JOIN order_items oi ON oi.order_id = o.id
    $where_sql
    GROUP BY o.id, u.name, u.email
    ORDER BY o.created_at DESC
");
$stmt->execute($params);
$orders = $stmt->fetchAll(PDO::FETCH_ASSOC);

// Status counts for filter pills
$status_counts = $pdo->query("SELECT status, COUNT(*) as cnt FROM orders GROUP BY status")->fetchAll(PDO::FETCH_KEY_PAIR);
$all_count = array_sum($status_counts);

// Get first selected order's details
$selected_id = $_GET['view'] ?? ($orders[0]['id'] ?? null);
$selected_order = null;
$selected_items = [];
$customer_stats = null;
$order_history = [];

if ($selected_id) {
    $stmt = $pdo->prepare("SELECT o.*, u.name as customer_name, u.email as customer_email, u.id as customer_id FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?");
    $stmt->execute([$selected_id]);
    $selected_order = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($selected_order) {
        $stmt = $pdo->prepare("SELECT oi.*, b.title, b.author, b.cover_url FROM order_items oi JOIN books b ON oi.book_id = b.id WHERE oi.order_id = ?");
        $stmt->execute([$selected_id]);
        $selected_items = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Customer stats
        $stmt = $pdo->prepare("SELECT COUNT(*) as total_orders, SUM(total_amount) as total_spent, MAX(created_at) as last_order FROM orders WHERE user_id = ? AND status != 'Cancelled'");
        $stmt->execute([$selected_order['customer_id']]);
        $customer_stats = $stmt->fetch(PDO::FETCH_ASSOC);

        // Recent order history for this customer
        $stmt = $pdo->prepare("SELECT id, total_amount, status, created_at FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 5");
        $stmt->execute([$selected_order['customer_id']]);
        $order_history = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
}

$statuses = ['Pending', 'Processing', 'Shipped', 'Delivered', 'Cancelled'];

function timelineSteps($status) {
    $all = ['Pending', 'Processing', 'Shipped', 'Delivered'];
    $idx = array_search($status, $all);
    return [$all, $idx];
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Orders | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <style>
        body { overflow: hidden; }
        .orders-layout { display: grid; grid-template-columns: 340px 1fr; height: calc(100vh - 0px); overflow: hidden; }

        /* LEFT PANE */
        .orders-list-pane { border-right: 1px solid var(--border-color); display: flex; flex-direction: column; overflow: hidden; background: var(--card-white); }
        .orders-pane-header { padding: 20px 20px 0; flex-shrink: 0; }
        .orders-pane-title { font-size: 20px; font-weight: 800; margin-bottom: 14px; display: flex; align-items: center; justify-content: space-between; }
        .orders-search { display: flex; align-items: center; gap: 8px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 8px 12px; margin-bottom: 14px; background: var(--bg-cream); }
        .orders-search input { border: none; background: transparent; outline: none; font-size: 13px; width: 100%; font-family: inherit; }
        .status-pills { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 14px; }
        .status-pill { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; cursor: pointer; border: 1px solid var(--border-color); background: white; color: var(--text-muted); transition: all 0.15s; white-space: nowrap; }
        .status-pill.active { background: var(--accent-green); color: white; border-color: var(--accent-green); }
        .bulk-bar { padding: 8px 12px; background: var(--accent-green); color: white; font-size: 13px; font-weight: 600; display: none; align-items: center; gap: 12px; flex-shrink: 0; }
        .bulk-bar.show { display: flex; }
        .bulk-action-btn { padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.4); background: transparent; color: white; font-size: 12px; font-weight: 700; cursor: pointer; }
        .orders-scroll { overflow-y: auto; flex: 1; }
        .order-row { display: flex; align-items: flex-start; gap: 12px; padding: 14px 20px; border-bottom: 1px solid var(--border-color); cursor: pointer; transition: background 0.15s; position: relative; }
        .order-row:hover { background: var(--bg-cream); }
        .order-row.active { background: #EFF7EE; border-right: 3px solid var(--accent-green); }
        .order-row-check { width: 16px; height: 16px; border-radius: 4px; border: 2px solid var(--border-color); flex-shrink: 0; margin-top: 2px; cursor: pointer; accent-color: var(--accent-green); }
        .order-row-avatar { width: 36px; height: 36px; border-radius: 50%; background: #E8F5E9; color: var(--accent-green); font-weight: 800; font-size: 14px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
        .order-row-body { flex: 1; min-width: 0; }
        .order-row-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 4px; }
        .order-row-name { font-weight: 700; font-size: 14px; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .order-row-time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; margin-left: 8px; }
        .order-row-preview { font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .order-row-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
        .order-row-amount { font-size: 13px; font-weight: 700; color: var(--text-main); }
        .order-status-dot { width: 8px; height: 8px; border-radius: 50%; }
        .dot-Pending { background: #F57F17; }
        .dot-Processing { background: #1565C0; }
        .dot-Shipped { background: #2E7D32; }
        .dot-Delivered { background: #9E9E9E; }
        .dot-Cancelled { background: #C62828; }

        /* RIGHT DETAIL PANE */
        .order-detail-pane { overflow-y: auto; background: var(--bg-cream); display: flex; flex-direction: column; }
        .detail-empty { display: flex; align-items: center; justify-content: center; height: 100%; flex-direction: column; gap: 16px; color: var(--text-muted); }
        .detail-header { padding: 24px 32px; background: var(--card-white); border-bottom: 1px solid var(--border-color); display: flex; align-items: flex-start; justify-content: space-between; flex-shrink: 0; }
        .detail-order-id { font-size: 13px; color: var(--text-muted); margin-bottom: 4px; font-family: monospace; }
        .detail-order-title { font-size: 22px; font-weight: 800; margin-bottom: 8px; }
        .detail-order-date { font-size: 13px; color: var(--text-muted); }
        .detail-actions { display: flex; gap: 12px; }
        .btn-sm { padding: 8px 16px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 700; cursor: pointer; border: none; display: flex; align-items: center; gap: 6px; }
        .btn-sm-primary { background: var(--accent-green); color: white; }
        .btn-sm-ghost { background: transparent; border: 1px solid var(--border-color); color: var(--text-main); }
        .detail-body { padding: 24px 32px; display: grid; grid-template-columns: 1fr 300px; gap: 24px; flex: 1; }
        .detail-section { background: var(--card-white); border-radius: var(--radius-lg); border: 1px solid var(--border-color); overflow: hidden; }
        .detail-section-header { padding: 16px 20px; border-bottom: 1px solid var(--border-color); font-weight: 700; font-size: 14px; display: flex; align-items: center; gap: 8px; }
        .detail-section-body { padding: 20px; }

        /* Order Timeline */
        .timeline { display: flex; align-items: center; gap: 0; margin: 8px 0; }
        .timeline-step { display: flex; flex-direction: column; align-items: center; flex: 1; }
        .timeline-dot { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 2px solid var(--border-color); background: white; z-index: 1; }
        .timeline-dot.done { background: var(--accent-green); border-color: var(--accent-green); color: white; }
        .timeline-dot.current { background: white; border-color: var(--accent-green); color: var(--accent-green); }
        .timeline-line { flex: 1; height: 2px; background: var(--border-color); }
        .timeline-line.done { background: var(--accent-green); }
        .timeline-label { font-size: 11px; color: var(--text-muted); margin-top: 6px; font-weight: 600; }
        .timeline-label.done, .timeline-label.current { color: var(--accent-green); }

        /* Items table */
        .items-table { width: 100%; border-collapse: collapse; }
        .items-table td { padding: 10px 0; border-bottom: 1px solid var(--border-color); vertical-align: middle; }
        .items-table tr:last-child td { border-bottom: none; }
        .item-cover { width: 36px; height: 50px; object-fit: cover; border-radius: 4px; background: var(--bg-cream); margin-right: 12px; }

        /* Customer card */
        .customer-stat { padding: 10px 0; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; font-size: 14px; }
        .customer-stat:last-child { border-bottom: none; }
        .customer-stat-label { color: var(--text-muted); }
        .customer-stat-value { font-weight: 700; }

        /* Status badge */
        .status-chip { display: inline-flex; align-items: center; gap: 5px; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
        .chip-Pending { background: #FFF3E0; color: #E65100; }
        .chip-Processing { background: #E3F2FD; color: #1565C0; }
        .chip-Shipped { background: #E8F5E9; color: #2E7D32; }
        .chip-Delivered { background: #F5F5F5; color: #616161; }
        .chip-Cancelled { background: #FFEBEE; color: #C62828; }

        .order-total-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }
        .order-total-row.grand { font-weight: 800; font-size: 16px; border-top: 2px solid var(--border-color); margin-top: 4px; padding-top: 12px; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1; padding: 0; overflow: hidden;">
        <div class="orders-layout">

            <!-- LEFT PANE: Order List -->
            <div class="orders-list-pane">
                <div class="orders-pane-header">
                    <div class="orders-pane-title">
                        Orders
                        <span style="font-size: 13px; color: var(--text-muted); font-weight: 400;"><?= $all_count ?> total</span>
                    </div>
                    <div class="orders-search">
                        <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                        <input type="text" id="orderSearch" placeholder="Search by customer or ID…" value="<?= htmlspecialchars($search) ?>">
                    </div>
                    <div class="status-pills">
                        <span class="status-pill <?= !$status_filter ? 'active' : '' ?>" onclick="filterOrders('')">All <strong><?= $all_count ?></strong></span>
                        <?php foreach($statuses as $s): $cnt = $status_counts[$s] ?? 0; ?>
                        <span class="status-pill <?= $status_filter === $s ? 'active' : '' ?>" onclick="filterOrders('<?= $s ?>')"><?= $s ?> <strong><?= $cnt ?></strong></span>
                        <?php endforeach; ?>
                    </div>
                </div>

                <div class="bulk-bar" id="bulkBar">
                    <span id="bulkCount">0 selected</span>
                    <?php foreach(['Processing','Shipped','Delivered','Cancelled'] as $s): ?>
                    <button class="bulk-action-btn" onclick="bulkUpdate('<?= $s ?>')">Mark <?= $s ?></button>
                    <?php endforeach; ?>
                    <button class="bulk-action-btn" onclick="clearSelection()" style="margin-left: auto;">Clear</button>
                </div>

                <div class="orders-scroll" id="ordersList">
                    <?php foreach($orders as $o): ?>
                    <div class="order-row <?= $o['id'] === $selected_id ? 'active' : '' ?>" id="row-<?= $o['id'] ?>" onclick="selectOrder('<?= $o['id'] ?>', event)">
                        <input type="checkbox" class="order-row-check" onclick="toggleSelect('<?= $o['id'] ?>', event)">
                        <div class="order-row-avatar"><?= strtoupper(substr($o['customer_name'],0,1)) ?></div>
                        <div class="order-row-body">
                            <div class="order-row-top">
                                <span class="order-row-name"><?= htmlspecialchars($o['customer_name']) ?></span>
                                <span class="order-row-time"><?= date('M j', strtotime($o['created_at'])) ?></span>
                            </div>
                            <div class="order-row-preview"><?= $o['item_count'] ?> item<?= $o['item_count'] != 1 ? 's' : '' ?> · #<?= substr($o['id'],0,8) ?></div>
                            <div class="order-row-meta">
                                <span class="order-row-amount">Ksh <?= number_format($o['total_amount']) ?></span>
                                <div style="display:flex; align-items:center; gap:5px;">
                                    <div class="order-status-dot dot-<?= $o['status'] ?>"></div>
                                    <span style="font-size:11px; color:var(--text-muted);"><?= $o['status'] ?></span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <?php endforeach; ?>
                    <?php if(empty($orders)): ?>
                    <div style="padding:40px; text-align:center; color:var(--text-muted);">No orders found.</div>
                    <?php endif; ?>
                </div>
            </div>

            <!-- RIGHT PANE: Order Detail -->
            <div class="order-detail-pane" id="detailPane">
                <?php if (!$selected_order): ?>
                <div class="detail-empty">
                    <svg width="64" height="64" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="opacity:0.2;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>
                    <span>Select an order to view details</span>
                </div>
                <?php else:
                    [$steps, $currentIdx] = timelineSteps($selected_order['status']);
                    $isCancelled = $selected_order['status'] === 'Cancelled';
                ?>
                <div class="detail-header">
                    <div>
                        <div class="detail-order-id">Order #<?= $selected_order['id'] ?></div>
                        <div class="detail-order-title"><?= htmlspecialchars($selected_order['customer_name']) ?></div>
                        <div class="detail-order-date">
                            <span class="status-chip chip-<?= $selected_order['status'] ?>">
                                <div class="order-status-dot dot-<?= $selected_order['status'] ?>"></div>
                                <?= $selected_order['status'] ?>
                            </span>
                            &nbsp; Placed <?= date('M j, Y · g:i a', strtotime($selected_order['created_at'])) ?>
                        </div>
                    </div>
                    <div class="detail-actions">
                        <?php if($selected_order['status'] === 'Pending'): ?>
                            <button class="btn-sm btn-sm-ghost" onclick="quickUpdate('<?= $selected_order['id'] ?>', 'Cancelled')">
                                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                                Cancel
                            </button>
                            <button class="btn-sm btn-sm-primary" onclick="quickUpdate('<?= $selected_order['id'] ?>', 'Processing')">
                                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                                Process Order
                            </button>
                        <?php elseif($selected_order['status'] === 'Processing'): ?>
                            <button class="btn-sm btn-sm-primary" onclick="quickUpdate('<?= $selected_order['id'] ?>', 'Shipped')">
                                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0"/></svg>
                                Mark as Shipped
                            </button>
                        <?php elseif($selected_order['status'] === 'Shipped'): ?>
                            <button class="btn-sm btn-sm-primary" onclick="quickUpdate('<?= $selected_order['id'] ?>', 'Delivered')">
                                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                                Mark Delivered
                            </button>
                        <?php endif; ?>
                    </div>
                </div>

                <div class="detail-body">
                    <div style="display: flex; flex-direction: column; gap: 20px;">

                        <?php if(!$isCancelled): ?>
                        <!-- Timeline -->
                        <div class="detail-section">
                            <div class="detail-section-header">
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/></svg>
                                Order Progress
                            </div>
                            <div class="detail-section-body">
                                <div class="timeline">
                                    <?php foreach($steps as $i => $step):
                                        $done = $i < $currentIdx;
                                        $current = $i === $currentIdx;
                                    ?>
                                    <?php if($i > 0): ?>
                                    <div class="timeline-line <?= $done ? 'done' : '' ?>"></div>
                                    <?php endif; ?>
                                    <div class="timeline-step">
                                        <div class="timeline-dot <?= $done ? 'done' : ($current ? 'current' : '') ?>">
                                            <?php if($done): ?>
                                            <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
                                            <?php else: ?>
                                            <div style="width:8px; height:8px; border-radius:50%; background: <?= $current ? 'var(--accent-green)' : 'var(--border-color)' ?>;"></div>
                                            <?php endif; ?>
                                        </div>
                                        <div class="timeline-label <?= $done ? 'done' : ($current ? 'current' : '') ?>"><?= $step ?></div>
                                    </div>
                                    <?php endforeach; ?>
                                </div>
                            </div>
                        </div>
                        <?php endif; ?>

                        <!-- Items -->
                        <div class="detail-section">
                            <div class="detail-section-header">
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/></svg>
                                Items (<?= count($selected_items) ?>)
                            </div>
                            <div class="detail-section-body" style="padding: 0 20px;">
                                <table class="items-table">
                                    <?php foreach($selected_items as $item): ?>
                                    <tr>
                                        <td>
                                            <div style="display:flex; align-items:center; gap:12px;">
                                                <?php if($item['cover_url']): ?>
                                                <img src="<?= $item['cover_url'] ?>" class="item-cover">
                                                <?php else: ?>
                                                <div class="item-cover"></div>
                                                <?php endif; ?>
                                                <div>
                                                    <div style="font-weight:600; font-size:14px;"><?= htmlspecialchars($item['title']) ?></div>
                                                    <div style="font-size:12px; color:var(--text-muted);"><?= htmlspecialchars($item['author']) ?></div>
                                                </div>
                                            </div>
                                        </td>
                                        <td style="text-align:center; color:var(--text-muted); font-size:13px;">×<?= $item['quantity'] ?></td>
                                        <td style="text-align:right; font-weight:600; font-size:14px;">Ksh <?= number_format($item['price_ksh'] * $item['quantity']) ?></td>
                                    </tr>
                                    <?php endforeach; ?>
                                </table>
                                <div style="border-top: 1px solid var(--border-color); padding: 16px 0 8px;">
                                    <div class="order-total-row"><span style="color:var(--text-muted)">Subtotal</span><span>Ksh <?= number_format($selected_order['total_amount']) ?></span></div>
                                    <div class="order-total-row"><span style="color:var(--text-muted)">Delivery</span><span style="color: var(--accent-green);">Free</span></div>
                                    <div class="order-total-row grand"><span>Total</span><span>Ksh <?= number_format($selected_order['total_amount']) ?></span></div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Right sidebar of detail -->
                    <div style="display: flex; flex-direction: column; gap: 20px;">
                        <!-- Customer info -->
                        <div class="detail-section">
                            <div class="detail-section-header">
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
                                Customer
                            </div>
                            <div class="detail-section-body">
                                <div style="display:flex; align-items:center; gap:12px; margin-bottom: 16px;">
                                    <div style="width:44px; height:44px; border-radius:50%; background:#E8F5E9; color:var(--accent-green); font-weight:800; font-size:18px; display:flex; align-items:center; justify-content:center;">
                                        <?= strtoupper(substr($selected_order['customer_name'],0,1)) ?>
                                    </div>
                                    <div>
                                        <div style="font-weight:700;"><?= htmlspecialchars($selected_order['customer_name']) ?></div>
                                        <div style="font-size:12px; color:var(--text-muted);"><?= htmlspecialchars($selected_order['customer_email']) ?></div>
                                    </div>
                                </div>
                                <?php if($customer_stats): ?>
                                <div class="customer-stat"><span class="customer-stat-label">Total Orders</span><span class="customer-stat-value"><?= $customer_stats['total_orders'] ?></span></div>
                                <div class="customer-stat"><span class="customer-stat-label">Lifetime Value</span><span class="customer-stat-value">Ksh <?= number_format($customer_stats['total_spent'] ?: 0) ?></span></div>
                                <div class="customer-stat"><span class="customer-stat-label">Avg. Order</span><span class="customer-stat-value">Ksh <?= $customer_stats['total_orders'] > 0 ? number_format(($customer_stats['total_spent'] ?: 0) / $customer_stats['total_orders']) : '—' ?></span></div>
                                <?php endif; ?>
                            </div>
                        </div>

                        <!-- Recent order history -->
                        <?php if(!empty($order_history)): ?>
                        <div class="detail-section">
                            <div class="detail-section-header">
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                                Order History
                            </div>
                            <div class="detail-section-body" style="padding: 8px 20px;">
                                <?php foreach($order_history as $h): ?>
                                <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px solid var(--border-color); font-size:13px;">
                                    <div>
                                        <div style="font-weight:600; font-family:monospace;">#<?= substr($h['id'],0,8) ?></div>
                                        <div style="color:var(--text-muted);"><?= date('M j, Y', strtotime($h['created_at'])) ?></div>
                                    </div>
                                    <div style="text-align:right;">
                                        <div style="font-weight:700;">Ksh <?= number_format($h['total_amount']) ?></div>
                                        <span class="status-chip chip-<?= $h['status'] ?>" style="font-size:10px;"><?= $h['status'] ?></span>
                                    </div>
                                </div>
                                <?php endforeach; ?>
                            </div>
                        </div>
                        <?php endif; ?>
                    </div>
                </div>
                <?php endif; ?>
            </div>
        </div>
    </main>

    <script>
        const selectedIds = new Set();
        let currentFilter = '<?= $status_filter ?>';

        function selectOrder(id, e) {
            if (e.target.type === 'checkbox') return;
            const url = new URL(window.location);
            url.searchParams.set('view', id);
            window.location.href = url.toString();
        }

        function filterOrders(status) {
            const url = new URL(window.location);
            if (status) url.searchParams.set('status', status);
            else url.searchParams.delete('status');
            url.searchParams.delete('view');
            window.location.href = url.toString();
        }

        function toggleSelect(id, e) {
            e.stopPropagation();
            if (selectedIds.has(id)) selectedIds.delete(id);
            else selectedIds.add(id);
            updateBulkBar();
        }

        function clearSelection() {
            selectedIds.clear();
            document.querySelectorAll('.order-row-check').forEach(c => c.checked = false);
            updateBulkBar();
        }

        function updateBulkBar() {
            const bar = document.getElementById('bulkBar');
            const cnt = document.getElementById('bulkCount');
            if (selectedIds.size > 0) { bar.classList.add('show'); cnt.textContent = selectedIds.size + ' selected'; }
            else { bar.classList.remove('show'); }
        }

        function bulkUpdate(status) {
            if (!selectedIds.size) return;
            fetch('orders.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'action=bulk_update&ids=' + encodeURIComponent(JSON.stringify([...selectedIds])) + '&status=' + encodeURIComponent(status)
            }).then(r => r.json()).then(d => { if(d.ok) location.reload(); });
        }

        function quickUpdate(id, status) {
            fetch('orders.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'action=update_status&id=' + id + '&status=' + status
            }).then(r => r.json()).then(d => { if(d.ok) location.reload(); });
        }

        // Live search
        document.getElementById('orderSearch').addEventListener('input', function() {
            const q = this.value;
            const url = new URL(window.location);
            if (q) url.searchParams.set('q', q);
            else url.searchParams.delete('q');
            // Debounce
            clearTimeout(this._t);
            this._t = setTimeout(() => window.location.href = url.toString(), 400);
        });
    </script>
</body>
</html>

