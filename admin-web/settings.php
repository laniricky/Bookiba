<?php
require 'db.php';
require 'includes/auth_gate.php';
$page = 'settings.php';

// ── Load current settings from DB ─────────────────────────────────────────────
function getSetting(PDO $pdo, string $key, string $default = ''): string {
    $stmt = $pdo->prepare("SELECT value FROM store_settings WHERE key = ?");
    $stmt->execute([$key]);
    $row = $stmt->fetch();
    return $row ? $row['value'] : $default;
}

// ── Handle form save ──────────────────────────────────────────────────────────
$success = false;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $fields = [
        'store_name'     => $_POST['store_name']     ?? '',
        'support_email'  => $_POST['support_email']  ?? '',
        'phone'          => $_POST['phone']           ?? '',
        'timezone'       => $_POST['timezone']        ?? 'Africa/Nairobi',
        'address'        => $_POST['address']         ?? '',
        'currency'       => $_POST['currency']        ?? 'KES',
        'tax_rate'       => $_POST['tax_rate']        ?? '16',
        'notif_orders'   => isset($_POST['notif_orders'])   ? '1' : '0',
        'notif_stock'    => isset($_POST['notif_stock'])    ? '1' : '0',
        'notif_weekly'   => isset($_POST['notif_weekly'])   ? '1' : '0',
        'admin_name'     => $_POST['admin_name']      ?? '',
        'admin_email'    => $_POST['admin_email']     ?? '',
    ];
    // Handle optional password change
    if (!empty($_POST['new_password'])) {
        $fields['admin_pass_hash'] = password_hash($_POST['new_password'], PASSWORD_BCRYPT);
    }

    $stmt = $pdo->prepare("INSERT INTO store_settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value");
    foreach ($fields as $k => $v) {
        $stmt->execute([$k, $v]);
    }
    $success = true;
}

