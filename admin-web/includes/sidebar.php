<?php
$page = basename($_SERVER['PHP_SELF']);

// Pull live counts for badges
if (!isset($pdo)) { require __DIR__ . '/../db.php'; }
$pending_count = (int)$pdo->query("SELECT COUNT(*) FROM orders WHERE status = 'Pending'")->fetchColumn();
$low_stock_count = (int)$pdo->query("SELECT COUNT(*) FROM books WHERE inventory_count > 0 AND inventory_count <= 10")->fetchColumn();
$out_stock_count = (int)$pdo->query("SELECT COUNT(*) FROM books WHERE inventory_count = 0")->fetchColumn();
?>
<aside class="sidebar">
    <div class="sidebar-logo">
        <svg style="width:28px; height:28px; color:var(--accent-green); flex-shrink:0;" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
        </svg>
        <div>
            Bookiba
            <span class="sidebar-subtitle">Admin Dashboard</span>
        </div>
    </div>

    <nav style="flex: 1;">
        <a href="index.php" class="<?= $page == 'index.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
            Overview
        </a>
        <a href="orders.php" class="<?= $page == 'orders.php' ? 'active' : '' ?>" style="justify-content:space-between;">
            <span style="display:flex; align-items:center; gap:10px;">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"></path></svg>
                Orders
            </span>
            <?php if ($pending_count > 0): ?>
            <span style="background:#E65100; color:white; font-size:10px; font-weight:800; padding:2px 7px; border-radius:10px; flex-shrink:0;"><?= $pending_count ?></span>
            <?php endif; ?>
        </a>
        <a href="products.php" class="<?= $page == 'products.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path></svg>
            Products
        </a>
        <a href="customers.php" class="<?= $page == 'customers.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
            Customers
        </a>
        <a href="inventory.php" class="<?= $page == 'inventory.php' ? 'active' : '' ?>" style="justify-content:space-between;">
            <span style="display:flex; align-items:center; gap:10px;">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"></path></svg>
                Inventory
            </span>
            <?php if ($out_stock_count > 0): ?>
            <span style="background:#C62828; color:white; font-size:10px; font-weight:800; padding:2px 7px; border-radius:10px; flex-shrink:0;"><?= $out_stock_count ?></span>
            <?php elseif ($low_stock_count > 0): ?>
            <span style="background:#F57F17; color:white; font-size:10px; font-weight:800; padding:2px 7px; border-radius:10px; flex-shrink:0;"><?= $low_stock_count ?></span>
            <?php endif; ?>
        </a>
        <a href="categories.php" class="<?= $page == 'categories.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
            Categories
        </a>
        <a href="reels.php" class="<?= $page == 'reels.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.723v6.554a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path></svg>
            Reels
        </a>
        <a href="analytics.php" class="<?= $page == 'analytics.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
            Reports
        </a>

        <div style="height:1px; background:var(--border-color); margin:12px 0;"></div>

        <a href="settings.php" class="<?= $page == 'settings.php' ? 'active' : '' ?>">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            Settings
        </a>
    </nav>

    <div class="store-status">
        <div class="store-status-title">Store Status</div>
        <div class="status-indicator"><div class="status-dot"></div> Online</div>
        <a href="#" class="btn-store" style="display:flex; align-items:center; justify-content:center; gap:6px; text-decoration:none; color:var(--text-main);">
            <svg width="13" height="13" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/></svg>
            View Live Store
        </a>
    </div>
</aside>
