<?php
require 'db.php';

// Form Handling (Slide-over drawer for Add Book)
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['_action'])) {
    $action = $_POST['_action'];
    $id = $_POST['book_id'] ?? '';
    
    if ($action === 'add' && empty($id)) {
        $id = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x', mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000, mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff));
    }

    $title = $_POST['title'] ?? ''; $author = $_POST['author'] ?? ''; $price = (int)($_POST['price_ksh'] ?? 0);
    $cover_url = $_POST['cover_url'] ?? ''; $category = $_POST['category'] ?? ''; $inventory_count = (int)($_POST['inventory_count'] ?? 15);
    $seller_id = '00000000-0000-0000-0000-000000000000';

    if ($action === 'add') {
        $stmt = $pdo->prepare("INSERT INTO books (id, title, author, price_ksh, cover_url, category, inventory_count, seller_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$id, $title, $author, $price, $cover_url, $category, $inventory_count, $seller_id, date('Y-m-d H:i:s')]);
    }
    header("Location: index.php?added=1"); exit;
}

// 1. STATS
$total_revenue = $pdo->query("SELECT SUM(total_amount) FROM orders WHERE status != 'Cancelled'")->fetchColumn() ?: 0;
$total_orders = $pdo->query("SELECT COUNT(*) FROM orders WHERE status != 'Cancelled'")->fetchColumn() ?: 0;
$total_customers = $pdo->query("SELECT COUNT(*) FROM users")->fetchColumn() ?: 0;
$avg_order = $total_orders > 0 ? $total_revenue / $total_orders : 0;

// 2. CHART: Revenue (Last 7 Days)
$rev_data = $pdo->query("SELECT date(created_at) as d, SUM(total_amount) as r FROM orders WHERE status != 'Cancelled' AND created_at >= date('now', '-7 days') GROUP BY date(created_at) ORDER BY d ASC")->fetchAll(PDO::FETCH_ASSOC);
$rev_labels = []; $rev_values = [];
foreach($rev_data as $row) { $rev_labels[] = date('M j', strtotime($row['d'])); $rev_values[] = (int)$row['r']; }

// 3. CHART: Category
$cat_data = $pdo->query("SELECT b.category, SUM(oi.quantity) as q FROM order_items oi JOIN books b ON oi.book_id = b.id JOIN orders o ON oi.order_id = o.id WHERE o.status != 'Cancelled' GROUP BY b.category ORDER BY q DESC")->fetchAll(PDO::FETCH_ASSOC);
$cat_labels = []; $cat_values = [];
foreach($cat_data as $row) { $cat_labels[] = $row['category'] ?: 'Other'; $cat_values[] = (int)$row['q']; }

// 4. TOP SELLING BOOKS
$top_books = $pdo->query("SELECT b.title, b.author, b.cover_url, SUM(oi.quantity) as sold, SUM(oi.quantity * oi.price_ksh) as rev FROM order_items oi JOIN books b ON oi.book_id = b.id JOIN orders o ON oi.order_id = o.id WHERE o.status != 'Cancelled' GROUP BY b.id ORDER BY sold DESC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);

// 5. RECENT ORDERS (Table)
$recent_orders = $pdo->query("SELECT o.*, u.name as customer FROM orders o JOIN users u ON o.user_id = u.id ORDER BY o.created_at DESC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);

// 6. LOW STOCK
$low_stock = $pdo->query("SELECT title, author, cover_url, inventory_count FROM books WHERE inventory_count < 10 ORDER BY inventory_count ASC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);

// 7. CUSTOMER ACTIVITY
$recent_users = $pdo->query("SELECT name, created_at FROM users ORDER BY created_at DESC LIMIT 3")->fetchAll(PDO::FETCH_ASSOC);

function getStatusBadge($status) {
    if ($status === 'Shipped') return 'badge-shipped';
    if ($status === 'Delivered') return 'badge-delivered';
    return 'badge-processing';
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>

    <!-- LEFT SIDEBAR -->
    <?php include 'includes/sidebar.php'; ?>

    <!-- MAIN CONTENT -->
    <main class="main-content">
        <!-- TOP HEADER -->
        <?php include 'includes/header.php'; ?>

        <!-- WELCOME -->
        <div class="welcome-header">
            <div>
                <div class="welcome-title">Welcome back, Admin! 👋</div>
                <div class="welcome-subtitle">Here's what's happening with your store today.</div>
            </div>
            <div class="date-picker">
                📅 <?= date('M j') ?> – <?= date('M j, Y') ?>
            </div>
        </div>

        <!-- STATS GRID -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Total Revenue</div>
                    <div class="stat-icon" style="background:#E8F5E9; color:#2E7D32;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
                <div class="stat-value">Ksh <?= number_format($total_revenue) ?></div>
                <div class="stat-trend"><span class="trend-up">▲ 12.5%</span> <span class="trend-text">vs last 7 days</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Orders</div>
                    <div class="stat-icon" style="background:#FFF3E0; color:#EF6C00;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"></path></svg>
                    </div>
                </div>
                <div class="stat-value"><?= number_format($total_orders) ?></div>
                <div class="stat-trend"><span class="trend-up">▲ 8.7%</span> <span class="trend-text">vs last 7 days</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Customers</div>
                    <div class="stat-icon" style="background:#E3F2FD; color:#1565C0;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                    </div>
                </div>
                <div class="stat-value"><?= number_format($total_customers) ?></div>
                <div class="stat-trend"><span class="trend-up">▲ 11.3%</span> <span class="trend-text">vs last 7 days</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Average Order Value</div>
                    <div class="stat-icon" style="background:#FCE4EC; color:#C2185B;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                    </div>
                </div>
                <div class="stat-value">Ksh <?= number_format($avg_order) ?></div>
                <div class="stat-trend"><span class="trend-up">▲ 4.2%</span> <span class="trend-text">vs last 7 days</span></div>
            </div>
        </div>

        <!-- CHARTS ROW -->
        <div class="charts-row">
            <div class="card">
                <div class="card-header">
                    <div class="card-title">Sales Overview</div>
                    <div style="font-size:12px; display:flex; gap:16px;">
                        <span style="display:flex; align-items:center; gap:4px;"><div class="status-dot"></div> This Week</span>
                        <span style="display:flex; align-items:center; gap:4px; color:var(--text-muted);"><div class="status-dot" style="background:var(--border-color)"></div> Last Week</span>
                    </div>
                </div>
                <div style="position:relative; height: 220px; width: 100%;">
                    <canvas id="salesChart"></canvas>
                </div>
            </div>
            
            <div class="card">
                <div class="card-header">
                    <div class="card-title">Top Selling Books</div>
                    <a href="#" class="card-action">View all</a>
                </div>
                <div class="top-selling-list">
                    <?php $rank=1; foreach($top_books as $b): ?>
                    <div class="top-selling-item">
                        <div class="rank"><?= $rank++ ?></div>
                        <?php if($b['cover_url']): ?><img src="<?= $b['cover_url'] ?>" class="top-cover"><?php else: ?><div class="top-cover"></div><?php endif; ?>
                        <div class="top-info">
                            <div class="top-title"><?= htmlspecialchars($b['title']) ?></div>
                            <div class="top-author"><?= htmlspecialchars($b['author']) ?></div>
                        </div>
                        <div class="top-stats">
                            <div class="top-rev">Ksh <?= number_format($b['rev']) ?></div>
                            <div class="top-sold"><?= $b['sold'] ?> sold</div>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>

        <!-- TABLES ROW -->
        <div class="tables-row">
            <div class="card">
                <div class="card-header">
                    <div class="card-title">Recent Orders</div>
                    <a href="orders.php" class="card-action">View all orders</a>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Order ID</th>
                            <th>Customer</th>
                            <th>Date</th>
                            <th>Total</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach($recent_orders as $o): ?>
                        <tr>
                            <td style="color:var(--text-muted)">#<?= substr($o['id'], 0, 5) ?></td>
                            <td class="td-user">
                                <div class="td-user-avatar"><?= substr($o['customer'],0,1) ?></div>
                                <?= htmlspecialchars($o['customer']) ?>
                            </td>
                            <td style="color:var(--text-muted)"><?= date('M j, Y', strtotime($o['created_at'])) ?></td>
                            <td style="font-weight:600;">Ksh <?= number_format($o['total_amount']) ?></td>
                            <td><span class="badge <?= getStatusBadge($o['status']) ?>"><?= $o['status'] ?></span></td>
                        </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>

            <div class="card">
                <div class="card-header">
                    <div class="card-title">Sales by Category</div>
                    <a href="#" class="card-action">View report</a>
                </div>
                <div style="position:relative; height: 180px; width: 100%;">
                    <canvas id="categoryChart"></canvas>
                </div>
            </div>
        </div>

        <!-- QUICK ACTIONS -->
        <div style="font-weight:700; font-size:15px; margin-bottom: 16px;">Quick Actions</div>
        <div class="quick-actions-bar">
            <a href="#" class="quick-action-btn" onclick="openDrawer('add')">
                <div class="quick-action-icon" style="color:var(--accent-green);">
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>
                </div>
                <span class="quick-action-label">Add New Book</span>
            </a>
            <a href="#" class="quick-action-btn">
                <div class="quick-action-icon" style="color:#E65100;">
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path></svg>
                </div>
                <span class="quick-action-label">Create Discount</span>
            </a>
            <a href="#" class="quick-action-btn">
                <div class="quick-action-icon" style="color:#D32F2F;">
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                </div>
                <span class="quick-action-label">Add Event</span>
            </a>
            <a href="#" class="quick-action-btn">
                <div class="quick-action-icon" style="color:#1565C0;">
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
                </div>
                <span class="quick-action-label">Send Newsletter</span>
            </a>
            <a href="#" class="quick-action-btn">
                <div class="quick-action-icon" style="color:#8D6E63;">
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"></path></svg>
                </div>
                <span class="quick-action-label">Manage Inventory</span>
            </a>
        </div>
    </main>

    <!-- RIGHT PANEL -->
    <aside class="right-panel">
        
        <!-- RECENT ORDERS FEED -->
        <div class="widget">
            <div class="widget-header">
                <div class="widget-title">Recent Orders</div>
                <div class="widget-action">View all</div>
            </div>
            <div class="feed-list">
                <?php foreach(array_slice($recent_orders, 0, 4) as $o): ?>
                <div class="feed-item">
                    <div class="feed-icon">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>
                    </div>
                    <div class="feed-content">
                        <div class="feed-top">
                            <span class="feed-main-text">#<?= substr($o['id'],0,5) ?></span>
                            <span class="feed-value">Ksh <?= number_format($o['total_amount']) ?></span>
                        </div>
                        <div class="feed-sub-text"><?= htmlspecialchars($o['customer']) ?></div>
                        <div class="feed-top" style="margin-top:4px;">
                            <span class="feed-time"><?= date('g:i a', strtotime($o['created_at'])) ?></span>
                            <span style="font-size:10px; font-weight:600; color:var(--accent-green)"><?= $o['status'] ?></span>
                        </div>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
        </div>

        <!-- LOW STOCK ALERTS -->
        <div class="widget">
            <div class="widget-header">
                <div class="widget-title">Low Stock Alerts</div>
                <div class="widget-action">View all</div>
            </div>
            <div class="feed-list">
                <?php if(empty($low_stock)): ?>
                    <div style="font-size:13px; color:var(--text-muted); text-align:center;">Inventory levels are healthy.</div>
                <?php endif; ?>
                <?php foreach($low_stock as $b): ?>
                <div class="feed-item">
                    <?php if($b['cover_url']): ?><img src="<?= $b['cover_url'] ?>" class="feed-icon" style="border-radius:4px;"><?php else: ?>
                        <div class="feed-icon"><svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg></div>
                    <?php endif; ?>
                    <div class="feed-content" style="align-self: center;">
                        <div class="feed-main-text"><?= htmlspecialchars($b['title']) ?></div>
                        <div class="feed-sub-text"><?= htmlspecialchars($b['author']) ?></div>
                    </div>
                    <div class="stock-alert"><?= $b['inventory_count'] ?> left</div>
                </div>
                <?php endforeach; ?>
            </div>
        </div>

        <!-- CUSTOMER ACTIVITY -->
        <div class="widget">
            <div class="widget-header">
                <div class="widget-title">Customer Activity</div>
                <div class="widget-action">View all</div>
            </div>
            <div class="feed-list">
                <?php foreach($recent_users as $u): ?>
                <div class="feed-item">
                    <div class="activity-icon-wrap">
                        <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                    </div>
                    <div class="feed-content">
                        <div class="activity-text">New customer registered</div>
                        <div class="activity-sub"><?= htmlspecialchars($u['name']) ?></div>
                    </div>
                    <div class="feed-time"><?= date('M j', strtotime($u['created_at'])) ?></div>
                </div>
                <?php endforeach; ?>
                <div class="feed-item">
                    <div class="activity-icon-wrap" style="color:#E65100;">
                        <svg width="18" height="18" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"></path></svg>
                    </div>
                    <div class="feed-content">
                        <div class="activity-text">Review submitted</div>
                        <div class="activity-sub">"Great book and fast shipping!"</div>
                    </div>
                    <div class="feed-time">1 hr ago</div>
                </div>
            </div>
        </div>
    </aside>

    <!-- SLIDE-OVER DRAWER FOR QUICK ACTION -->
    <div id="drawer-overlay" class="drawer-overlay" onclick="closeDrawer()"></div>
    <div id="main-drawer" class="drawer">
        <div class="drawer-header">
            <div class="drawer-title">Add New Book</div>
            <button class="drawer-close" onclick="closeDrawer()">×</button>
        </div>
        <div class="drawer-body">
            <form id="book-form" method="POST">
                <input type="hidden" name="_action" value="add">
                <div class="form-group"><label>Title</label><input type="text" name="title" required></div>
                <div class="form-group"><label>Author</label><input type="text" name="author" required></div>
                <div class="form-group"><label>Price (Ksh)</label><input type="number" name="price_ksh" required></div>
                <div class="form-group"><label>Initial Inventory Count</label><input type="number" name="inventory_count" value="15" required></div>
                <div class="form-group"><label>Category</label><input type="text" name="category" placeholder="Fiction, Non-Fiction..."></div>
                <div class="form-group"><label>Cover URL</label><input type="url" name="cover_url"></div>
            </form>
        </div>
        <div class="drawer-footer">
            <button class="btn btn-outline" onclick="closeDrawer()">Cancel</button>
            <button class="btn btn-primary" onclick="document.getElementById('book-form').submit()" style="margin-left:auto;">Save Book</button>
        </div>
    </div>

    <!-- CHARTS INIT -->
    <script>
        const accentGreen = '#365134';
        const colors = ['#365134', '#8B7355', '#C4A882', '#EAEAEA', '#2D2D2D'];

        // Line Chart
        const sCtx = document.getElementById('salesChart').getContext('2d');
        new Chart(sCtx, {
            type: 'line',
            data: {
                labels: <?= json_encode($rev_labels) ?>,
                datasets: [{
                    label: 'Revenue', data: <?= json_encode($rev_values) ?>,
                    borderColor: accentGreen, borderWidth: 3, tension: 0.4, pointRadius: 0
                }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { display: false }, border: {display: false} }, y: { grid: { color: '#EAEAEA' }, border: {display: false}, ticks: { callback: function(val) { return 'Ksh ' + val/1000 + 'k'; } } } } }
        });

        // Donut Chart
        const cCtx = document.getElementById('categoryChart').getContext('2d');
        new Chart(cCtx, {
            type: 'doughnut',
            data: { labels: <?= json_encode($cat_labels) ?>, datasets: [{ data: <?= json_encode($cat_values) ?>, backgroundColor: colors, borderWidth: 0 }] },
            options: { responsive: true, maintainAspectRatio: false, cutout: '65%', plugins: { legend: { position: 'right', labels: { boxWidth: 10, font: {size: 11} } } } }
        });

        function openDrawer() { document.getElementById('drawer-overlay').classList.add('show'); document.getElementById('main-drawer').classList.add('open'); }
        function closeDrawer() { document.getElementById('drawer-overlay').classList.remove('show'); document.getElementById('main-drawer').classList.remove('open'); }
    </script>
</body>
</html>
