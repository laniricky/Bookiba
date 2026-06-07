<?php
if (!isset($pdo)) { require __DIR__ . '/../db.php'; }

// Live notification data
$pending_orders = $pdo->query("
    SELECT o.id, u.name as customer_name, o.total_amount, o.created_at
    FROM orders o JOIN users u ON o.user_id = u.id
    WHERE o.status = 'Pending'
    ORDER BY o.created_at DESC LIMIT 5
")->fetchAll(PDO::FETCH_ASSOC);

$pending_count = count($pending_orders);

$out_of_stock = $pdo->query("
    SELECT title FROM books WHERE inventory_count = 0 LIMIT 3
")->fetchAll(PDO::FETCH_COLUMN);

$total_alerts = $pending_count + count($out_of_stock);
?>
<header class="top-header">
    <!-- Global Search -->
    <div class="global-search-wrap" style="position:relative;">
        <div class="search-box">
            <svg style="width:15px;height:15px;color:var(--text-muted);flex-shrink:0;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
            <input type="text" id="globalSearch" placeholder="Search orders, books, customers…" autocomplete="off"
                style="border:none;outline:none;background:transparent;width:260px;font-size:13px;font-family:inherit;padding-left:8px;">
            <kbd style="font-size:10px;color:var(--text-muted);border:1px solid var(--border-color);border-radius:4px;padding:2px 5px;font-family:inherit;flex-shrink:0;">/</kbd>
        </div>
        <div id="searchDropdown" style="
            display:none; position:absolute; top:calc(100% + 8px); left:0; width:420px;
            background:white; border:1px solid var(--border-color); border-radius:var(--radius-lg);
            box-shadow:var(--shadow-lg); z-index:9999; overflow:hidden;
        "></div>
    </div>

    <div class="header-actions">
        <!-- Notification Bell -->
        <div style="position:relative;">
            <button id="notifBtn" style="background:none;border:none;cursor:pointer;position:relative;padding:8px;border-radius:var(--radius-sm);transition:background 0.15s;" onclick="toggleNotif()">
                <svg style="width:20px;height:20px;color:var(--text-main);" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
                <?php if ($total_alerts > 0): ?>
                <span style="position:absolute;top:4px;right:4px;background:#C62828;color:white;font-size:9px;font-weight:800;width:16px;height:16px;border-radius:50%;display:flex;align-items:center;justify-content:center;border:2px solid var(--bg-cream);"><?= min($total_alerts, 9) ?><?= $total_alerts > 9 ? '+' : '' ?></span>
                <?php endif; ?>
            </button>

            <!-- Notification dropdown -->
            <div id="notifPanel" style="
                display:none; position:absolute; top:calc(100% + 8px); right:0; width:360px;
                background:white; border:1px solid var(--border-color); border-radius:var(--radius-lg);
                box-shadow:var(--shadow-lg); z-index:9999; overflow:hidden;
            ">
                <div style="padding:16px 18px; border-bottom:1px solid var(--border-color); display:flex; justify-content:space-between; align-items:center;">
                    <span style="font-weight:800; font-size:14px;">Notifications</span>
                    <?php if ($total_alerts > 0): ?>
                    <span style="background:#FFEBEE; color:#C62828; font-size:11px; font-weight:700; padding:2px 8px; border-radius:10px;"><?= $total_alerts ?> new</span>
                    <?php endif; ?>
                </div>
                <div style="max-height:340px; overflow-y:auto;">
                    <?php foreach ($pending_orders as $po): ?>
                    <a href="orders.php?view=<?= $po['id'] ?>" style="display:flex; gap:12px; padding:14px 18px; border-bottom:1px solid var(--border-color); text-decoration:none; color:inherit; transition:background 0.15s;" onmouseover="this.style.background='#FAFAFA'" onmouseout="this.style.background=''">
                        <div style="width:36px;height:36px;border-radius:50%;background:#FFF3E0;color:#E65100;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
                            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>
                        </div>
                        <div style="flex:1; min-width:0;">
                            <div style="font-weight:700; font-size:13px;">New order from <?= htmlspecialchars($po['customer_name']) ?></div>
                            <div style="font-size:12px; color:var(--text-muted);">Ksh <?= number_format($po['total_amount']) ?> · <?= date('M j, g:i a', strtotime($po['created_at'])) ?></div>
                        </div>
                        <span style="font-size:10px; font-weight:700; padding:2px 8px; border-radius:10px; background:#FFF3E0; color:#E65100; flex-shrink:0; height:fit-content;">Pending</span>
                    </a>
                    <?php endforeach; ?>
                    <?php foreach ($out_of_stock as $title): ?>
                    <a href="inventory.php" style="display:flex; gap:12px; padding:14px 18px; border-bottom:1px solid var(--border-color); text-decoration:none; color:inherit; transition:background 0.15s;" onmouseover="this.style.background='#FAFAFA'" onmouseout="this.style.background=''">
                        <div style="width:36px;height:36px;border-radius:50%;background:#FFEBEE;color:#C62828;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
                            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
                        </div>
                        <div style="flex:1; min-width:0;">
                            <div style="font-weight:700; font-size:13px;">Out of stock</div>
                            <div style="font-size:12px; color:var(--text-muted); overflow:hidden; text-overflow:ellipsis; white-space:nowrap;"><?= htmlspecialchars($title) ?></div>
                        </div>
                        <span style="font-size:10px; font-weight:700; padding:2px 8px; border-radius:10px; background:#FFEBEE; color:#C62828; flex-shrink:0; height:fit-content;">Restock</span>
                    </a>
                    <?php endforeach; ?>
                    <?php if ($total_alerts === 0): ?>
                    <div style="padding:32px; text-align:center; color:var(--text-muted);">
                        <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="opacity:0.3; margin:0 auto 8px; display:block;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                        All caught up!
                    </div>
                    <?php endif; ?>
                </div>
                <a href="orders.php" style="display:block; text-align:center; padding:12px; font-size:13px; font-weight:700; color:var(--accent-green); text-decoration:none; border-top:1px solid var(--border-color);">View all orders</a>
            </div>
        </div>

        <!-- User Profile -->
        <div class="user-profile">
            <img src="https://ui-avatars.com/api/?name=Admin&background=365134&color=fff&size=64" class="user-avatar" style="width:32px;height:32px;">
            <span>Admin</span>
            <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7"/></svg>
        </div>
    </div>
</header>

<script>
// Global Search AJAX
const searchInput = document.getElementById('globalSearch');
const searchDrop  = document.getElementById('searchDropdown');
let searchTimer;

searchInput.addEventListener('input', function() {
    clearTimeout(searchTimer);
    const q = this.value.trim();
    if (q.length < 2) { searchDrop.style.display = 'none'; return; }
    searchTimer = setTimeout(() => {
        fetch('search_api.php?q=' + encodeURIComponent(q))
            .then(r => r.json())
            .then(data => renderSearchResults(data, q));
    }, 200);
});

searchInput.addEventListener('focus', function() {
    if (this.value.trim().length >= 2) searchDrop.style.display = 'block';
});

document.addEventListener('click', function(e) {
    if (!e.target.closest('.global-search-wrap') && !e.target.closest('#notifPanel') && !e.target.closest('#notifBtn')) {
        searchDrop.style.display = 'none';
        document.getElementById('notifPanel').style.display = 'none';
    }
});

// Keyboard shortcut: / to focus search
document.addEventListener('keydown', function(e) {
    if (e.key === '/' && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
        e.preventDefault();
        searchInput.focus();
    }
    if (e.key === 'Escape') { searchDrop.style.display = 'none'; searchInput.blur(); }
});

function renderSearchResults(data, q) {
    if (!data.results || data.results.length === 0) {
        searchDrop.innerHTML = '<div style="padding:24px; text-align:center; color:var(--text-muted); font-size:13px;">No results for "' + q + '"</div>';
        searchDrop.style.display = 'block';
        return;
    }
    let html = '';
    let lastType = '';
    data.results.forEach(r => {
        if (r.type !== lastType) {
            html += `<div style="padding:8px 16px 4px; font-size:10px; font-weight:800; text-transform:uppercase; letter-spacing:0.8px; color:var(--text-muted);">${r.type}</div>`;
            lastType = r.type;
        }
        html += `<a href="${r.url}" style="display:flex; align-items:center; gap:12px; padding:10px 16px; text-decoration:none; color:inherit; transition:background 0.1s;" onmouseover="this.style.background='#F8F5F0'" onmouseout="this.style.background=''">
            <div style="width:32px;height:32px;border-radius:8px;background:var(--accent-light);color:var(--accent-green);display:flex;align-items:center;justify-content:center;flex-shrink:0;font-weight:800;font-size:13px;">${r.icon}</div>
            <div style="flex:1; min-width:0;">
                <div style="font-weight:700; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${r.title}</div>
                <div style="font-size:12px; color:var(--text-muted);">${r.subtitle}</div>
            </div>
        </a>`;
    });
    searchDrop.innerHTML = html;
    searchDrop.style.display = 'block';
}

function toggleNotif() {
    const panel = document.getElementById('notifPanel');
    panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
}
</script>