// ── Fetch live values ─────────────────────────────────────────────────────────
$s = [
    'store_name'    => getSetting($pdo, 'store_name',    'Bookiba'),
    'support_email' => getSetting($pdo, 'support_email', 'support@bookiba.co.ke'),
    'phone'         => getSetting($pdo, 'phone',         '+254 700 123 456'),
    'timezone'      => getSetting($pdo, 'timezone',      'Africa/Nairobi'),
    'address'       => getSetting($pdo, 'address',       'Moi Avenue, CBD' . "\n" . 'Nairobi, Kenya'),
    'currency'      => getSetting($pdo, 'currency',      'KES'),
    'tax_rate'      => getSetting($pdo, 'tax_rate',      '16'),
    'notif_orders'  => getSetting($pdo, 'notif_orders',  '1'),
    'notif_stock'   => getSetting($pdo, 'notif_stock',   '1'),
    'notif_weekly'  => getSetting($pdo, 'notif_weekly',  '0'),
    'admin_name'    => getSetting($pdo, 'admin_name',    'Admin'),
    'admin_email'   => getSetting($pdo, 'admin_email',   'admin@bookiba.co.ke'),
];

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Settings | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .settings-layout { display: grid; grid-template-columns: 240px 1fr; gap: 40px; align-items: start; max-width: 1000px; margin: 0 auto; }
        
        .settings-nav { display: flex; flex-direction: column; gap: 8px; position: sticky; top: 120px; }
        .settings-nav a { padding: 10px 14px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 600; color: var(--text-muted); text-decoration: none; transition: all 0.15s; }
        .settings-nav a:hover { color: var(--text-main); background: rgba(0,0,0,0.02); }
        .settings-nav a.active { color: var(--accent-green); background: var(--accent-light); }

        .settings-section { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 32px; margin-bottom: 24px; box-shadow: var(--shadow-sm); }
        .settings-title { font-size: 18px; font-weight: 800; margin-bottom: 6px; }
        .settings-desc { font-size: 13px; color: var(--text-muted); margin-bottom: 24px; }
        
        .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
        .form-group label { display: block; font-size: 12px; font-weight: 700; color: var(--text-main); margin-bottom: 8px; text-transform: none; letter-spacing: 0; }
        .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 14px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 14px; font-family: inherit; background: var(--bg-cream); transition: border-color 0.15s, box-shadow 0.15s; }
        .form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: var(--accent-green); box-shadow: 0 0 0 3px rgba(54,81,52,0.08); outline: none; background: white; }
        .form-group .hint { font-size: 11px; color: var(--text-muted); margin-top: 6px; }

        .toggle-wrap { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; border-bottom: 1px solid var(--border-color); }
        .toggle-wrap:last-child { border-bottom: none; }
        .toggle-label { font-size: 14px; font-weight: 600; color: var(--text-main); }
        .toggle-desc { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
        
        /* CSS Toggle Switch */
        .switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
        .switch input { opacity: 0; width: 0; height: 0; }
        .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #E0E0E0; transition: .3s; border-radius: 24px; }
        .slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .3s; border-radius: 50%; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        input:checked + .slider { background-color: var(--accent-green); }
        input:checked + .slider:before { transform: translateX(20px); }

        .save-bar { position: sticky; bottom: 24px; background: rgba(255,255,255,0.9); backdrop-filter: blur(8px); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; box-shadow: var(--shadow-md); z-index: 100; margin-top: 40px; }

        /* Toast */
        .toast { position: fixed; bottom: 24px; right: 24px; background: var(--accent-green); color: white; padding: 12px 20px; border-radius: var(--radius-sm); font-weight: 600; font-size: 13px; box-shadow: var(--shadow-lg); display: flex; align-items: center; gap: 8px; transform: translateY(100px); opacity: 0; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); z-index: 9999; }
        .toast.show { transform: translateY(0); opacity: 1; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="margin-bottom:32px;">
            <h1 style="font-size:26px; font-weight:900; letter-spacing:-0.5px;">Settings</h1>
            <p style="color:var(--text-muted); font-size:14px; margin-top:4px;">Manage your store configuration, notifications, and profile.</p>
        </div>

        <form method="POST" action="settings.php">
            <div class="settings-layout">
                <div class="settings-nav">
                    <a href="#store" class="active" onclick="navTo(this, 'store')">Store Details</a>
                    <a href="#payments" onclick="navTo(this, 'payments')">Payments & Currency</a>
                    <a href="#notifications" onclick="navTo(this, 'notifications')">Notifications</a>
                    <a href="#profile" onclick="navTo(this, 'profile')">Admin Profile</a>
                </div>

                <div>
                    <!-- Store Details -->
                    <div id="store" class="settings-section">
                        <div class="settings-title">Store Details</div>
                        <div class="settings-desc">This information is displayed publicly on your storefront and receipts.</div>
                        
                        <div class="form-row">
                            <div class="form-group">
                                <label>Store Name</label>
                                <input type="text" name="store_name" value="<?= htmlspecialchars($s['store_name']) ?>" required>
                            </div>
                            <div class="form-group">
                                <label>Support Email</label>
                                <input type="email" name="support_email" value="<?= htmlspecialchars($s['support_email']) ?>">
                            </div>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label>Phone Number</label>
                                <input type="text" name="phone" value="<?= htmlspecialchars($s['phone']) ?>">
                            </div>
                            <div class="form-group">
                                <label>Timezone</label>
                                <select name="timezone">
                                    <option value="Africa/Nairobi" <?= $s['timezone'] === 'Africa/Nairobi' ? 'selected' : '' ?>>Africa/Nairobi (EAT)</option>
                                    <option value="UTC" <?= $s['timezone'] === 'UTC' ? 'selected' : '' ?>>UTC</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group">
                            <label>Physical Address</label>
                            <textarea name="address" rows="3"><?= htmlspecialchars($s['address']) ?></textarea>
                            <div class="hint">Leave blank if you operate purely online.</div>
                        </div>
                    </div>

                    <!-- Payments -->
                    <div id="payments" class="settings-section">
                        <div class="settings-title">Payments & Currency</div>
                        <div class="settings-desc">Configure how you accept payments and display prices.</div>
                        
                        <div class="form-row">
                            <div class="form-group">
                                <label>Base Currency</label>
                                <select name="currency">
                                    <option value="KES" <?= $s['currency'] === 'KES' ? 'selected' : '' ?>>Kenyan Shilling (Ksh)</option>
                                    <option value="USD" <?= $s['currency'] === 'USD' ? 'selected' : '' ?>>US Dollar ($)</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>Tax Rate (%)</label>
                                <input type="number" name="tax_rate" value="<?= htmlspecialchars($s['tax_rate']) ?>" min="0" max="100">
                            </div>
                        </div>

                        <div style="margin-top:24px; padding:20px; background:var(--bg-cream); border-radius:var(--radius-md); border:1px solid var(--border-color);">
                            <div style="display:flex; justify-content:space-between; align-items:center;">
                                <div>
                                    <div style="font-weight:700; font-size:14px; margin-bottom:4px;">M-PESA Integration</div>
                                    <div style="font-size:12px; color:var(--text-muted);">Receive payments via M-PESA Daraja API.</div>
                                </div>
                                <span style="background:#E8F5E9; color:#2E7D32; font-size:11px; font-weight:700; padding:4px 10px; border-radius:12px;">Connected</span>
                            </div>
                        </div>
                    </div>

                    <!-- Notifications -->
                    <div id="notifications" class="settings-section">
                        <div class="settings-title">Notifications</div>
                        <div class="settings-desc">Choose what alerts you want to receive.</div>
                        
                        <div class="toggle-wrap">
                            <div>
                                <div class="toggle-label">New Order Alerts</div>
                                <div class="toggle-desc">Get an email whenever a customer places an order.</div>
                            </div>
                            <label class="switch"><input type="checkbox" name="notif_orders" <?= $s['notif_orders'] === '1' ? 'checked' : '' ?>><span class="slider"></span></label>
                        </div>
                        <div class="toggle-wrap">
                            <div>
                                <div class="toggle-label">Low Stock Warnings</div>
                                <div class="toggle-desc">Alert when a product drops below 10 units.</div>
                            </div>
                            <label class="switch"><input type="checkbox" name="notif_stock" <?= $s['notif_stock'] === '1' ? 'checked' : '' ?>><span class="slider"></span></label>
                        </div>
                        <div class="toggle-wrap">
                            <div>
                                <div class="toggle-label">Weekly Summary</div>
                                <div class="toggle-desc">Receive a report of sales and performance every Monday.</div>
                            </div>
                            <label class="switch"><input type="checkbox" name="notif_weekly" <?= $s['notif_weekly'] === '1' ? 'checked' : '' ?>><span class="slider"></span></label>
                        </div>
                    </div>

                    <!-- Profile -->
                    <div id="profile" class="settings-section">
                        <div class="settings-title">Admin Profile</div>
                        <div class="settings-desc">Update your login details and avatar.</div>

                        <div style="display:flex; gap:20px; align-items:center; margin-bottom:24px;">
                            <div style="width:64px; height:64px; border-radius:50%; background:#1A1512; display:flex; align-items:center; justify-content:center;">
                                <span style="color:#F5F0E8; font-size:22px; font-weight:800;"><?= strtoupper(substr($s['admin_name'], 0, 1)) ?></span>
                            </div>
                        </div>

                        <div class="form-row">
                            <div class="form-group">
                                <label>Full Name</label>
                                <input type="text" name="admin_name" value="<?= htmlspecialchars($s['admin_name']) ?>" required>
                            </div>
                            <div class="form-group">
                                <label>Login Email</label>
                                <input type="email" name="admin_email" value="<?= htmlspecialchars($s['admin_email']) ?>" required>
                            </div>
                        </div>
                        <div class="form-group">
                            <label>New Password</label>
                            <input type="password" name="new_password" placeholder="Leave blank to keep current password">
                        </div>
                    </div>

                    <div class="save-bar">
                        <span style="font-size:13px; color:var(--text-muted);">Unsaved changes</span>
                        <button type="submit" class="btn btn-primary" style="padding:10px 24px; font-size:14px;">Save Settings</button>
                    </div>

                </div>
            </div>
        </form>

        <?php if ($success): ?>
        <div id="toast" class="toast">
            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
            Settings saved successfully.
        </div>
        <script>
            setTimeout(() => { document.getElementById('toast').classList.add('show'); }, 100);
            setTimeout(() => { document.getElementById('toast').classList.remove('show'); }, 3000);
        </script>
        <?php endif; ?>

    </main>

    <script>
        // Simple scroll spy / nav logic
        function navTo(el, id) {
            document.querySelectorAll('.settings-nav a').forEach(a => a.classList.remove('active'));
            el.classList.add('active');
        }

        // Detect unsaved changes
        const form = document.querySelector('form');
        const saveBarText = document.querySelector('.save-bar span');
        let initialData = new FormData(form);

        form.addEventListener('input', () => {
            saveBarText.textContent = "Unsaved changes";
            saveBarText.style.color = "var(--text-main)";
            saveBarText.style.fontWeight = "600";
        });
    </script>
</body>
</html>
