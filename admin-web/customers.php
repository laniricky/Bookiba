<?php
require 'db.php';

// Segment definitions
$segment = $_GET['segment'] ?? 'all';
$search = $_GET['q'] ?? '';

// All customers with full stats
$base_query = "
    SELECT u.id, u.name, u.email, u.created_at,
           COUNT(DISTINCT o.id) as total_orders,
           COALESCE(SUM(o.total_amount), 0) as lifetime_value,
           MAX(o.created_at) as last_order_date,
           COALESCE(AVG(o.total_amount), 0) as avg_order_value
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id AND o.status != 'Cancelled'
    GROUP BY u.id
";
$all_customers = $pdo->query($base_query)->fetchAll(PDO::FETCH_ASSOC);

// Compute segments
$now = new DateTime();
$segments = ['all' => [], 'vip' => [], 'at_risk' => [], 'new' => [], 'dormant' => []];
foreach ($all_customers as $c) {
    $segments['all'][] = $c;
    $daysSince = $c['last_order_date'] ? (int)$now->diff(new DateTime($c['last_order_date']))->days : 9999;
    $joinedDaysAgo = (int)$now->diff(new DateTime($c['created_at']))->days;

    if ($c['lifetime_value'] >= 3000 || $c['total_orders'] >= 5) $segments['vip'][] = $c;
    elseif ($c['last_order_date'] && $daysSince > 60 && $daysSince <= 180) $segments['at_risk'][] = $c;
    elseif ($joinedDaysAgo <= 30) $segments['new'][] = $c;
    elseif (!$c['last_order_date'] || $daysSince > 180) $segments['dormant'][] = $c;
}

$customers = $segments[$segment] ?? $segments['all'];
if ($search) {
    $customers = array_filter($customers, fn($c) => stripos($c['name'], $search) !== false || stripos($c['email'], $search) !== false);
}

// Summary stats
$total_revenue = $pdo->query("SELECT SUM(total_amount) FROM orders WHERE status != 'Cancelled'")->fetchColumn() ?: 0;
$total_all = count($segments['all']);
$avg_ltv = $total_all > 0 ? array_sum(array_column($segments['all'], 'lifetime_value')) / $total_all : 0;

