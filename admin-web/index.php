<?php
require 'db.php';

// Form Handling (Add/Edit/Delete) - Same backend logic
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['_action'] ?? 'add';
    $id = $_POST['book_id'] ?? '';
    
    if ($action === 'add' && empty($id)) {
        $id = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x', mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000, mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff));
    }

    $title = $_POST['title'] ?? ''; $author = $_POST['author'] ?? ''; $description = $_POST['description'] ?? '';
    $price = (int)($_POST['price_ksh'] ?? 0); $condition = $_POST['condition'] ?? 'New'; $cover_url = $_POST['cover_url'] ?? '';
    $category = $_POST['category'] ?? ''; $edition = $_POST['edition'] ?? ''; $publisher = $_POST['publisher'] ?? '';
    $genre = $_POST['genre'] ?? ''; $tags = $_POST['tags'] ?? '';
    
    $is_rare = isset($_POST['is_rare']) ? 1 : 0; $is_featured = isset($_POST['is_featured']) ? 1 : 0; $is_staff_pick = isset($_POST['is_staff_pick']) ? 1 : 0;
    $seller_id = '00000000-0000-0000-0000-000000000000';

    if ($action === 'add' || $action === 'edit') {
        if ($action === 'add') {
            $stmt = $pdo->prepare("INSERT INTO books (id, title, author, description, price_ksh, condition, cover_url, category, edition, publisher, genre, tags, is_rare, is_featured, is_staff_pick, seller_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([$id, $title, $author, $description, $price, $condition, $cover_url, $category, $edition, $publisher, $genre, $tags, $is_rare, $is_featured, $is_staff_pick, $seller_id, date('Y-m-d H:i:s')]);
        } else {
            $stmt = $pdo->prepare("UPDATE books SET title=?, author=?, description=?, price_ksh=?, condition=?, cover_url=?, category=?, edition=?, publisher=?, genre=?, tags=?, is_rare=?, is_featured=?, is_staff_pick=? WHERE id=?");
            $stmt->execute([$title, $author, $description, $price, $condition, $cover_url, $category, $edition, $publisher, $genre, $tags, $is_rare, $is_featured, $is_staff_pick, $id]);
        }
        header("Location: index.php?" . ($action === 'add' ? 'added=1' : 'updated=1')); exit;
    }
    if ($action === 'delete') {
        $stmt = $pdo->prepare("DELETE FROM books WHERE id = ?"); $stmt->execute([$id]);
        header("Location: index.php?deleted=1"); exit;
    }
}

// Fetch Books
$search = $_GET['q'] ?? '';
$where = []; $params = [];
if ($search) { $where[] = "(title LIKE ? OR author LIKE ?)"; $params[] = "%$search%"; $params[] = "%$search%"; }
$where_sql = $where ? "WHERE " . implode(' AND ', $where) : "";
$stmt = $pdo->prepare("SELECT * FROM books $where_sql ORDER BY created_at DESC");
$stmt->execute($params);
$books = $stmt->fetchAll(PDO::FETCH_ASSOC);

