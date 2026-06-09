<?php
require 'db.php';

// Date range filter
$range = $_GET['range'] ?? '30';
$customFrom = $_GET['from'] ?? '';
$customTo = $_GET['to'] ?? '';

if ($customFrom && $customTo) {
    $dateFrom = $customFrom;
    $dateTo = $customTo;
} else {
    $dateTo = date('Y-m-d');
    $dateFrom = date('Y-m-d', strtotime("-{$range} days"));
}

// ── 1. KPI Overview ──────────────────────────────────────────
$kpis = $pdo->prepare("
    SELECT
        SUM(total_amount) as revenue,
        COUNT(*) as orders,
        COUNT(DISTINCT user_id) as customers,
        AVG(total_amount) as aov
    FROM orders
    WHERE status != 'Cancelled'
      AND DATE(created_at) BETWEEN ? AND ?
");
$kpis->execute([$dateFrom, $dateTo]);
$kpi = $kpis->fetch(PDO::FETCH_ASSOC);

// Previous period for delta
$prevTo = date('Y-m-d', strtotime($dateFrom . ' -1 day'));
$prevFrom = date('Y-m-d', strtotime($prevTo . ' -' . (strtotime($dateTo) - strtotime($dateFrom)) / 86400 . ' days'));
$prevKpis = $pdo->prepare("SELECT SUM(total_amount) as revenue, COUNT(*) as orders FROM orders WHERE status != 'Cancelled' AND DATE(created_at) BETWEEN ? AND ?");
$prevKpis->execute([$prevFrom, $prevTo]);
$prev = $prevKpis->fetch(PDO::FETCH_ASSOC);

function delta($current, $previous) {
    if (!$previous) return null;
    return round((($current - $previous) / $previous) * 100, 1);
}
$revDelta = delta($kpi['revenue'], $prev['revenue']);
$ordDelta = delta($kpi['orders'], $prev['orders']);

// ── 2. Daily Revenue + Orders (dual axis) ────────────────────
$daily = $pdo->prepare("
    SELECT DATE(created_at) as d,
           SUM(total_amount) as rev,
           COUNT(*) as cnt
    FROM orders
    WHERE status != 'Cancelled' AND DATE(created_at) BETWEEN ? AND ?
    GROUP BY d ORDER BY d ASC
");
$daily->execute([$dateFrom, $dateTo]);
$dailyRows = $daily->fetchAll(PDO::FETCH_ASSOC);
$dailyLabels = array_map(fn($r) => date('M j', strtotime($r['d'])), $dailyRows);
$dailyRev    = array_column($dailyRows, 'rev');
$dailyOrders = array_column($dailyRows, 'cnt');

// ── 3. Revenue by Category (horizontal bar) ──────────────────
$catRev = $pdo->prepare("
    SELECT b.category, SUM(oi.quantity * oi.price_ksh) as rev
    FROM order_items oi
    JOIN books b ON oi.book_id = b.id
    JOIN orders o ON oi.order_id = o.id
    WHERE o.status != 'Cancelled' AND DATE(o.created_at) BETWEEN ? AND ?
    GROUP BY b.category ORDER BY rev DESC LIMIT 8
");
$catRev->execute([$dateFrom, $dateTo]);
$catRows = $catRev->fetchAll(PDO::FETCH_ASSOC);
$catLabels = array_map(fn($r) => $r['category'] ?: 'Uncategorised', $catRows);
$catVals   = array_column($catRows, 'rev');

// ── 4. Order Status Donut ─────────────────────────────────────
$statusRows = $pdo->prepare("
    SELECT status, COUNT(*) as cnt FROM orders
    WHERE DATE(created_at) BETWEEN ? AND ?
    GROUP BY status
");
$statusRows->execute([$dateFrom, $dateTo]);
$statusData = $statusRows->fetchAll(PDO::FETCH_ASSOC);
$statusLabels = array_column($statusData, 'status');
$statusCounts = array_column($statusData, 'cnt');

// ── 5. Hourly Heatmap (day-of-week × hour) ───────────────────
$heatmap = array_fill(0, 7, array_fill(0, 24, 0));
if ($isPostgres) {
    $heatmapSql = "
        SELECT EXTRACT(DOW FROM created_at)::int as dow,
               EXTRACT(HOUR FROM created_at)::int as hr,
               COUNT(*) as cnt
        FROM orders
        WHERE status != 'Cancelled' AND DATE(created_at) BETWEEN ? AND ?
        GROUP BY dow, hr
    ";
} else {
    $heatmapSql = "
        SELECT strftime('%w', created_at) as dow,
               strftime('%H', created_at) as hr,
               COUNT(*) as cnt
        FROM orders
        WHERE status != 'Cancelled' AND DATE(created_at) BETWEEN ? AND ?
        GROUP BY dow, hr
    ";
}
$heatRows = $pdo->prepare($heatmapSql);
$heatRows->execute([$dateFrom, $dateTo]);
foreach ($heatRows->fetchAll(PDO::FETCH_ASSOC) as $row) {
    $heatmap[(int)$row['dow']][(int)$row['hr']] = (int)$row['cnt'];
}
$maxHeat = max(array_map('max', $heatmap)) ?: 1;

// ── 6. Top Books ──────────────────────────────────────────────
$topBooks = $pdo->prepare("
    SELECT b.title, b.author, b.cover_url,
           SUM(oi.quantity) as units,
           SUM(oi.quantity * oi.price_ksh) as rev
    FROM order_items oi
    JOIN books b ON oi.book_id = b.id
    JOIN orders o ON oi.order_id = o.id
    WHERE o.status != 'Cancelled' AND DATE(o.created_at) BETWEEN ? AND ?
    GROUP BY b.id, b.title, b.author, b.cover_url ORDER BY rev DESC LIMIT 7
");
$topBooks->execute([$dateFrom, $dateTo]);
$topBooksRows = $topBooks->fetchAll(PDO::FETCH_ASSOC);

$days = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
$hours = range(0, 23);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Analytics | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        /* KPI Cards */
        .kpi-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 16px; margin-bottom: 28px; }
        .kpi-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 22px; }
        .kpi-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.8px; font-weight: 700; color: var(--text-muted); margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
        .kpi-value { font-size: 28px; font-weight: 900; color: var(--text-main); letter-spacing: -0.5px; line-height: 1; }
        .kpi-delta { margin-top: 10px; font-size: 12px; font-weight: 700; display: flex; align-items: center; gap: 4px; }
        .delta-pos { color: #2E7D32; } .delta-neg { color: #C62828; } .delta-nil { color: var(--text-muted); }

        /* Date range bar */
        .date-bar { display: flex; gap: 8px; align-items: center; margin-bottom: 24px; flex-wrap: wrap; }
        .range-btn { padding: 7px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; border: 1px solid var(--border-color); cursor: pointer; background: white; color: var(--text-muted); transition: all 0.15s; }
        .range-btn.active { background: var(--text-main); color: white; border-color: var(--text-main); }
        .date-input { padding: 7px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 13px; font-family: inherit; outline: none; }
        .date-input:focus { border-color: var(--accent-green); }

        /* Chart grid */
        .charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 20px; }
        .charts-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
        .chart-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 22px; }
        .chart-card-title { font-size: 15px; font-weight: 800; margin-bottom: 18px; display: flex; align-items: center; gap: 8px; justify-content: space-between; }
        .chart-sub { font-size: 12px; color: var(--text-muted); font-weight: 400; }

        /* Heatmap */
        .heatmap-wrap { overflow-x: auto; }
        .heatmap-table { border-collapse: collapse; width: 100%; font-size: 10px; }
        .heatmap-table th { padding: 4px 6px; color: var(--text-muted); font-weight: 600; text-align: center; }
        .heatmap-table td { width: 32px; height: 28px; border-radius: 3px; text-align: center; vertical-align: middle; font-size: 9px; font-weight: 700; cursor: default; transition: transform 0.1s; }
        .heatmap-table td:hover { transform: scale(1.2); }
        .hm-label { font-weight: 700; color: var(--text-muted); font-size: 11px; padding-right: 10px; white-space: nowrap; }

        /* Top books table */
        .top-table { width: 100%; border-collapse: collapse; }
        .top-table td { padding: 10px 0; border-bottom: 1px solid var(--border-color); font-size: 13px; vertical-align: middle; }
        .top-table tr:last-child td { border-bottom: none; }
        .rank-num { width: 24px; height: 24px; border-radius: 50%; background: var(--bg-cream); display: inline-flex; align-items: center; justify-content: center; font-weight: 800; font-size: 11px; color: var(--text-muted); flex-shrink: 0; }
        .rank-num.gold { background: #FFF8E1; color: #F57F17; }
        .rank-num.silver { background: #FAFAFA; color: #757575; }
        .rank-num.bronze { background: #FBE9E7; color: #BF360C; }

        /* Full-width heatmap section */
        .full-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 22px; margin-bottom: 20px; }

        .legend-dot { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:20px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Analytics</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= date('M j, Y', strtotime($dateFrom)) ?> – <?= date('M j, Y', strtotime($dateTo)) ?></p>
            </div>
            <button class="btn btn-outline" onclick="window.print()" style="display:flex; align-items:center; gap:8px;">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"/></svg>
                Export
            </button>
        </div>

        <!-- Date Range Selector -->
        <form method="GET" id="rangeForm">
            <div class="date-bar">
                <?php foreach (['7'=>'7D','30'=>'30D','90'=>'90D','365'=>'1Y'] as $v=>$l): ?>
                <button type="submit" name="range" value="<?= $v ?>" class="range-btn <?= $range === $v && !$customFrom ? 'active' : '' ?>"><?= $l ?></button>
                <?php endforeach; ?>
                <div style="display:flex; align-items:center; gap:8px; margin-left:8px; border-left:1px solid var(--border-color); padding-left:16px;">
                    <input type="date" name="from" value="<?= $customFrom ?: $dateFrom ?>" class="date-input">
                    <span style="color:var(--text-muted);">to</span>
                    <input type="date" name="to" value="<?= $customTo ?: $dateTo ?>" class="date-input">
                    <button type="submit" class="btn btn-primary" style="padding:7px 16px; font-size:13px;">Apply</button>
                </div>
            </div>
        </form>

        <!-- KPI Cards -->
        <div class="kpi-grid">
            <?php
            $kpiDefs = [
                ['label'=>'Total Revenue', 'value'=>'Ksh '.number_format($kpi['revenue']??0), 'delta'=>$revDelta, 'icon'=>'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z'],
                ['label'=>'Orders', 'value'=>number_format($kpi['orders']??0), 'delta'=>$ordDelta, 'icon'=>'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4'],
                ['label'=>'Active Customers', 'value'=>number_format($kpi['customers']??0), 'delta'=>null, 'icon'=>'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z'],
                ['label'=>'Avg. Order Value', 'value'=>'Ksh '.number_format($kpi['aov']??0), 'delta'=>null, 'icon'=>'M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z'],
            ];
            foreach ($kpiDefs as $k): 
                $d = $k['delta'];
            ?>
            <div class="kpi-card">
                <div class="kpi-label">
                    <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="<?= $k['icon'] ?>"/></svg>
                    <?= $k['label'] ?>
                </div>
                <div class="kpi-value"><?= $k['value'] ?></div>
                <div class="kpi-delta">
                    <?php if($d !== null): ?>
                        <?php if($d > 0): ?><span class="delta-pos">
                            <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 10l7-7m0 0l7 7m-7-7v18"/></svg>
                            +<?= $d ?>% vs prior period
                        </span>
                        <?php elseif($d < 0): ?><span class="delta-neg">
                            <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M19 14l-7 7m0 0l-7-7m7 7V3"/></svg>
                            <?= $d ?>% vs prior period
                        </span>
                        <?php else: ?><span class="delta-nil">No change vs prior period</span><?php endif; ?>
                    <?php else: ?><span class="delta-nil">vs prior period</span><?php endif; ?>
                </div>
            </div>
            <?php endforeach; ?>
        </div>

        <!-- Revenue + Orders dual axis chart + Status donut -->
        <div class="charts-grid">
            <div class="chart-card">
                <div class="chart-card-title">
                    Revenue & Orders
                    <span class="chart-sub">Daily breakdown</span>
                </div>
                <div style="position:relative; height:240px;">
                    <canvas id="dualChart"></canvas>
                </div>
            </div>
            <div class="chart-card">
                <div class="chart-card-title">Order Status</div>
                <div style="position:relative; height:180px;">
                    <canvas id="statusChart"></canvas>
                </div>
                <div style="display:flex; flex-wrap:wrap; gap:10px; margin-top:14px;">
                    <?php 
                    $statusColors = ['Pending'=>'#F57F17','Processing'=>'#1565C0','Shipped'=>'#2E7D32','Delivered'=>'#9E9E9E','Cancelled'=>'#C62828'];
                    foreach($statusLabels as $j => $sl): ?>
                    <div style="display:flex; align-items:center; gap:6px; font-size:12px;">
                        <span class="legend-dot" style="background:<?= $statusColors[$sl] ?? '#ccc' ?>;"></span>
                        <?= $sl ?> (<?= $statusCounts[$j] ?>)
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>

        <!-- Category Revenue (horizontal) + Top Books -->
        <div class="charts-row-2">
            <div class="chart-card">
                <div class="chart-card-title">Revenue by Category</div>
                <div style="position:relative; height:260px;">
                    <canvas id="catChart"></canvas>
                </div>
            </div>
            <div class="chart-card">
                <div class="chart-card-title">Top Performing Books</div>
                <table class="top-table">
                    <?php foreach($topBooksRows as $ri => $book): 
                        $rankClass = $ri === 0 ? 'gold' : ($ri === 1 ? 'silver' : ($ri === 2 ? 'bronze' : ''));
                    ?>
                    <tr>
                        <td width="28"><span class="rank-num <?= $rankClass ?>"><?= $ri+1 ?></span></td>
                        <td>
                            <div style="font-weight:700; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:160px;"><?= htmlspecialchars($book['title']) ?></div>
                            <div style="font-size:11px; color:var(--text-muted);"><?= htmlspecialchars($book['author']) ?> · <?= $book['units'] ?> units</div>
                        </td>
                        <td style="text-align:right; font-weight:800; color:var(--accent-green); font-size:13px; white-space:nowrap;">Ksh <?= number_format($book['rev']) ?></td>
                    </tr>
                    <?php endforeach; ?>
                    <?php if(empty($topBooksRows)): ?>
                    <tr><td colspan="3" style="text-align:center; color:var(--text-muted); padding:20px;">No sales in this period.</td></tr>
                    <?php endif; ?>
                </table>
            </div>
        </div>

        <!-- Sales Heatmap -->
        <div class="full-card">
            <div class="chart-card-title" style="margin-bottom:20px;">
                Sales Activity Heatmap
                <span class="chart-sub">Orders by day-of-week and hour — darker = more orders</span>
            </div>
            <div class="heatmap-wrap">
                <table class="heatmap-table">
                    <thead>
                        <tr>
                            <th></th>
                            <?php for($h=0; $h<24; $h++): ?>
                            <th><?= $h === 0 ? '12a' : ($h < 12 ? $h.'a' : ($h === 12 ? '12p' : ($h-12).'p')) ?></th>
                            <?php endfor; ?>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach($days as $d => $dayName): ?>
                        <tr>
                            <td class="hm-label"><?= $dayName ?></td>
                            <?php for($h=0; $h<24; $h++):
                                $val = $heatmap[$d][$h];
                                $intensity = $val / $maxHeat;
                                // Green color scale
                                $r = (int)(232 - ($intensity * (232 - 27)));
                                $g = (int)(245 - ($intensity * (245 - 94)));
                                $b2 = (int)(233 - ($intensity * (233 - 32)));
                                $bg = $val > 0 ? "rgb($r,$g,$b2)" : '#F8F5F0';
                                $textColor = $intensity > 0.5 ? 'white' : 'rgba(0,0,0,0.4)';
                            ?>
                            <td style="background:<?= $bg ?>; color:<?= $textColor ?>;" title="<?= $dayName ?> <?= $h ?>:00 — <?= $val ?> order<?= $val != 1 ? 's' : '' ?>">
                                <?= $val > 0 ? $val : '' ?>
                            </td>
                            <?php endfor; ?>
                        </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
            <div style="display:flex; align-items:center; gap:8px; margin-top:14px; font-size:11px; color:var(--text-muted);">
                Less
                <?php for($i=0; $i<=4; $i++): 
                    $pct = $i / 4;
                    $r = (int)(232 - ($pct*(232-27)));
                    $g = (int)(245 - ($pct*(245-94)));
                    $b2 = (int)(233 - ($pct*(233-32)));
                ?>
                <span class="legend-dot" style="background:rgb(<?=$r?>,<?=$g?>,<?=$b2?>); width:16px; height:12px; border-radius:2px;"></span>
                <?php endfor; ?>
                More
            </div>
        </div>
    </main>

    <script>
        const accent = '#365134';
        const statusColorMap = {
            'Pending': '#F57F17', 'Processing': '#1565C0',
            'Shipped': '#2E7D32', 'Delivered': '#9E9E9E', 'Cancelled': '#C62828'
        };

        // Dual Axis: Revenue (line) + Orders (bar)
        new Chart(document.getElementById('dualChart').getContext('2d'), {
            data: {
                labels: <?= json_encode($dailyLabels) ?>,
                datasets: [
                    {
                        type: 'bar', label: 'Orders', data: <?= json_encode($dailyOrders) ?>,
                        backgroundColor: 'rgba(54,81,52,0.12)', borderColor: 'transparent',
                        yAxisID: 'yOrders', borderRadius: 4, order: 2
                    },
                    {
                        type: 'line', label: 'Revenue', data: <?= json_encode($dailyRev) ?>,
                        borderColor: accent, borderWidth: 2.5, pointRadius: 3, pointBackgroundColor: accent,
                        fill: false, tension: 0.35, yAxisID: 'yRev', order: 1
                    }
                ]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: true, position: 'top', labels: { boxWidth: 10, font: { size: 12 } } },
                    tooltip: {
                        callbacks: {
                            label: ctx => ctx.dataset.label === 'Revenue'
                                ? 'Ksh ' + Number(ctx.raw).toLocaleString()
                                : ctx.raw + ' orders'
                        }
                    }
                },
                scales: {
                    yRev: { position: 'left', border: { display: false }, grid: { color: '#EFEFEF' }, ticks: { callback: v => 'Ksh ' + (v/1000).toFixed(0) + 'k' } },
                    yOrders: { position: 'right', border: { display: false }, grid: { display: false }, ticks: { stepSize: 1 } },
                    x: { border: { display: false }, grid: { display: false } }
                }
            }
        });

        // Status Donut
        new Chart(document.getElementById('statusChart').getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: <?= json_encode($statusLabels) ?>,
                datasets: [{ data: <?= json_encode($statusCounts) ?>, backgroundColor: <?= json_encode(array_map(fn($s) => $statusColorMap[$s] ?? '#ccc', $statusLabels)) ?>, borderWidth: 0 }]
            },
            options: {
                responsive: true, maintainAspectRatio: false, cutout: '70%',
                plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => ctx.label + ': ' + ctx.raw + ' orders' } } }
            }
        });

        // Category Horizontal Bar
        new Chart(document.getElementById('catChart').getContext('2d'), {
            type: 'bar',
            data: {
                labels: <?= json_encode($catLabels) ?>,
                datasets: [{ data: <?= json_encode($catVals) ?>, backgroundColor: accent, borderRadius: 4 }]
            },
            options: {
                indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => 'Ksh ' + Number(ctx.raw).toLocaleString() } } },
                scales: {
                    x: { border: { display: false }, grid: { color: '#EFEFEF' }, ticks: { callback: v => 'Ksh ' + (v/1000).toFixed(0) + 'k' } },
                    y: { border: { display: false }, grid: { display: false } }
                }
            }
        });
    </script>
</body>
</html>
