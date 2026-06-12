<?php
require 'db.php';
require 'includes/auth_gate.php';

// Handle AJAX actions
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';
    
    if ($action === 'create') {
        $id = bin2hex(random_bytes(16));
        $stmt = $pdo->prepare("INSERT INTO banners (id, image_url, title, subtitle, sort_order) VALUES (?, ?, ?, ?, ?)");
        $sort = (int)($pdo->query("SELECT MAX(sort_order) FROM banners")->fetchColumn()) + 1;
        $stmt->execute([$id, $_POST['image_url'], $_POST['title'], $_POST['subtitle'], $sort]);
        echo json_encode(['ok' => true]);
    } elseif ($action === 'delete') {
        $stmt = $pdo->prepare("DELETE FROM banners WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);
    } elseif ($action === 'toggle_active') {
        $stmt = $pdo->prepare("UPDATE banners SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);
    } elseif ($action === 'reorder') {
        $ids = json_decode($_POST['ids']);
        $stmt = $pdo->prepare("UPDATE banners SET sort_order = ? WHERE id = ?");
        foreach ($ids as $index => $id) {
            $stmt->execute([$index, $id]);
        }
        echo json_encode(['ok' => true]);
    }
    exit;
}

// Fetch Banners
$banners = $pdo->query("SELECT * FROM banners ORDER BY sort_order ASC")->fetchAll(PDO::FETCH_ASSOC);

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Marketing Banners | Bookiba</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://upload-widget.cloudinary.com/global/all.js" type="text/javascript"></script>
    <style>
        .banner-card { display: flex; gap: 16px; background: white; padding: 16px; border-radius: 8px; border: 1px solid var(--border-color); margin-bottom: 12px; align-items: center; }
        .banner-img { width: 160px; height: 90px; object-fit: cover; border-radius: 4px; background: #eee; }
        .banner-info { flex: 1; }
        .banner-title { font-weight: bold; font-size: 16px; margin-bottom: 4px; }
        .banner-subtitle { color: var(--text-muted); font-size: 14px; }
        .banner-actions { display: flex; gap: 8px; }
        
        .drag-handle { cursor: grab; color: var(--text-muted); padding: 8px; }
        
        .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); align-items: center; justify-content: center; z-index: 1000; }
        .modal.active { display: flex; }
        .modal-content { background: white; padding: 24px; border-radius: 12px; width: 400px; max-width: 90%; }
        .form-group { margin-bottom: 16px; }
        .form-group label { display: block; margin-bottom: 8px; font-weight: 600; font-size: 14px; }
        .form-control { width: 100%; padding: 10px; border: 1px solid var(--border-color); border-radius: 6px; box-sizing: border-box; }
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
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Marketing Banners</h1>
                <p style="color:var(--text-muted); font-size:14px;">Manage homepage carousel banners</p>
            </div>
            <button class="btn btn-primary" onclick="showModal()">+ New Banner</button>
        </div>

        <div id="banner-list">
            <?php foreach ($banners as $b): ?>
            <div class="banner-card" data-id="<?= $b['id'] ?>" draggable="true">
                <div class="drag-handle">☰</div>
                <img src="<?= htmlspecialchars($b['image_url']) ?>" class="banner-img">
                <div class="banner-info">
                    <div class="banner-title"><?= htmlspecialchars($b['title']) ?></div>
                    <div class="banner-subtitle"><?= htmlspecialchars($b['subtitle']) ?></div>
                    <div style="margin-top:8px;">
                        <span style="font-size:12px; padding:2px 8px; border-radius:12px; background:<?= $b['is_active'] ? '#E8F5E9; color:#2E7D32' : '#FFEBEE; color:#C62828' ?>;">
                            <?= $b['is_active'] ? 'Active' : 'Inactive' ?>
                        </span>
                    </div>
                </div>
                <div class="banner-actions">
                    <button class="btn btn-outline" onclick="toggleActive('<?= $b['id'] ?>')"><?= $b['is_active'] ? 'Deactivate' : 'Activate' ?></button>
                    <button class="btn btn-outline" style="color:#C62828; border-color:#FFCDD2;" onclick="deleteBanner('<?= $b['id'] ?>')">Delete</button>
                </div>
            </div>
            <?php endforeach; ?>
            <?php if (empty($banners)): ?>
            <div style="padding:40px; text-align:center; color:var(--text-muted); border:1px dashed var(--border-color); border-radius:8px;">
                No banners added yet.
            </div>
            <?php endif; ?>
        </div>
    </main>

    <!-- Add Banner Modal -->
    <div class="modal" id="addModal">
        <div class="modal-content">
            <h2 style="margin-top:0; margin-bottom:20px;">Add Banner</h2>
            <form id="addForm" onsubmit="submitBanner(event)">
                <div class="form-group">
                    <label>Image *</label>
                    <div style="display:flex; gap:10px; align-items:center;">
                        <img id="previewImg" style="width:100px; height:56px; object-fit:cover; border-radius:4px; display:none; background:#eee;">
                        <button type="button" class="btn btn-outline" id="uploadWidget">Upload Image</button>
                        <input type="hidden" id="imageUrl" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>Title (Optional)</label>
                    <input type="text" class="form-control" id="bannerTitle" placeholder="e.g. Summer Sale">
                </div>
                <div class="form-group">
                    <label>Subtitle (Optional)</label>
                    <input type="text" class="form-control" id="bannerSubtitle" placeholder="Up to 50% off">
                </div>
                <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:24px;">
                    <button type="button" class="btn btn-outline" onclick="hideModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary" id="saveBtn" disabled>Save Banner</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        // Cloudinary setup
        const myWidget = cloudinary.createUploadWidget({
            cloudName: 'dmgyyvupn',
            uploadPreset: 'yldpwwqx',
            sources: ['local', 'url', 'camera'],
            multiple: false
        }, (error, result) => {
            if (!error && result && result.event === "success") {
                document.getElementById('imageUrl').value = result.info.secure_url;
                const preview = document.getElementById('previewImg');
                preview.src = result.info.secure_url;
                preview.style.display = 'block';
                document.getElementById('saveBtn').disabled = false;
            }
        });

        document.getElementById('uploadWidget').addEventListener('click', function() {
            myWidget.open();
        }, false);

        function showModal() { document.getElementById('addModal').classList.add('active'); }
        function hideModal() { 
            document.getElementById('addModal').classList.remove('active'); 
            document.getElementById('addForm').reset();
            document.getElementById('previewImg').style.display = 'none';
            document.getElementById('saveBtn').disabled = true;
        }

        function submitBanner(e) {
            e.preventDefault();
            const data = new URLSearchParams();
            data.append('action', 'create');
            data.append('image_url', document.getElementById('imageUrl').value);
            data.append('title', document.getElementById('bannerTitle').value);
            data.append('subtitle', document.getElementById('bannerSubtitle').value);
            
            fetch('banners.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: data
            }).then(() => location.reload());
        }

        function toggleActive(id) {
            fetch('banners.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=toggle_active&id=${id}`
            }).then(() => location.reload());
        }

        function deleteBanner(id) {
            if(!confirm("Are you sure you want to delete this banner?")) return;
            fetch('banners.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=delete&id=${id}`
            }).then(() => location.reload());
        }

        // Drag and drop reordering
        const list = document.getElementById('banner-list');
        let draggedItem = null;

        list.addEventListener('dragstart', e => {
            draggedItem = e.target.closest('.banner-card');
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/html', draggedItem.innerHTML);
            setTimeout(() => draggedItem.style.opacity = '0.5', 0);
        });

        list.addEventListener('dragover', e => {
            e.preventDefault();
            const target = e.target.closest('.banner-card');
            if (target && target !== draggedItem) {
                const rect = target.getBoundingClientRect();
                const next = (e.clientY - rect.top) / (rect.bottom - rect.top) > .5;
                list.insertBefore(draggedItem, next && target.nextSibling || target);
            }
        });

        list.addEventListener('dragend', e => {
            draggedItem.style.opacity = '1';
            const ids = Array.from(list.querySelectorAll('.banner-card')).map(c => c.dataset.id);
            if(ids.length > 0) {
                fetch('banners.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                    body: `action=reorder&ids=${JSON.stringify(ids)}`
                });
            }
        });
    </script>
</body>
</html>