// Stats
$total_books = $pdo->query("SELECT COUNT(*) FROM books")->fetchColumn();
$featured_count = $pdo->query("SELECT COUNT(*) FROM books WHERE is_featured = 1")->fetchColumn();
$rare_count = $pdo->query("SELECT COUNT(*) FROM books WHERE is_rare = 1")->fetchColumn();
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bookiba Dashboard</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>

    <!-- STICKY SIDEBAR -->
    <div class="sidebar-container">
        <aside class="sidebar">
            <div class="sidebar-logo">
                <span style="font-size:28px">📚</span> Bookiba
            </div>
            <nav>
                <a href="index.php" class="active"><span class="icon">🏠</span> Home Feed</a>
                <a href="orders.php"><span class="icon">🔔</span> Activity</a>
                <a href="analytics.php"><span class="icon">📈</span> Insights</a>
            </nav>
            <div class="user-profile-mini">
                <div class="user-avatar">A</div>
                <div>
                    <div style="font-weight:700; font-size:14px; color:var(--dark-brown)">Admin</div>
                    <div style="font-size:12px; color:var(--warm-brown)">@bookiba_hq</div>
                </div>
            </div>
        </aside>
    </div>

    <!-- MAIN FEED -->
    <main class="main-content">
        
        <!-- SEARCH -->
        <form class="search-bar" method="GET">
            <span style="font-size:20px;">🔍</span>
            <input type="search" name="q" placeholder="Search your inventory..." value="<?= htmlspecialchars($search) ?>">
        </form>

        <!-- STORIES (Stats) -->
        <div class="stories-header">
            <div class="stories-track">
                <div class="story-circle">
                    <div class="story-ring"><div class="story-inner"><?= $total_books ?></div></div>
                    <div class="story-label">Total Books</div>
                </div>
                <div class="story-circle">
                    <div class="story-ring"><div class="story-inner" style="color: #2A5A8B;"><?= $featured_count ?></div></div>
                    <div class="story-label">Featured</div>
                </div>
                <div class="story-circle">
                    <div class="story-ring"><div class="story-inner" style="color: #8B5E2A;"><?= $rare_count ?></div></div>
                    <div class="story-label">Rare</div>
                </div>
                <!-- Create Story Button -->
                <div class="story-circle" style="cursor:pointer;" onclick="openDrawer('add')">
                    <div class="story-ring" style="background: var(--border);">
                        <div class="story-inner" style="color: var(--warm-brown);">+</div>
                    </div>
                    <div class="story-label">Add New</div>
                </div>
            </div>
        </div>

        <div class="page-title">
            Your Library 
            <span style="font-size: 14px; font-weight: 600; color: var(--warm-brown); background: var(--border); padding: 4px 12px; border-radius: 20px;">Latest</span>
        </div>

        <!-- BOOKS GRID -->
        <div class="book-grid">
            <?php foreach ($books as $b): 
                $json = htmlspecialchars(json_encode($b));
            ?>
            <div class="book-card" onclick="openDrawer('edit', <?= $json ?>)">
                <?php if ($b['is_rare']): ?><div class="badge-float">Rare</div><?php endif; ?>
                
                <?php if ($b['cover_url']): ?>
                    <img src="<?= htmlspecialchars($b['cover_url']) ?>" class="book-cover">
                <?php else: ?>
                    <div class="no-cover">No Cover</div>
                <?php endif; ?>
                
                <div class="book-meta">
                    <div class="book-title" title="<?= htmlspecialchars($b['title']) ?>"><?= htmlspecialchars($b['title']) ?></div>
                    <div class="book-author"><?= htmlspecialchars($b['author']) ?></div>
                    <div class="book-price">Ksh <?= number_format($b['price_ksh']) ?></div>
                </div>
                
                <div class="book-actions" onclick="event.stopPropagation()">
                    <button class="action-btn <?= $b['is_featured'] ? 'active' : '' ?>" title="Feature" onclick="toggleField('<?= $b['id'] ?>', 'is_featured', this)">
                        <?= $b['is_featured'] ? '❤️' : '🤍' ?>
                    </button>
                    <button class="action-btn" title="Edit" onclick="openDrawer('edit', <?= $json ?>)">💬</button>
                    <form method="POST" style="display:inline;" onsubmit="return confirmDelete('<?= htmlspecialchars(addslashes($b['title'])) ?>')">
                        <input type="hidden" name="_action" value="delete">
                        <input type="hidden" name="book_id" value="<?= $b['id'] ?>">
                        <button type="submit" class="action-btn" title="Delete">🗑️</button>
                    </form>
                </div>
            </div>
            <?php endforeach; ?>
        </div>

    </main>

    <!-- FAB -->
    <button class="fab" onclick="openDrawer('add')" title="New Book">＋</button>

    <!-- SLIDE-OVER DRAWER -->
    <div id="drawer-overlay" class="drawer-overlay" onclick="closeDrawer()"></div>
    <div id="main-drawer" class="drawer">
        <div class="drawer-header">
            <div class="drawer-title" id="drawer-title">New Post (Book)</div>
            <button class="drawer-close" onclick="closeDrawer()">×</button>
        </div>
        <div class="drawer-body">
            <form id="book-form" method="POST">
                <input type="hidden" name="_action" id="form-action" value="add">
                <input type="hidden" name="book_id" id="edit-book-id" value="">

                <div class="form-group">
                    <label>Title</label>
                    <input type="text" name="title" required placeholder="What's the book called?">
                </div>
                <div class="form-group">
                    <label>Author</label>
                    <input type="text" name="author" required placeholder="Who wrote it?">
                </div>
                <div class="form-group">
                    <label>Description</label>
                    <textarea name="description" data-maxlen="1000" placeholder="Tell your followers about it..."></textarea>
                </div>
                <div class="form-group">
                    <label>Cover Image URL</label>
                    <input type="url" name="cover_url" placeholder="https://..." onchange="updateImagePreview(this.value)">
                    <img id="cover-preview" style="width: 100px; border-radius: 12px; margin-top: 10px; display: none;">
                </div>

                <div style="display:flex; gap: 16px;">
                    <div class="form-group" style="flex:1;">
                        <label>Price (Ksh)</label>
                        <input type="number" name="price_ksh" required>
                    </div>
                    <div class="form-group" style="flex:1;">
                        <label>Condition</label>
                        <select name="condition">
                            <option value="New">New</option>
                            <option value="Like New">Like New</option>
                            <option value="Good">Good</option>
                        </select>
                    </div>
                </div>

                <div style="display:flex; justify-content: space-between; align-items: center; padding: 12px 0; border-top: 1px solid var(--border);">
                    <div>
                        <div style="font-weight:700;">Feature this post</div>
                        <div style="font-size:12px; color:var(--warm-brown)">Show prominently to followers</div>
                    </div>
                    <label class="toggle"><input type="checkbox" name="is_featured"><div class="toggle-slider"></div></label>
                </div>
                <div style="display:flex; justify-content: space-between; align-items: center; padding: 12px 0; border-top: 1px solid var(--border);">
                    <div>
                        <div style="font-weight:700;">Mark as Rare</div>
                        <div style="font-size:12px; color:var(--warm-brown)">Add a special collector's badge</div>
                    </div>
                    <label class="toggle"><input type="checkbox" name="is_rare"><div class="toggle-slider"></div></label>
                </div>

            </form>
        </div>
        <div class="drawer-footer">
            <button type="button" class="btn btn-outline" onclick="closeDrawer()">Cancel</button>
            <button type="button" class="btn btn-primary" onclick="document.getElementById('book-form').submit()" style="margin-left: auto;">Publish</button>
        </div>
    </div>

    <!-- TOAST CONTAINER -->
    <div id="toast-container" style="position:fixed; bottom:24px; right:24px; z-index:9999; display:flex; flex-direction:column; gap:8px;"></div>

    <script>
        function openDrawer(mode, data) {
            document.getElementById('drawer-overlay').classList.add('show');
            document.getElementById('main-drawer').classList.add('open');
            document.body.style.overflow = 'hidden';
            const form = document.getElementById('book-form');
            if (mode === 'add') {
                document.getElementById('drawer-title').textContent = 'Create Post';
                form.reset(); document.getElementById('form-action').value = 'add';
                document.getElementById('edit-book-id').value = ''; updateImagePreview('');
            } else if (data) {
                document.getElementById('drawer-title').textContent = 'Edit Post';
                document.getElementById('form-action').value = 'edit';
                document.getElementById('edit-book-id').value = data.id;
                ['title','author','description','price_ksh','condition','cover_url'].forEach(k => { if(form.elements[k]) form.elements[k].value = data[k] ?? ''; });
                ['is_rare','is_featured'].forEach(k => { if(form.elements[k]) form.elements[k].checked = data[k] == 1; });
                updateImagePreview(data.cover_url);
            }
        }
        function closeDrawer() {
            document.getElementById('drawer-overlay').classList.remove('show');
            document.getElementById('main-drawer').classList.remove('open');
            document.body.style.overflow = '';
        }
        function updateImagePreview(url) {
            const img = document.getElementById('cover-preview');
            if(url) { img.src=url; img.style.display='block'; } else { img.style.display='none'; }
        }
        function toggleField(bookId, field, btn) {
            fetch('books_api.php', {
                method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `action=toggle&book_id=${bookId}&field=${field}&value=${btn.classList.contains('active') ? 0 : 1}`
            }).then(r=>r.json()).then(d=>{
                if(d.ok) { 
                    btn.classList.toggle('active'); 
                    if(field === 'is_featured') btn.textContent = btn.classList.contains('active') ? '❤️' : '🤍';
                }
            });
        }
        function confirmDelete(t) { return confirm("Delete "+t+"?"); }
    </script>
</body>
</html>
