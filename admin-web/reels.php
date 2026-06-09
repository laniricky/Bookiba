<?php
require 'db.php';

// ── Handle AJAX actions ────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';

    if ($action === 'add') {
        // Ensure reels table exists
        $pdo->exec("CREATE TABLE IF NOT EXISTS reels (
            id VARCHAR(36) PRIMARY KEY,
            title VARCHAR(200) NOT NULL,
            video_url VARCHAR(500) NOT NULL,
            thumbnail_url VARCHAR(500),
            book_id VARCHAR(36),
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )");
        $id = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff));
        $stmt = $pdo->prepare("INSERT INTO reels (id, title, video_url, thumbnail_url, book_id, is_active, created_at) VALUES (?, ?, ?, ?, ?, TRUE, ?)");
        $stmt->execute([$id, $_POST['title'], $_POST['video_url'], $_POST['thumbnail_url'] ?? null, $_POST['book_id'] ?: null, date('Y-m-d H:i:s')]);
        echo json_encode(['ok' => true, 'id' => $id]);

    } elseif ($action === 'toggle') {
        $stmt = $pdo->prepare("UPDATE reels SET is_active = NOT is_active WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);

    } elseif ($action === 'delete') {
        $stmt = $pdo->prepare("DELETE FROM reels WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);
    }
    exit;
}

// ── Ensure table exists ────────────────────────────────────────────────────
$pdo->exec("CREATE TABLE IF NOT EXISTS reels (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    video_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    book_id VARCHAR(36),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

$reels = $pdo->query("SELECT r.*, b.title as book_title FROM reels r LEFT JOIN books b ON r.book_id = b.id ORDER BY r.created_at DESC")->fetchAll(PDO::FETCH_ASSOC);
$books = $pdo->query("SELECT id, title FROM books ORDER BY title ASC")->fetchAll(PDO::FETCH_ASSOC);
$total_reels = count($reels);
$active_reels = count(array_filter($reels, fn($r) => $r['is_active']));
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reels | Bookiba Admin</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .reels-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
        .reel-card { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); overflow: hidden; position: relative; transition: transform 0.2s, box-shadow 0.2s; }
        .reel-card:hover { transform: translateY(-4px); box-shadow: 0 16px 40px rgba(0,0,0,0.1); }
        .reel-thumb { width: 100%; aspect-ratio: 9/16; object-fit: cover; background: #111; display: block; position: relative; }
        .reel-thumb-placeholder { width: 100%; aspect-ratio: 9/16; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); display: flex; align-items: center; justify-content: center; }
        .reel-play-icon { width: 48px; height: 48px; background: rgba(255,255,255,0.15); border-radius: 50%; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); }
        .reel-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 50%); pointer-events: none; }
        .reel-body { padding: 14px; }
        .reel-title { font-weight: 700; font-size: 14px; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .reel-book { font-size: 12px; color: var(--text-muted); margin-bottom: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .reel-footer { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
        .reel-inactive { opacity: 0.5; }
        .reel-actions { display: flex; gap: 6px; }
        .reel-action-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); padding: 4px; border-radius: 4px; transition: all 0.15s; }
        .reel-action-btn:hover { background: var(--bg-cream); color: var(--text-main); }
        .reel-action-btn.del:hover { background: #FFEBEE; color: #C62828; }
        .badge-active { background: #E8F5E9; color: #2E7D32; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 10px; text-transform: uppercase; }
        .badge-inactive { background: #EEEEEE; color: #757575; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 10px; text-transform: uppercase; }
        .upload-section { border: 2px dashed var(--border-color); border-radius: var(--radius-lg); padding: 28px; text-align: center; cursor: pointer; transition: border-color 0.2s, background 0.2s; background: #FAFAFA; }
        .upload-section:hover { border-color: #365134; background: #F0F5F0; }
        .upload-section.has-file { border-color: #2E7D32; background: #F1F8E9; }

        /* Slide-over */
        .slide-over { position: fixed; right: -520px; top: 0; width: 500px; height: 100vh; background: white; box-shadow: -8px 0 40px rgba(0,0,0,0.12); z-index: 1000; transition: right 0.3s cubic-bezier(0.4,0,0.2,1); display: flex; flex-direction: column; }
        .slide-over.open { right: 0; }
        .slide-over-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 999; display: none; }
        .slide-over-overlay.show { display: block; }
        .slide-over-header { padding: 24px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
        .slide-over-body { padding: 24px; overflow-y: auto; flex: 1; }
        .slide-over-footer { padding: 20px 24px; border-top: 1px solid var(--border-color); display: flex; gap: 12px; justify-content: flex-end; }
        .form-group { margin-bottom: 20px; }
        .form-group label { display: block; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.6px; color: var(--text-muted); margin-bottom: 6px; }
        .form-input { width: 100%; padding: 10px 14px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 14px; font-family: inherit; outline: none; box-sizing: border-box; }
        .form-input:focus { border-color: var(--accent-green); box-shadow: 0 0 0 3px rgba(54,81,52,0.1); }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <div class="slide-over-overlay" id="overlay" onclick="closeSlideOver()"></div>
    <div class="slide-over" id="slideOver">
        <div class="slide-over-header">
            <div style="font-size:18px; font-weight:800;">Upload New Reel</div>
            <button onclick="closeSlideOver()" style="background:none; border:none; cursor:pointer; color:var(--text-muted);">
                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
        </div>
        <div class="slide-over-body">
            <div class="form-group">
                <label>Reel Title *</label>
                <input type="text" id="f_title" class="form-input" placeholder="e.g. Top 5 Books for Entrepreneurs">
            </div>

            <div class="form-group">
                <label>Video</label>
                <div id="video-upload-area" class="upload-section" onclick="openVideoWidget()">
                    <video id="video-preview" style="display:none; width:100%; max-height:200px; border-radius:8px; margin-bottom:12px;" controls></video>
                    <div id="video-placeholder">
                        <svg width="40" height="40" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="color:var(--text-muted); margin:0 auto 10px; display:block;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.723v6.554a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
                        <p style="font-size:14px; font-weight:600; color:var(--text-main); margin:0;">Click to upload video</p>
                        <p style="font-size:12px; color:var(--text-muted); margin:6px 0 0;">MP4, MOV up to 100MB · 9:16 vertical ratio recommended</p>
                    </div>
                </div>
                <input type="hidden" id="f_video_url">
            </div>

            <div class="form-group">
                <label>Thumbnail (Auto-generated or Custom)</label>
                <div id="thumb-upload-area" class="upload-section" onclick="openThumbWidget()" style="padding: 16px;">
                    <img id="thumb-preview" src="" style="display:none; width:60px; height:90px; object-fit:cover; border-radius:6px; margin:0 auto 10px;">
                    <div id="thumb-placeholder">
                        <svg width="28" height="28" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="color:var(--text-muted); margin:0 auto 6px; display:block;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                        <p style="font-size:12px; color:var(--text-muted); margin:0;">Click to upload a custom thumbnail (optional)</p>
                    </div>
                </div>
                <input type="hidden" id="f_thumbnail_url">
            </div>

            <div class="form-group">
                <label>Link to Book (Optional)</label>
                <select id="f_book_id" class="form-input">
                    <option value="">— No linked book —</option>
                    <?php foreach($books as $b): ?>
                    <option value="<?= $b['id'] ?>"><?= htmlspecialchars($b['title']) ?></option>
                    <?php endforeach; ?>
                </select>
            </div>
        </div>
        <div class="slide-over-footer">
            <button class="btn btn-outline" onclick="closeSlideOver()">Cancel</button>
            <button class="btn btn-primary" onclick="saveReel()" id="saveBtn">Upload Reel</button>
        </div>
    </div>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:24px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Reels</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= $total_reels ?> reels · <?= $active_reels ?> active</p>
            </div>
            <button class="btn btn-primary" onclick="openSlideOver()" style="display:flex; align-items:center; gap:8px;">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"/></svg>
                Upload Reel
            </button>
        </div>

        <?php if (empty($reels)): ?>
        <div class="card" style="text-align:center; padding:60px 40px;">
            <div style="width:64px; height:64px; background:var(--bg-cream); border-radius:50%; margin:0 auto 16px; display:flex; align-items:center; justify-content:center;">
                <svg width="28" height="28" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="color:var(--text-muted);"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.723v6.554a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
            </div>
            <h3 style="font-size:18px; font-weight:700; margin-bottom:8px;">No reels yet</h3>
            <p style="color:var(--text-muted); font-size:14px; margin-bottom:20px;">Upload short video reels to showcase books and drive sales.</p>
            <button class="btn btn-primary" onclick="openSlideOver()">Upload your first reel</button>
        </div>
        <?php else: ?>
        <div class="reels-grid">
            <?php foreach($reels as $r): ?>
            <div class="reel-card <?= $r['is_active'] ? '' : 'reel-inactive' ?>" id="reel-<?= $r['id'] ?>">
                <div style="position:relative;">
                    <?php if ($r['thumbnail_url']): ?>
                    <img src="<?= htmlspecialchars($r['thumbnail_url']) ?>" class="reel-thumb">
                    <?php else: ?>
                    <div class="reel-thumb-placeholder">
                        <div class="reel-play-icon">
                            <svg width="20" height="20" fill="white" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                        </div>
                    </div>
                    <?php endif; ?>
                    <div class="reel-overlay"></div>
                    <div style="position:absolute; top:10px; right:10px;">
                        <span class="<?= $r['is_active'] ? 'badge-active' : 'badge-inactive' ?>">
                            <?= $r['is_active'] ? 'Live' : 'Hidden' ?>
                        </span>
                    </div>
                </div>
                <div class="reel-body">
                    <div class="reel-title" title="<?= htmlspecialchars($r['title']) ?>"><?= htmlspecialchars($r['title']) ?></div>
                    <div class="reel-book"><?= $r['book_title'] ? '📖 ' . htmlspecialchars($r['book_title']) : 'No linked book' ?></div>
                    <div class="reel-footer">
                        <a href="<?= htmlspecialchars($r['video_url']) ?>" target="_blank" style="font-size:11px; color:var(--text-muted); text-decoration:none; display:flex; align-items:center; gap:4px;">
                            <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/></svg>
                            Preview
                        </a>
                        <div class="reel-actions">
                            <button class="reel-action-btn" title="<?= $r['is_active'] ? 'Hide' : 'Show' ?>" onclick="toggleReel('<?= $r['id'] ?>', this)">
                                <?php if ($r['is_active']): ?>
                                <svg width="15" height="15" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/></svg>
                                <?php else: ?>
                                <svg width="15" height="15" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                                <?php endif; ?>
                            </button>
                            <button class="reel-action-btn del" title="Delete" onclick="deleteReel('<?= $r['id'] ?>')">
                                <svg width="15" height="15" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
        <?php endif; ?>
    </main>

    <script src="https://upload-widget.cloudinary.com/global/all.js" type="text/javascript"></script>
    <script>
        const CLOUD = 'dmgyyvupn';
        const PRESET = 'yldpwwqx';

        // ── Video Widget ──────────────────────────────────────────────────────
        let videoWidget = cloudinary.createUploadWidget({
            cloudName: CLOUD, uploadPreset: PRESET,
            sources: ['local', 'url'],
            resourceType: 'video',
            maxFileSize: 100000000,
            clientAllowedFormats: ['mp4', 'mov', 'avi', 'webm'],
        }, (error, result) => {
            if (!error && result && result.event === 'success') {
                const info = result.info;
                document.getElementById('f_video_url').value = info.secure_url;
                const vid = document.getElementById('video-preview');
                vid.src = info.secure_url;
                vid.style.display = 'block';
                document.getElementById('video-placeholder').style.display = 'none';
                document.getElementById('video-upload-area').classList.add('has-file');
                // Auto-set thumbnail from Cloudinary video thumbnail
                const thumbUrl = `https://res.cloudinary.com/${CLOUD}/video/upload/so_0,w_400,h_600,c_fill/${info.public_id}.jpg`;
                if (!document.getElementById('f_thumbnail_url').value) {
                    document.getElementById('f_thumbnail_url').value = thumbUrl;
                    document.getElementById('thumb-preview').src = thumbUrl;
                    document.getElementById('thumb-preview').style.display = 'block';
                    document.getElementById('thumb-placeholder').style.display = 'none';
                    document.getElementById('thumb-upload-area').classList.add('has-file');
                }
            }
        });

        // ── Thumbnail Widget ──────────────────────────────────────────────────
        let thumbWidget = cloudinary.createUploadWidget({
            cloudName: CLOUD, uploadPreset: PRESET,
            sources: ['local', 'url', 'camera'],
            resourceType: 'image',
            cropping: true, croppingAspectRatio: 0.67,
        }, (error, result) => {
            if (!error && result && result.event === 'success') {
                const url = result.info.secure_url;
                document.getElementById('f_thumbnail_url').value = url;
                document.getElementById('thumb-preview').src = url;
                document.getElementById('thumb-preview').style.display = 'block';
                document.getElementById('thumb-placeholder').style.display = 'none';
                document.getElementById('thumb-upload-area').classList.add('has-file');
            }
        });

        function openVideoWidget() { videoWidget.open(); }
        function openThumbWidget() { thumbWidget.open(); }

        function openSlideOver() {
            document.getElementById('slideOver').classList.add('open');
            document.getElementById('overlay').classList.add('show');
        }
        function closeSlideOver() {
            document.getElementById('slideOver').classList.remove('open');
            document.getElementById('overlay').classList.remove('show');
            // Reset form
            document.getElementById('f_title').value = '';
            document.getElementById('f_video_url').value = '';
            document.getElementById('f_thumbnail_url').value = '';
            document.getElementById('f_book_id').value = '';
            document.getElementById('video-preview').style.display = 'none';
            document.getElementById('video-placeholder').style.display = 'block';
            document.getElementById('thumb-preview').style.display = 'none';
            document.getElementById('thumb-placeholder').style.display = 'block';
            document.getElementById('video-upload-area').classList.remove('has-file');
            document.getElementById('thumb-upload-area').classList.remove('has-file');
        }

        function saveReel() {
            const title = document.getElementById('f_title').value.trim();
            const videoUrl = document.getElementById('f_video_url').value;
            if (!title) { alert('Please enter a title.'); return; }
            if (!videoUrl) { alert('Please upload a video.'); return; }
            document.getElementById('saveBtn').textContent = 'Uploading...';
            fetch('reels.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: new URLSearchParams({
                    action: 'add',
                    title: title,
                    video_url: videoUrl,
                    thumbnail_url: document.getElementById('f_thumbnail_url').value,
                    book_id: document.getElementById('f_book_id').value,
                })
            }).then(r => r.json()).then(d => { if (d.ok) location.reload(); });
        }

        function toggleReel(id, btn) {
            fetch('reels.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=toggle&id=${id}`
            }).then(() => location.reload());
        }

        function deleteReel(id) {
            if (!confirm('Permanently delete this reel?')) return;
            fetch('reels.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=delete&id=${id}`
            }).then(r => r.json()).then(d => {
                if (d.ok) document.getElementById(`reel-${id}`)?.remove();
            });
        }
    </script>
</body>
</html>
