<?php
require 'db.php';

// 1. Overview Stats
$total_revenue = $pdo->query("SELECT SUM(total_amount) FROM orders WHERE status != 'Cancelled'")->fetchColumn() ?: 0;
$total_orders = $pdo->query("SELECT COUNT(*) FROM orders WHERE status != 'Cancelled'")->fetchColumn() ?: 0;
$avg_order_value = $total_orders > 0 ? $total_revenue / $total_orders : 0;

// 2. Revenue by Day (Last 7 Days)
$revenue_by_day = $pdo->query("
    SELECT date(created_at) as order_date, SUM(total_amount) as daily_revenue 
    FROM orders 
    WHERE status != 'Cancelled' AND created_at >= date('now', '-7 days')
    GROUP BY date(created_at)
    ORDER BY date(created_at) ASC
")->fetchAll(PDO::FETCH_ASSOC);

$dates = []; $daily_revenues = [];
foreach ($revenue_by_day as $row) {
    $dates[] = date('M j', strtotime($row['order_date']));
    $daily_revenues[] = (int)$row['daily_revenue'];
}

// 3. Top Categories
$top_categories = $pdo->query("
    SELECT b.category, SUM(oi.quantity) as total_sold
    FROM order_items oi
    JOIN books b ON oi.book_id = b.id
    JOIN orders o ON oi.order_id = o.id
    WHERE o.status != 'Cancelled'
    GROUP BY b.category
    ORDER BY total_sold DESC
    LIMIT 5
")->fetchAll(PDO::FETCH_ASSOC);

$cat_labels = []; $cat_data = [];
foreach ($top_categories as $row) {
    $cat_labels[] = $row['category'] ?: 'Uncategorised';
    $cat_data[] = (int)$row['total_sold'];
}

// 4. Order Status Distribution
$statuses = $pdo->query("SELECT status, COUNT(*) as count FROM orders GROUP BY status")->fetchAll(PDO::FETCH_ASSOC);
$status_labels = []; $status_data = [];
foreach ($statuses as $row) {
    $status_labels[] = $row['status'];
    $status_data[] = (int)$row['count'];
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Insights</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 24px; }
        @media (max-width: 900px) { .charts-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <?php include 'includes/sidebar.php'; ?>

    <!-- MAIN CONTENT -->
    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>
        
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
            <h1 style="font-size: 24px; font-weight: 700;">Insights</h1>
            <button class="btn btn-outline" style="display:flex; align-items:center; gap:8px;" onclick="window.print()">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"></path></svg>
                Export
            </button>
        </div>

        <!-- STATS OVERVIEW -->
        <div class="book-grid" style="grid-template-columns: repeat(3, 1fr); margin-bottom: 32px;">
            <div class="book-card" style="padding: 24px; text-align: center;">
                <div style="font-size: 13px; font-weight: 700; color: var(--warm-brown); text-transform: uppercase; margin-bottom: 8px;">Total Revenue</div>
                <div style="font-size: 24px; font-weight: 800; color: var(--dark-brown);">Ksh <?= number_format($total_revenue) ?></div>
            </div>
            <div class="book-card" style="padding: 24px; text-align: center;">
                <div style="font-size: 13px; font-weight: 700; color: var(--warm-brown); text-transform: uppercase; margin-bottom: 8px;">Completed Orders</div>
                <div style="font-size: 24px; font-weight: 800; color: var(--dark-brown);"><?= number_format($total_orders) ?></div>
            </div>
            <div class="book-card" style="padding: 24px; text-align: center;">
                <div style="font-size: 13px; font-weight: 700; color: var(--warm-brown); text-transform: uppercase; margin-bottom: 8px;">Avg Order Value</div>
                <div style="font-size: 24px; font-weight: 800; color: var(--dark-brown);">Ksh <?= number_format($avg_order_value) ?></div>
            </div>
        </div>

        <div class="charts-grid">
            <!-- REVENUE CHART -->
            <div class="analytics-card">
                <h3 style="font-size: 16px; font-weight: 800; margin-bottom: 24px;">Revenue (Last 7 Days)</h3>
                <div class="chart-wrapper">
                    <canvas id="revenueChart"></canvas>
                </div>
            </div>

            <!-- STATUS CHART -->
            <div class="analytics-card">
                <h3 style="font-size: 16px; font-weight: 800; margin-bottom: 24px;">Orders by Status</h3>
                <div class="chart-wrapper">
                    <canvas id="statusChart"></canvas>
                </div>
            </div>
        </div>

        <!-- TOP CATEGORIES -->
        <div class="analytics-card">
            <h3 style="font-size: 16px; font-weight: 800; margin-bottom: 24px;">Top Selling Categories</h3>
            <?php if (empty($top_categories)): ?>
                <div style="color: var(--muted-brown); text-align: center; padding: 20px;">No sales data available yet.</div>
            <?php else: ?>
                <div style="display: flex; flex-direction: column; gap: 16px;">
                    <?php 
                    $max_sold = max($cat_data ?: [1]);
                    foreach ($top_categories as $cat): 
                        $pct = round(($cat['total_sold'] / $max_sold) * 100);
                    ?>
                        <div>
                            <div style="display:flex; justify-content:space-between; font-size:14px; font-weight:600; margin-bottom: 8px;">
                                <span><?= htmlspecialchars($cat['category'] ?: 'Uncategorised') ?></span>
                                <span style="color: var(--warm-brown);"><?= $cat['total_sold'] ?> books sold</span>
                            </div>
                            <div style="height: 12px; background: var(--border); border-radius: 6px; overflow: hidden;">
                                <div style="height: 100%; width: <?= $pct ?>%; background: var(--dark-brown); border-radius: 6px;"></div>
                            </div>
                        </div>
                    <?php endforeach; ?>
                </div>
            <?php endif; ?>
        </div>

    </main>

    <script>
        const brandDark = '#1A1512';
        const brandWarm = '#8B7355';
        const bgColors = ['#1A1512', '#8B7355', '#C4A882', '#D6C8B0', '#F0E8D8'];

        const revCtx = document.getElementById('revenueChart').getContext('2d');
        new Chart(revCtx, {
            type: 'line',
            data: {
                labels: <?= json_encode($dates) ?>,
                datasets: [{
                    label: 'Revenue (Ksh)', data: <?= json_encode($daily_revenues) ?>,
                    borderColor: brandDark, backgroundColor: 'rgba(26, 21, 18, 0.1)',
                    borderWidth: 3, fill: true, tension: 0.4, pointBackgroundColor: brandDark, pointRadius: 4
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, border: {display: false}, grid: { color: 'rgba(0,0,0,0.05)' } },
                    x: { border: {display: false}, grid: { display: false } }
                }
            }
        });

        const statCtx = document.getElementById('statusChart').getContext('2d');
        new Chart(statCtx, {
            type: 'doughnut',
            data: {
                labels: <?= json_encode($status_labels) ?>,
                datasets: [{ data: <?= json_encode($status_data) ?>, backgroundColor: bgColors, borderWidth: 0 }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16 } } },
                cutout: '75%'
            }
        });
    </script>
</body>
</html>
