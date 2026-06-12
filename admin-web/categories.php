<?php
require 'db.php';
require 'includes/auth_gate.php';

// Full category analytics
$categories = $pdo->query("
    SELECT 
        b.category,
        COUNT(DISTINCT b.id) as book_count,
        COALESCE(SUM(oi.quantity), 0) as total_sold,
        COALESCE(SUM(oi.quantity * oi.price_ksh), 0) as total_revenue,
        AVG(b.price_ksh) as avg_price,
        SUM(b.inventory_count) as total_stock
    FROM books b
    LEFT JOIN order_items oi ON b.id = oi.book_id
    LEFT JOIN orders o ON oi.order_id = o.id AND o.status != 'Cancelled'
    GROUP BY b.category
    ORDER BY total_revenue DESC
")->fetchAll(PDO::FETCH_ASSOC);

// Get 6-month monthly revenue per category
$cat_monthly = [];
foreach ($categories as $cat) {
    $name = $cat['category'] ?: 'Uncategorised';
    if ($isPostgres) {
        $mo_expr = "TO_CHAR(o.created_at, 'YYYY-MM')";
        $date_limit = "CURRENT_DATE - INTERVAL '6 months'";
    } else {
        $mo_expr = "strftime('%Y-%m', o.created_at)";
        $date_limit = "date('now', '-6 months')";
    }
    $stmt = $pdo->prepare("
        SELECT $mo_expr as mo, SUM(oi.quantity * oi.price_ksh) as rev
        FROM order_items oi
        JOIN books b ON oi.book_id = b.id
        JOIN orders o ON oi.order_id = o.id
        WHERE o.status != 'Cancelled'
          AND (b.category = ? OR (b.category = '' AND ? = 'Uncategorised'))
          AND o.created_at >= $date_limit
        GROUP BY mo ORDER BY mo ASC
    ");
    $stmt->execute([$cat['category'], $name]);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $cat_monthly[$name] = array_column($rows, 'rev');
}

// Grand totals for share calculation
$grand_rev = array_sum(array_column($categories, 'total_revenue')) ?: 1;

// Trend: compare last 30 vs previous 30 days per category
$trends = [];
foreach ($categories as $cat) {
    if ($isPostgres) {
        $trend_sql = "
            SELECT
                SUM(CASE WHEN o.created_at >= CURRENT_DATE - INTERVAL '30 days' THEN oi.quantity * oi.price_ksh ELSE 0 END) as recent,
                SUM(CASE WHEN o.created_at >= CURRENT_DATE - INTERVAL '60 days' AND o.created_at < CURRENT_DATE - INTERVAL '30 days' THEN oi.quantity * oi.price_ksh ELSE 0 END) as prior
            FROM order_items oi
            JOIN books b ON oi.book_id = b.id
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status != 'Cancelled' AND b.category = ?
        ";
    } else {
        $trend_sql = "
            SELECT
                SUM(CASE WHEN o.created_at >= date('now', '-30 days') THEN oi.quantity * oi.price_ksh ELSE 0 END) as recent,
                SUM(CASE WHEN o.created_at >= date('now', '-60 days') AND o.created_at < date('now', '-30 days') THEN oi.quantity * oi.price_ksh ELSE 0 END) as prior
            FROM order_items oi
            JOIN books b ON oi.book_id = b.id
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status != 'Cancelled' AND b.category = ?
        ";
    }
    $stmt = $pdo->prepare($trend_sql);
    $stmt->execute([$cat['category']]);
    $r = $stmt->fetch(PDO::FETCH_ASSOC);
    $recent = (float)($r['recent'] ?? 0);
    $prior = (float)($r['prior'] ?? 0);
    if ($prior > 0) $pctChange = round((($recent - $prior) / $prior) * 100, 1);
    elseif ($recent > 0) $pctChange = 100;
    else $pctChange = 0;
    $trends[$cat['category']] = $pctChange;
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Categories | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; }
        .cat-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); overflow: hidden; transition: transform 0.2s, box-shadow 0.2s; }
        .cat-card:hover { transform: translateY(-4px); box-shadow: 0 16px 40px rgba(0,0,0,0.08); }
        .cat-card-header { padding: 20px 20px 0; }
        .cat-title-row { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
        .cat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
        .cat-name { font-size: 18px; font-weight: 800; margin-bottom: 2px; }
        .cat-sub { font-size: 13px; color: var(--text-muted); }
        .trend-pill { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 700; }
        .trend-up { background: #E8F5E9; color: #2E7D32; }
        .trend-down { background: #FFEBEE; color: #C62828; }
        .trend-flat { background: #F5F5F5; color: #757575; }

        /* Mini chart area */
        .cat-chart-area { height: 80px; margin: 4px 0 0; }

        /* Stats row */
        .cat-stats { display: grid; grid-template-columns: 1fr 1fr 1fr; border-top: 1px solid var(--border-color); }
        .cat-stat { padding: 14px 16px; border-right: 1px solid var(--border-color); }
        .cat-stat:last-child { border-right: none; }
        .cat-stat-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.6px; font-weight: 700; color: var(--text-muted); margin-bottom: 4px; }
        .cat-stat-val { font-size: 15px; font-weight: 800; color: var(--text-main); }

        /* Revenue share bar */
        .rev-share { height: 4px; background: var(--border-color); margin: 0; }
        .rev-share-fill { height: 100%; transition: width 0.6s cubic-bezier(0.4,0,0.2,1); }

        /* Summary top bar */
        .top-summary { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; margin-bottom: 28px; }
        .sum-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 20px; }
        .sum-label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.6px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px; }
        .sum-val { font-size: 26px; font-weight: 800; }
    </style>
    <!-- PWA Tags -->
    <link rel="manifest" href="manifest.json">
    <meta name="theme-color" content="#365134">
    <link rel="apple-touch-icon" href="icon.svg">
    <script>
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('sw.js').then(reg => {
                    console.log('SW registered!', reg);
                }).catch(err => console.log('SW registration failed', err));
            });
        }
    </script>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:20px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Categories</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= count($categories) ?> categories · Ksh <?= number_format($grand_rev) ?> total revenue</p>
            </div>
            <button class="btn btn-primary" onclick="alert('Add Category coming soon!')">+ New Category</button>
        </div>

        <!-- Summary -->
        <div class="top-summary">
            <div class="sum-card">
                <div class="sum-label">Total Categories</div>
                <div class="sum-val"><?= count($categories) ?></div>
            </div>
            <div class="sum-card">
                <div class="sum-label">Top Earning Category</div>
                <?php if (!empty($categories)): ?>
                <div class="sum-val" style="font-size:18px;"><?= htmlspecialchars($categories[0]['category'] ?: 'Uncategorised') ?></div>
                <div style="color:var(--text-muted); font-size:13px; margin-top:4px;">Ksh <?= number_format($categories[0]['total_revenue'] ?? 0) ?></div>
                <?php else: ?>
                <div class="sum-val" style="font-size:18px; color:var(--text-muted);">None yet</div>
                <?php endif; ?>
            </div>
            <div class="sum-card">
                <div class="sum-label">Total Books Sold</div>
                <div class="sum-val"><?= number_format(array_sum(array_column($categories, 'total_sold'))) ?></div>
            </div>
        </div>

        <!-- Category Cards Grid -->
        <div class="cat-grid">
            <?php 
            $palette = [
                ['bg'=>'#E8F5E9','icon'=>'#2E7D32'],
                ['bg'=>'#E3F2FD','icon'=>'#1565C0'],
                ['bg'=>'#FFF3E0','icon'=>'#E65100'],
                ['bg'=>'#FCE4EC','icon'=>'#C2185B'],
                ['bg'=>'#EDE7F6','icon'=>'#4527A0'],
                ['bg'=>'#E0F7FA','icon'=>'#00695C'],
            ];
            foreach ($categories as $i => $cat):
                $name = $cat['category'] ?: 'Uncategorised';
                $color = $palette[$i % count($palette)];
                $revShare = round(($cat['total_revenue'] / $grand_rev) * 100, 1);
                $trend = $trends[$cat['category']];
                if ($trend > 5) { $tClass = 'trend-up'; $tIcon = '↑'; $tLabel = '+' . $trend . '%'; }
                elseif ($trend < -5) { $tClass = 'trend-down'; $tIcon = '↓'; $tLabel = $trend . '%'; }
                else { $tClass = 'trend-flat'; $tIcon = '→'; $tLabel = 'Stable'; }
                $sparkData = json_encode($cat_monthly[$name] ?? []);
            ?>
            <div class="cat-card">
                <!-- Revenue share indicator bar at top -->
                <div class="rev-share">
                    <div class="rev-share-fill" style="width:<?= $revShare ?>%; background:<?= $color['icon'] ?>;"></div>
                </div>
                <div class="cat-card-header">
                    <div class="cat-title-row">
                        <div style="display:flex; align-items:center; gap:12px;">
                            <div class="cat-icon" style="background:<?= $color['bg'] ?>; color:<?= $color['icon'] ?>;">
                                <svg width="22" height="22" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/></svg>
                            </div>
                            <div>
                                <div class="cat-name"><?= htmlspecialchars($name) ?></div>
                                <div class="cat-sub"><?= $cat['book_count'] ?> books · <?= $revShare ?>% of revenue</div>
                            </div>
                        </div>
                        <span class="trend-pill <?= $tClass ?>">
                            <?= $tIcon ?> <?= $tLabel ?>
                        </span>
                    </div>

                    <!-- Mini chart -->
                    <div class="cat-chart-area">
                        <canvas class="mini-cat-chart" data-values="<?= htmlspecialchars($sparkData) ?>" data-color="<?= $color['icon'] ?>"></canvas>
                    </div>
                </div>

                <div class="cat-stats">
                    <div class="cat-stat">
                        <div class="cat-stat-label">Revenue</div>
                        <div class="cat-stat-val">Ksh <?= number_format($cat['total_revenue']) ?></div>
                    </div>
                    <div class="cat-stat">
                        <div class="cat-stat-label">Units Sold</div>
                        <div class="cat-stat-val"><?= number_format($cat['total_sold']) ?></div>
                    </div>
                    <div class="cat-stat">
                        <div class="cat-stat-label">Avg. Price</div>
                        <div class="cat-stat-val">Ksh <?= number_format($cat['avg_price']) ?></div>
                    </div>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
    </main>

    <script>
        document.querySelectorAll('.mini-cat-chart').forEach(canvas => {
            const vals = JSON.parse(canvas.dataset.values || '[]');
            const color = canvas.dataset.color;
            if (!vals.length) {
                canvas.parentElement.innerHTML = '<div style="height:80px; display:flex; align-items:center; justify-content:center; color:var(--text-muted); font-size:12px;">No sales data yet</div>';
                return;
            }
            // Parse color to rgba
            const hex = color.replace('#','');
            const r = parseInt(hex.substring(0,2),16);
            const g = parseInt(hex.substring(2,4),16);
            const b = parseInt(hex.substring(4,6),16);

            new Chart(canvas.getContext('2d'), {
                type: 'line',
                data: {
                    labels: vals.map((_,i) => ''),
                    datasets: [{
                        data: vals,
                        borderColor: color,
                        borderWidth: 2,
                        pointRadius: 0,
                        fill: true,
                        backgroundColor: `rgba(${r},${g},${b},0.08)`,
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false,
                    plugins: { legend: { display: false }, tooltip: {
                        callbacks: { label: ctx => 'Ksh ' + Number(ctx.raw).toLocaleString() }
                    }},
                    scales: { x: { display: false }, y: { display: false } },
                    animation: { duration: 800 }
                }
            });
        });
    </script>
</body>
</html>

