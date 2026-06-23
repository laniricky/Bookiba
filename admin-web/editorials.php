<?php
require 'db.php';
require 'includes/auth_gate.php';

// ── Handle AJAX actions ────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_SERVER['HTTP_X_REQUESTED_WITH'])) {
    header('Content-Type: application/json');
    $action = $_POST['action'] ?? '';

    if ($action === 'add') {
        $id = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff));
            
        $expiresAt = !empty($_POST['expires_at']) ? date('Y-m-d H:i:s', strtotime($_POST['expires_at'])) : null;
        $queryTag = trim($_POST['query_tag']);
        
        // Insert Editorial
        $stmt = $pdo->prepare("INSERT INTO editorials (id, label, image_url, query_tag, sort_order, is_active, expires_at, created_at) VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)");
        $stmt->execute([$id, $_POST['label'], $_POST['image_url'], $queryTag, 0, $expiresAt, date('Y-m-d H:i:s')]);

        // Auto-tag selected books
        if (!empty($_POST['book_ids'])) {
            $bookIds = explode(',', $_POST['book_ids']);
            $updateStmt = $pdo->prepare("UPDATE books SET tags = CASE WHEN tags IS NULL OR tags = '' THEN ? ELSE tags || ',' || ? END WHERE id = ?");
            foreach ($bookIds as $bId) {
                if (trim($bId)) {
                    $updateStmt->execute([$queryTag, $queryTag, trim($bId)]);
                }
            }
        }

        echo json_encode(['ok' => true, 'id' => $id]);

    } elseif ($action === 'delete') {
        $stmt = $pdo->prepare("DELETE FROM editorials WHERE id = ?");
        $stmt->execute([$_POST['id']]);
        echo json_encode(['ok' => true]);
    }
    exit;
}

// ── Fetch data ────────────────────────────────────────────────────
$editorials = $pdo->query("SELECT * FROM editorials ORDER BY created_at DESC")->fetchAll(PDO::FETCH_ASSOC);
$books = $pdo->query("SELECT id, title, author FROM books ORDER BY title ASC")->fetchAll(PDO::FETCH_ASSOC);