// Monthly acquisition data for chart (last 6 months)
$monthly = $pdo->query("
    SELECT strftime('%Y-%m', created_at) as month, COUNT(*) as cnt
    FROM users
    WHERE created_at >= date('now', '-6 months')
    GROUP BY month ORDER BY month ASC
")->fetchAll(PDO::FETCH_ASSOC);
$month_labels = array_column($monthly, 'month');
$month_counts = array_column($monthly, 'cnt');

// Sparkline data per customer (last 6 months spending)
function getSparkline($pdo, $user_id) {
    $rows = $pdo->prepare("
        SELECT strftime('%Y-%m', created_at) as m, SUM(total_amount) as v
        FROM orders WHERE user_id = ? AND status != 'Cancelled'
        AND created_at >= date('now', '-6 months')
        GROUP BY m ORDER BY m
    ");
    $rows->execute([$user_id]);
    return array_column($rows->fetchAll(PDO::FETCH_ASSOC), 'v');
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Customers | The Book Nook</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .segment-tabs { display: flex; gap: 4px; border-bottom: 2px solid var(--border-color); margin-bottom: 24px; }
        .seg-tab { padding: 10px 18px; font-size: 13px; font-weight: 700; cursor: pointer; border: none; background: none; color: var(--text-muted); border-bottom: 2px solid transparent; margin-bottom: -2px; position: relative; display: flex; align-items: center; gap: 8px; transition: color 0.15s; }
        .seg-tab.active { color: var(--accent-green); border-bottom-color: var(--accent-green); }
        .seg-tab:hover { color: var(--text-main); }
        .seg-badge { font-size: 10px; font-weight: 800; padding: 2px 7px; border-radius: 10px; }
        .badge-all { background: #EEEEEE; color: #424242; }
        .badge-vip { background: #FFF8E1; color: #F57F17; }
        .badge-risk { background: #FFEBEE; color: #C62828; }
        .badge-new { background: #E8F5E9; color: #2E7D32; }
        .badge-dormant { background: #F5F5F5; color: #757575; }

        .crm-table { width: 100%; border-collapse: collapse; }
        .crm-table th { padding: 10px 16px; font-size: 11px; text-transform: uppercase; letter-spacing: 0.8px; color: var(--text-muted); font-weight: 700; border-bottom: 2px solid var(--border-color); text-align: left; }
        .crm-table td { padding: 14px 16px; border-bottom: 1px solid var(--border-color); vertical-align: middle; font-size: 14px; }
        .crm-table tbody tr { transition: background 0.15s; }
        .crm-table tbody tr:hover { background: #FAFAFA; }
        .c-avatar { width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 15px; flex-shrink: 0; }
        .ltv-tier { display: inline-flex; align-items: center; gap: 5px; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
        .tier-platinum { background: #EDE7F6; color: #4527A0; }
        .tier-gold { background: #FFF8E1; color: #E65100; }
        .tier-silver { background: #FAFAFA; color: #616161; }

        /* Sparkline canvas in table cells */
        .sparkline { width: 80px; height: 32px; }

        /* Right panel for segment insight */
        .crm-layout { display: grid; grid-template-columns: 1fr 280px; gap: 24px; align-items: start; }
        .insight-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 20px; }
        .insight-title { font-weight: 800; font-size: 14px; margin-bottom: 14px; }
        .insight-stat { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--border-color); font-size: 14px; }
        .insight-stat:last-child { border-bottom: none; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:20px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Customers</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= $total_all ?> total customers · Avg LTV Ksh <?= number_format($avg_ltv) ?></p>
            </div>
            <div style="display:flex; gap:12px;">
                <div style="display:flex; align-items:center; gap:8px; border:1px solid var(--border-color); border-radius:var(--radius-sm); padding:7px 12px; background:white;">
                    <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                    <input type="text" id="custSearch" placeholder="Search customers…" value="<?= htmlspecialchars($search) ?>" style="border:none; outline:none; font-size:13px; font-family:inherit; width:160px;">
                </div>
                <button class="btn btn-outline" style="display:flex; align-items:center; gap:8px;">
                    <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                    Export
                </button>
            </div>
        </div>

        <!-- Segment Tabs -->
        <div class="segment-tabs">
            <?php
            $tabDefs = [
                ['key'=>'all','label'=>'All Customers','badgeClass'=>'badge-all'],
                ['key'=>'vip','label'=>'VIP','badgeClass'=>'badge-vip'],
                ['key'=>'at_risk','label'=>'At Risk','badgeClass'=>'badge-risk'],
                ['key'=>'new','label'=>'New (30d)','badgeClass'=>'badge-new'],
                ['key'=>'dormant','label'=>'Dormant','badgeClass'=>'badge-dormant'],
            ];
            foreach($tabDefs as $t): ?>
            <a href="?segment=<?= $t['key'] ?><?= $search ? '&q='.urlencode($search) : '' ?>" class="seg-tab <?= $segment === $t['key'] ? 'active' : '' ?>">
                <?= $t['label'] ?>
                <span class="seg-badge <?= $t['badgeClass'] ?>"><?= count($segments[$t['key']]) ?></span>
            </a>
            <?php endforeach; ?>
        </div>

        <div class="crm-layout">
            <div class="card" style="padding:0; overflow:hidden;">
                <table class="crm-table">
                    <thead>
                        <tr>
                            <th>Customer</th>
                            <th>Activity (6mo)</th>
                            <th>Orders</th>
                            <th>Avg. Order</th>
                            <th>Lifetime Value</th>
                            <th>Last Order</th>
                            <th>Tier</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach($customers as $c):
                            $ltv = (float)$c['lifetime_value'];
                            $orders = (int)$c['total_orders'];
                            $spark = getSparkline($pdo, $c['id']);
                            // Tier logic
                            if ($ltv >= 5000 || $orders >= 10) { $tierClass = 'tier-platinum'; $tierLabel = 'Platinum'; }
                            elseif ($ltv >= 2000 || $orders >= 5) { $tierClass = 'tier-gold'; $tierLabel = 'Gold'; }
                            else { $tierClass = 'tier-silver'; $tierLabel = 'Regular'; }
                            $daysSince = $c['last_order_date'] ? (int)(new DateTime())->diff(new DateTime($c['last_order_date']))->days : null;
                            $colors = ['#4285F4','#EA4335','#34A853','#FBBC05','#9C27B0','#FF6D00'];
                            $avatarColor = $colors[crc32($c['id']) % count($colors)];
                        ?>
                        <tr>
                            <td>
                                <div style="display:flex; align-items:center; gap:12px;">
                                    <div class="c-avatar" style="background: <?= $avatarColor ?>22; color: <?= $avatarColor ?>;">
                                        <?= strtoupper(substr($c['name'],0,1)) ?>
                                    </div>
                                    <div>
                                        <div style="font-weight:700;"><?= htmlspecialchars($c['name']) ?></div>
                                        <div style="font-size:12px; color:var(--text-muted);"><?= htmlspecialchars($c['email']) ?></div>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <?php if(!empty($spark)): ?>
                                <canvas class="sparkline" data-values="<?= htmlspecialchars(json_encode($spark)) ?>"></canvas>
                                <?php else: ?>
                                <span style="font-size:12px; color:var(--text-muted);">No activity</span>
                                <?php endif; ?>
                            </td>
                            <td style="font-weight:700; font-size:16px;"><?= $orders ?></td>
                            <td style="color:var(--text-muted);">Ksh <?= $orders > 0 ? number_format($c['avg_order_value']) : '—' ?></td>
                            <td style="font-weight:800; font-size:15px; color:var(--accent-green);">Ksh <?= number_format($ltv) ?></td>
                            <td style="color:var(--text-muted); font-size:13px;">
                                <?php if($c['last_order_date']): ?>
                                <?= date('M j, Y', strtotime($c['last_order_date'])) ?>
                                <div style="font-size:11px;"><?= $daysSince ?> days ago</div>
                                <?php else: ?><span style="color:var(--border-color);">Never</span><?php endif; ?>
                            </td>
                            <td><span class="ltv-tier <?= $tierClass ?>"><?= $tierLabel ?></span></td>
                        </tr>
                        <?php endforeach; ?>
                        <?php if(empty($customers)): ?>
                        <tr><td colspan="7" style="padding:40px; text-align:center; color:var(--text-muted);">No customers in this segment.</td></tr>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>

            <!-- Insight Panel -->
            <div style="display:flex; flex-direction:column; gap:20px;">
                <div class="insight-card">
                    <div class="insight-title">Segment Breakdown</div>
                    <?php foreach($tabDefs as $t): ?>
                    <div class="insight-stat">
                        <span style="color:var(--text-muted);"><?= $t['label'] ?></span>
                        <strong><?= count($segments[$t['key']]) ?></strong>
                    </div>
                    <?php endforeach; ?>
                </div>
                <div class="insight-card">
                    <div class="insight-title">Acquisition (6mo)</div>
                    <div style="position:relative; height:160px;">
                        <canvas id="acqChart"></canvas>
                    </div>
                </div>
                <div class="insight-card">
                    <div class="insight-title">Quick Actions</div>
                    <button class="btn btn-outline" style="width:100%; margin-bottom:10px; justify-content:center; display:flex; align-items:center; gap:8px; font-size:13px;">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>
                        Email Segment
                    </button>
                    <button class="btn btn-outline" style="width:100%; justify-content:center; display:flex; align-items:center; gap:8px; font-size:13px;">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/></svg>
                        Offer Discount
                    </button>
                </div>
            </div>
        </div>
    </main>

    <script>
        // Sparklines
        document.querySelectorAll('.sparkline').forEach(canvas => {
            const vals = JSON.parse(canvas.dataset.values || '[]');
            if (!vals.length) return;
            new Chart(canvas.getContext('2d'), {
                type: 'line',
                data: {
                    labels: vals.map((_,i) => i),
                    datasets: [{ data: vals, borderColor: '#365134', borderWidth: 2, pointRadius: 0, fill: true, backgroundColor: 'rgba(54,81,52,0.1)', tension: 0.4 }]
                },
                options: { responsive: false, plugins: { legend: { display: false }, tooltip: { enabled: false } }, scales: { x: { display: false }, y: { display: false } }, animation: false }
            });
        });

        // Acquisition chart
        new Chart(document.getElementById('acqChart').getContext('2d'), {
            type: 'bar',
            data: {
                labels: <?= json_encode($month_labels) ?>,
                datasets: [{ data: <?= json_encode($month_counts) ?>, backgroundColor: '#365134', borderRadius: 4 }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { display: false }, border: { display: false } }, y: { beginAtZero: true, border: { display: false }, grid: { color: 'rgba(0,0,0,0.04)' } } } }
        });

        // Search
        document.getElementById('custSearch').addEventListener('input', function() {
            const url = new URL(window.location);
            if(this.value) url.searchParams.set('q', this.value);
            else url.searchParams.delete('q');
            clearTimeout(this._t);
            this._t = setTimeout(() => window.location.href = url.toString(), 400);
        });
    </script>
</body>
</html>
