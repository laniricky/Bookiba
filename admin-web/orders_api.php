<?php
require 'db.php';

$action = $_REQUEST['action'] ?? '';
$order_id = $_REQUEST['id'] ?? '';

if (!$order_id) {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        echo json_encode(['ok' => false, 'error' => 'No order ID']);
    } else {
        echo 'Error: No order ID';
    }
    exit;
}

if ($action === 'details') {
    // Render the order details HTML
    $stmt = $pdo->prepare("SELECT o.*, u.name as user_name, u.email as user_email 
                           FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?");
    $stmt->execute([$order_id]);
    $order = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$order) {
        echo '<div style="color:red; padding:20px;">Order not found.</div>';
        exit;
    }

    $stmt = $pdo->prepare("SELECT oi.*, b.title, b.cover_url 
                           FROM order_items oi JOIN books b ON oi.book_id = b.id WHERE oi.order_id = ?");
    $stmt->execute([$order_id]);
    $items = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Determine badge class
    $badge_class = 'badge-processing';
    if ($order['status'] === 'Shipped') $badge_class = 'badge-shipped';
    if ($order['status'] === 'Delivered') $badge_class = 'badge-delivered';
    if ($order['status'] === 'Cancelled') $badge_class = 'badge-rare';
    if ($order['status'] === 'Pending') $badge_class = 'badge';

    ?>
    <div style="margin-bottom: 24px;">
        <div style="display:flex; justify-content:space-between; margin-bottom: 8px;">
            <div style="font-weight: 600; font-size: 16px;"><?= htmlspecialchars($order['user_name']) ?></div>
            <span id="drawer-status-badge" class="badge <?= $badge_class ?>"><?= htmlspecialchars($order['status']) ?></span>
        </div>
        <div style="font-size: 14px; color: var(--warm-brown);"><?= htmlspecialchars($order['user_email']) ?></div>
        <div style="font-size: 13px; color: var(--muted-brown); margin-top: 4px;">Placed on: <?= date('M j, Y, g:i a', strtotime($order['created_at'])) ?></div>
    </div>

    <div class="form-section">Shipping Information</div>
    <div style="background: var(--cream); padding: 16px; border-radius: 6px; border: 1px solid var(--border); font-size: 14px; line-height: 1.6; margin-bottom: 24px;">
        <strong>Address:</strong><br>
        <?= nl2br(htmlspecialchars($order['shipping_address'])) ?><br><br>
        <strong>Payment Method:</strong> <?= htmlspecialchars($order['payment_method']) ?>
    </div>

    <div class="form-section">Order Items</div>
    <div style="margin-bottom: 24px;">
        <?php foreach($items as $item): ?>
            <div style="display:flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--border); align-items: center;">
                <div style="width:40px; height:56px; background:var(--border); border-radius:3px; overflow:hidden; flex-shrink:0;">
                    <?php if($item['cover_url']): ?>
                        <img src="<?= htmlspecialchars($item['cover_url']) ?>" style="width:100%; height:100%; object-fit:cover;">
                    <?php endif; ?>
                </div>
                <div style="flex:1;">
                    <div style="font-weight: 600; font-size: 14px; color: var(--dark-brown);"><?= htmlspecialchars($item['title']) ?></div>
                    <div style="font-size: 13px; color: var(--warm-brown);">Qty: <?= $item['quantity'] ?></div>
                </div>
                <div style="font-weight: 600; font-size: 14px;">
                    Ksh <?= number_format($item['price_ksh'] * $item['quantity']) ?>
                </div>
            </div>
        <?php endforeach; ?>
        
        <div style="display:flex; justify-content:space-between; padding: 16px 0; font-weight: 700; font-size: 16px; color: var(--dark-brown);">
            <div>Total</div>
            <div>Ksh <?= number_format($order['total_amount']) ?></div>
        </div>
    </div>

    <div class="form-section">Actions</div>
    <div class="quick-actions">
        <?php if($order['status'] === 'Pending'): ?>
            <button class="quick-action" onclick="updateOrderStatus('<?= $order['id'] ?>', 'Processing')">
                <span class="quick-action-icon">📦</span> Start Processing
            </button>
            <button class="quick-action" onclick="updateOrderStatus('<?= $order['id'] ?>', 'Cancelled')">
                <span class="quick-action-icon">❌</span> Cancel Order
            </button>
        <?php elseif($order['status'] === 'Processing'): ?>
            <button class="quick-action" onclick="updateOrderStatus('<?= $order['id'] ?>', 'Shipped')">
                <span class="quick-action-icon">🚚</span> Mark as Shipped
            </button>
        <?php elseif($order['status'] === 'Shipped'): ?>
            <button class="quick-action" onclick="updateOrderStatus('<?= $order['id'] ?>', 'Delivered')">
                <span class="quick-action-icon">✅</span> Mark as Delivered
            </button>
        <?php endif; ?>
    </div>
    
    <?php if(in_array($order['status'], ['Delivered', 'Cancelled'])): ?>
        <div style="text-align:center; padding: 16px; color: var(--warm-brown); font-size: 13px;">
            This order has reached its final state.
        </div>
    <?php endif; ?>
    
    <?php
    exit;
}

if ($action === 'update_status' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $status = $_POST['status'] ?? '';
    $allowed = ['Pending', 'Processing', 'Shipped', 'Delivered', 'Cancelled'];
    
    if (!in_array($status, $allowed)) {
        echo json_encode(['ok' => false, 'error' => 'Invalid status']);
        exit;
    }

    $stmt = $pdo->prepare("UPDATE orders SET status = ? WHERE id = ?");
    $result = $stmt->execute([$status, $order_id]);

    header('Content-Type: application/json');
    echo json_encode(['ok' => $result]);
    exit;
}

echo "Invalid action.";