$total_stories = count($editorials);
$active_stories = 0;
$now = new DateTime();
foreach ($editorials as $e) {
    if ($e['is_active'] && (!$e['expires_at'] || new DateTime($e['expires_at']) > $now)) {
        $active_stories++;
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Story Campaigns | Bookiba Admin</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .grid-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
        .card-story { background: var(--card-white); border: 1px solid var(--border-color); border-radius: var(--radius-lg); overflow: hidden; position: relative; transition: transform 0.2s, box-shadow 0.2s; display: flex; align-items: center; padding: 16px; gap: 16px; }
        .card-story:hover { transform: translateY(-2px); box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
        .story-img { width: 70px; height: 70px; border-radius: 50%; object-fit: cover; background: #EEE; border: 2px solid var(--accent-green); flex-shrink: 0; }
        .story-info { flex-grow: 1; min-width: 0; }
        .story-title { font-weight: 700; font-size: 16px; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-main); }
        .story-tag { font-size: 12px; color: var(--accent-green); font-weight: 600; margin-bottom: 6px; }
        .story-meta { font-size: 11px; color: var(--text-muted); }
        .badge-active { background: #E8F5E9; color: #2E7D32; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 10px; text-transform: uppercase; }
        .badge-expired { background: #FFEBEE; color: #C62828; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 10px; text-transform: uppercase; }
        
        .upload-section { border: 2px dashed var(--border-color); border-radius: var(--radius-lg); padding: 20px; text-align: center; cursor: pointer; transition: border-color 0.2s, background 0.2s; background: #FAFAFA; }
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
        
        .book-list { max-height: 200px; overflow-y: auto; border: 1px solid var(--border-color); border-radius: 6px; padding: 10px; }
        .book-item { display: flex; align-items: center; gap: 10px; padding: 6px 0; border-bottom: 1px solid #EEE; }
        .book-item:last-child { border-bottom: none; }
    </style>
</head>
<body>
    <?php include 'includes/sidebar.php'; ?>

    <div class="slide-over-overlay" id="overlay" onclick="closeSlideOver()"></div>
    <div class="slide-over" id="slideOver">
        <div class="slide-over-header">
            <div style="font-size:18px; font-weight:800;">Create Story Campaign</div>
            <button onclick="closeSlideOver()" style="background:none; border:none; cursor:pointer; color:var(--text-muted);">
                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
        </div>
        <div class="slide-over-body">
            <div class="form-group">
                <label>Story Label *</label>
                <input type="text" id="f_label" class="form-input" placeholder="e.g. Weekend Thrillers">
            </div>

            <div class="form-group">
                <label>Unique Hashtag *</label>
                <input type="text" id="f_tag" class="form-input" placeholder="e.g. #weekend_thrillers" value="#">
                <p style="font-size:11px; color:var(--text-muted); margin-top:4px;">Books selected below will be automatically tagged with this.</p>
            </div>

            <div class="form-group">
                <label>Circle Artwork (Image)</label>
                <div id="thumb-upload-area" class="upload-section" onclick="openThumbWidget()">
                    <img id="thumb-preview" src="" style="display:none; width:80px; height:80px; object-fit:cover; border-radius:50%; margin:0 auto 10px;">
                    <div id="thumb-placeholder">
                        <svg width="28" height="28" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="color:var(--text-muted); margin:0 auto 6px; display:block;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                        <p style="font-size:12px; color:var(--text-muted); margin:0;">Click to upload circle image</p>
                    </div>
                </div>
                <input type="hidden" id="f_image_url">
            </div>

            <div class="form-group">
                <label>Expires At (Optional)</label>
                <input type="datetime-local" id="f_expires" class="form-input">
                <p style="font-size:11px; color:var(--text-muted); margin-top:4px;">If left blank, the story will remain permanently active.</p>
            </div>

            <div class="form-group">
                <label>Select Books for Story</label>
                <div class="book-list">
                    <?php foreach($books as $b): ?>
                    <label class="book-item">
                        <input type="checkbox" value="<?= $b['id'] ?>" class="book-checkbox">
                        <span style="font-size: 13px;"><b><?= htmlspecialchars($b['title']) ?></b> <span style="color:#777;">by <?= htmlspecialchars($b['author']) ?></span></span>
                    </label>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>
        <div class="slide-over-footer">
            <button class="btn btn-outline" onclick="closeSlideOver()">Cancel</button>
            <button class="btn btn-primary" onclick="saveStory()" id="saveBtn">Publish Story</button>
        </div>
    </div>

    <main class="main-content" style="grid-column: 2 / -1;">
        <?php include 'includes/header.php'; ?>

        <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:24px;">
            <div>
                <h1 style="font-size:24px; font-weight:800; margin-bottom:4px;">Story Campaigns</h1>
                <p style="color:var(--text-muted); font-size:14px;"><?= $total_stories ?> total · <?= $active_stories ?> currently live</p>
            </div>
            <button class="btn btn-primary" onclick="openSlideOver()" style="display:flex; align-items:center; gap:8px;">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"/></svg>
                Create Story
            </button>
        </div>

        <?php if (empty($editorials)): ?>
        <div class="card" style="text-align:center; padding:60px 40px;">
            <div style="width:64px; height:64px; background:var(--bg-cream); border-radius:50%; margin:0 auto 16px; display:flex; align-items:center; justify-content:center;">
                <svg width="28" height="28" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="color:var(--text-muted);"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
            </div>
            <h3 style="font-size:18px; font-weight:700; margin-bottom:8px;">No stories yet</h3>
            <p style="color:var(--text-muted); font-size:14px; margin-bottom:20px;">Curate collections of books to show in the immersive Story Tray.</p>
            <button class="btn btn-primary" onclick="openSlideOver()">Create your first story</button>
        </div>
        <?php else: ?>
        <div class="grid-cards">
            <?php foreach($editorials as $e): 
                $isExpired = false;
                if ($e['expires_at']) {
                    $exp = new DateTime($e['expires_at']);
                    if ($exp < $now) $isExpired = true;
                }
            ?>
            <div class="card-story" id="story-<?= $e['id'] ?>">
                <?php if ($e['image_url']): ?>
                    <img src="<?= htmlspecialchars($e['image_url']) ?>" class="story-img">
                <?php else: ?>
                    <div class="story-img" style="display:flex; align-items:center; justify-content:center; font-size:24px; font-weight:800; color:var(--text-muted);">
                        <?= strtoupper(substr($e['label'], 0, 1)) ?>
                    </div>
                <?php endif; ?>

                <div class="story-info">
                    <div class="story-title" title="<?= htmlspecialchars($e['label']) ?>"><?= htmlspecialchars($e['label']) ?></div>
                    <div class="story-tag"><?= htmlspecialchars($e['query_tag']) ?></div>
                    <div class="story-meta">
                        <?php if ($isExpired): ?>
                            <span class="badge-expired">Expired</span>
                        <?php elseif ($e['expires_at']): ?>
                            <span class="badge-active">Live</span> · Ends <?= (new DateTime($e['expires_at']))->format('M j') ?>
                        <?php else: ?>
                            <span class="badge-active">Live</span> · Permanent
                        <?php endif; ?>
                    </div>
                </div>

                <button class="btn" style="padding:6px; background:none; border:none; color:#C62828; cursor:pointer;" onclick="deleteStory('<?= $e['id'] ?>')" title="Delete Story">
                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                </button>
            </div>
            <?php endforeach; ?>
        </div>
        <?php endif; ?>
    </main>

    <script src="https://upload-widget.cloudinary.com/global/all.js" type="text/javascript"></script>
    <script>
        const CLOUD = 'dmgyyvupn';
        const PRESET = 'yldpwwqx';

        let thumbWidget = cloudinary.createUploadWidget({
            cloudName: CLOUD, uploadPreset: PRESET,
            sources: ['local', 'url', 'camera'],
            resourceType: 'image',
            cropping: true, croppingAspectRatio: 1.0,
        }, (error, result) => {
            if (!error && result && result.event === 'success') {
                const url = result.info.secure_url;
                document.getElementById('f_image_url').value = url;
                document.getElementById('thumb-preview').src = url;
                document.getElementById('thumb-preview').style.display = 'block';
                document.getElementById('thumb-placeholder').style.display = 'none';
                document.getElementById('thumb-upload-area').classList.add('has-file');
            }
        });

        function openThumbWidget() { thumbWidget.open(); }

        function openSlideOver() {
            document.getElementById('slideOver').classList.add('open');
            document.getElementById('overlay').classList.add('show');
        }
        function closeSlideOver() {
            document.getElementById('slideOver').classList.remove('open');
            document.getElementById('overlay').classList.remove('show');
            document.getElementById('f_label').value = '';
            document.getElementById('f_tag').value = '#';
            document.getElementById('f_image_url').value = '';
            document.getElementById('f_expires').value = '';
            document.querySelectorAll('.book-checkbox').forEach(cb => cb.checked = false);
            document.getElementById('thumb-preview').style.display = 'none';
            document.getElementById('thumb-placeholder').style.display = 'block';
            document.getElementById('thumb-upload-area').classList.remove('has-file');
        }

        function saveStory() {
            const label = document.getElementById('f_label').value.trim();
            const tag = document.getElementById('f_tag').value.trim();
            const expires = document.getElementById('f_expires').value;
            const imageUrl = document.getElementById('f_image_url').value;

            if (!label) { alert('Please enter a label.'); return; }
            if (!tag || tag === '#') { alert('Please enter a valid unique hashtag.'); return; }

            const selectedBooks = Array.from(document.querySelectorAll('.book-checkbox:checked')).map(cb => cb.value);

            document.getElementById('saveBtn').textContent = 'Publishing...';
            
            fetch('editorials.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: new URLSearchParams({
                    action: 'add',
                    label: label,
                    query_tag: tag,
                    expires_at: expires,
                    image_url: imageUrl,
                    book_ids: selectedBooks.join(',')
                })
            }).then(r => r.json()).then(d => { if (d.ok) location.reload(); });
        }

        function deleteStory(id) {
            if (!confirm('Delete this story campaign?')) return;
            fetch('editorials.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
                body: `action=delete&id=${id}`
            }).then(r => r.json()).then(d => {
                if (d.ok) document.getElementById(`story-${id}`)?.remove();
            });
        }
    </script>
</body>
</html>
