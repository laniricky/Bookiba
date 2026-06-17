<?php
require 'db.php';
require 'includes/auth_gate.php';

// Handle AJAX inline edit & delete
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';
    
    if ($action === 'update_field') {
        $field = $_POST['field'];
        $value = $_POST['value'];
        $id = $_POST['id'];
        
        $allowed = ['name', 'tag', 'sort_order'];
        if (in_array($field, $allowed)) {
            $stmt = $pdo->prepare("UPDATE themes SET $field = ? WHERE id = ?");
            $stmt->execute([$value, $id]);
        }
        echo json_encode(['ok' => true]);
        exit;
    }
    
    if ($action === 'toggle_active') {
        $id = $_POST['id'];
        $val = $_POST['is_active'] === 'true' ? 'true' : 'false';
        $stmt = $pdo->prepare("UPDATE themes SET is_active = ? WHERE id = ?");
        $stmt->execute([$val, $id]);
        echo json_encode(['ok' => true]);
        exit;
    }

    if ($action === 'delete') {
        $stmt = $pdo->prepare("DELETE FROM themes WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);
        exit;
    }
    
    if ($action === 'add') {
        $name = $_POST['name'] ?? '';
        $tag = $_POST['tag'] ?: null;
        $sort = (int)($_POST['sort_order'] ?? 0);
        $active = !empty($_POST['is_active']) ? 'true' : 'false';
        
        $stmt = $pdo->prepare("INSERT INTO themes (name, tag, sort_order, is_active) VALUES (?, ?, ?, ?)");
        $stmt->execute([$name, $tag, $sort, $active]);
        echo json_encode(['ok' => true]);
        exit;
    }
}

// Fetch all themes
$themes = $pdo->query("SELECT * FROM themes ORDER BY sort_order ASC")->fetchAll(PDO::FETCH_ASSOC);

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Themes | Bookiba Admin</title>
    <link rel="stylesheet" href="style.css">
    <script src="app.js"></script>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>
    <main class="main-content">
        <?php include 'includes/header.php'; ?>
        
        <div class="page-header">
            <h1 class="page-title">Themes</h1>
            <div class="page-actions">
                <button class="btn btn-primary" onclick="openDrawer()">Add Theme</button>
            </div>
        </div>

        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th>Display Name</th>
                        <th>Search Tag</th>
                        <th>Sort Order</th>
                        <th>Active</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach($themes as $t): ?>
                    <tr data-id="<?= $t['id'] ?>">
                        <td>
                            <input type="text" class="inline-edit" data-field="name" value="<?= htmlspecialchars($t['name']) ?>">
                        </td>
                        <td>
                            <input type="text" class="inline-edit" data-field="tag" value="<?= htmlspecialchars($t['tag'] ?? '') ?>">
                        </td>
                        <td>
                            <input type="number" class="inline-edit" data-field="sort_order" value="<?= $t['sort_order'] ?>" style="width: 80px;">
                        </td>
                        <td>
                            <label class="toggle-switch">
                                <input type="checkbox" class="toggle-active" <?= $t['is_active'] === 'true' || $t['is_active'] === true || $t['is_active'] === 1 ? 'checked' : '' ?>>
                                <span class="slider"></span>
                            </label>
                        </td>
                        <td>
                            <button class="btn-icon text-danger" onclick="deleteTheme(<?= $t['id'] ?>, this)" title="Delete Theme">
                                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            </button>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                    <?php if(empty($themes)): ?>
                    <tr><td colspan="5" style="text-align:center; padding: 40px; color: var(--text-muted);">No themes found.</td></tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </main>

    <!-- Slide-over Drawer -->
    <div id="drawer-overlay" class="drawer-overlay" onclick="closeDrawer()"></div>
    <div id="main-drawer" class="drawer">
        <div class="drawer-header">
            <div class="drawer-title">Add New Theme</div>
            <button class="drawer-close" onclick="closeDrawer()">×</button>
        </div>
        <div class="drawer-body">
            <form id="theme-form" onsubmit="event.preventDefault(); submitAdd();">
                <div class="form-group"><label>Display Name</label><input type="text" name="name" required placeholder="e.g. Keep me up all night"></div>
                <div class="form-group"><label>Search Tag</label><input type="text" name="tag" placeholder="e.g. thriller"></div>
                <div class="form-group"><label>Sort Order</label><input type="number" name="sort_order" value="0" required></div>
                <label style="display:flex; align-items:center; gap:6px; margin-bottom:20px; cursor:pointer;">
                    <input type="checkbox" name="is_active" checked> <span style="font-size:14px; font-weight:600">Active</span>
                </label>
            </form>
        </div>
        <div class="drawer-footer">
            <button class="btn btn-outline" onclick="closeDrawer()">Cancel</button>
            <button class="btn btn-primary" onclick="submitAdd()" style="margin-left:auto;">Save Theme</button>
        </div>
    </div>

    <script>
        document.querySelectorAll('.inline-edit').forEach(input => {
            let originalVal = input.value;
            input.addEventListener('blur', function() {
                if (this.value !== originalVal) {
                    saveField(this.closest('tr').dataset.id, this.dataset.field, this.value, this);
                    originalVal = this.value;
                }
            });
            input.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') this.blur();
            });
        });

        document.querySelectorAll('.toggle-active').forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                const tr = this.closest('tr');
                const id = tr.dataset.id;
                fetch('themes.php', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest'},
                    body: `action=toggle_active&id=${id}&is_active=${this.checked}`
                });
            });
        });

        function saveField(id, field, value, inputEl) {
            inputEl.style.backgroundColor = '#f0fdf4';
            fetch('themes.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest'},
                body: `action=update_field&id=${id}&field=${field}&value=${encodeURIComponent(value)}`
            }).then(() => setTimeout(() => inputEl.style.backgroundColor='', 500));
        }

        function deleteTheme(id, btn) {
            if (confirm('Are you sure you want to delete this theme?')) {
                fetch('themes.php', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest'},
                    body: `action=delete&id=${id}`
                }).then(() => btn.closest('tr').remove());
            }
        }

        function submitAdd() {
            const form = document.getElementById('theme-form');
            if(!form.checkValidity()) { form.reportValidity(); return; }
            
            const fd = new FormData(form);
            fd.append('action', 'add');
            
            fetch('themes.php', {
                method: 'POST',
                headers: {'X-Requested-With': 'XMLHttpRequest'},
                body: new URLSearchParams(fd)
            }).then(() => location.reload());
        }

        function openDrawer() {
            document.getElementById('theme-form').reset();
            document.getElementById('drawer-overlay').classList.add('show');
            document.getElementById('main-drawer').classList.add('open');
        }
        function closeDrawer() {
            document.getElementById('drawer-overlay').classList.remove('show');
            document.getElementById('main-drawer').classList.remove('open');
        }
    </script>
</body>
</html>
