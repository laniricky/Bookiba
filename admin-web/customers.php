<?php
require 'db.php';

// Stats for overview cards
$total_customers = $pdo->query("SELECT COUNT(*) FROM users")->fetchColumn();
$active_customers = $pdo->query("SELECT COUNT(DISTINCT user_id) FROM orders WHERE created_at >= date('now', '-30 days') AND status != 'Cancelled'")->fetchColumn();
$total_revenue = $pdo->query("SELECT SUM(total_amount) FROM orders WHERE status != 'Cancelled'")->fetchColumn() ?: 0;
$avg_ltv = $total_customers > 0 ? $total_revenue / $total_customers : 0;

// Fetch Customers List
$stmt = $pdo->query("
    SELECT u.id, u.name, u.email, u.created_at,
           COUNT(o.id) as total_orders,
           SUM(o.total_amount) as lifetime_spent,
           MAX(o.created_at) as last_active
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id AND o.status != 'Cancelled'
    GROUP BY u.id
    ORDER BY u.created_at DESC
");
$customers = $stmt->fetchAll(PDO::FETCH_ASSOC);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Customers | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .cust-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; background: var(--bg-cream); display: flex; align-items: center; justify-content: center; font-weight: 700; color: var(--accent-green); }
        .stats-grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
            <h1 style="font-size: 24px; font-weight: 700;">Customer Directory</h1>
            <button class="btn btn-outline" style="display:flex; align-items:center; gap:8px;">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                Export CSV
            </button>
        </div>

        <div class="stats-grid-3">
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Total Customers</div>
                    <div class="stat-icon" style="background:#E3F2FD; color:#1565C0;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                    </div>
                </div>
                <div class="stat-value"><?= number_format($total_customers) ?></div>
            </div>
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Active (Last 30 Days)</div>
                    <div class="stat-icon" style="background:#E8F5E9; color:#2E7D32;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
                <div class="stat-value"><?= number_format($active_customers) ?></div>
            </div>
            <div class="stat-card">
                <div class="stat-card-top">
                    <div class="stat-label">Average Lifetime Value</div>
                    <div class="stat-icon" style="background:#FFF3E0; color:#EF6C00;">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </div>
                </div>
                <div class="stat-value">Ksh <?= number_format($avg_ltv) ?></div>
            </div>
        </div>

        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th>Customer</th>
                        <th>Join Date</th>
                        <th>Total Orders</th>
                        <th>Lifetime Spent</th>
                        <th>Last Active</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($customers)): ?>
                        <tr><td colspan="5" style="text-align:center; padding: 32px; color:var(--text-muted);">No customers found.</td></tr>
                    <?php endif; ?>
                    <?php foreach($customers as $c): ?>
                    <tr style="cursor: pointer;" onclick="alert('View customer details coming soon')">
                        <td>
                            <div style="display:flex; align-items:center; gap: 12px;">
                                <div class="cust-avatar"><?= strtoupper(substr($c['name'], 0, 1)) ?></div>
                                <div>
                                    <div style="font-weight: 600; color: var(--text-main);"><?= htmlspecialchars($c['name']) ?></div>
                                    <div style="font-size: 12px; color: var(--text-muted);"><?= htmlspecialchars($c['email']) ?></div>
                                </div>
                            </div>
                        </td>
                        <td style="color:var(--text-muted);"><?= date('M j, Y', strtotime($c['created_at'])) ?></td>
                        <td><span style="background:var(--bg-cream); padding:2px 8px; border-radius:12px; font-weight:600;"><?= $c['total_orders'] ?></span></td>
                        <td style="font-weight: 600;">Ksh <?= number_format($c['lifetime_spent'] ?: 0) ?></td>
                        <td style="color:var(--text-muted);"><?= $c['last_active'] ? date('M j, Y', strtotime($c['last_active'])) : 'Never' ?></td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </main>
</body>
</html>
