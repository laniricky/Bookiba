// =====================
// BOOKIBA ADMIN — APP.JS
// =====================

// ---- DRAWER ----
function openDrawer(mode, data) {
    document.getElementById('drawer-overlay').classList.add('show');
    document.getElementById('main-drawer').classList.add('open');
    document.body.style.overflow = 'hidden';

    const form = document.getElementById('book-form');
    if (!form) return;

    if (mode === 'add') {
        document.getElementById('drawer-title').textContent = 'Add New Book';
        form.reset();
        document.getElementById('form-action').value = 'add';
        document.getElementById('edit-book-id').value = '';
        updateImagePreview('');
        const saa = document.getElementById('save-add-another');
        if (saa) saa.style.display = 'inline-flex';
    } else if (mode === 'edit' && data) {
        document.getElementById('drawer-title').textContent = 'Edit Book';
        document.getElementById('form-action').value = 'edit';
        document.getElementById('edit-book-id').value = data.id;
        const saa = document.getElementById('save-add-another');
        if (saa) saa.style.display = 'none';

        const fields = ['title','author','description','price_ksh','condition','cover_url',
                        'category','edition','publisher','genre','tags'];
        fields.forEach(k => {
            const el = form.elements[k];
            if (el) el.value = data[k] ?? '';
        });

        ['is_rare','is_featured','is_staff_pick'].forEach(k => {
            const el = form.elements[k];
            if (el) el.checked = data[k] == 1 || data[k] === true;
        });

        updateImagePreview(data.cover_url || '');
    }
}

function closeDrawer() {
    document.getElementById('drawer-overlay').classList.remove('show');
    document.getElementById('main-drawer').classList.remove('open');
    document.body.style.overflow = '';
}

// ---- IMAGE PREVIEW ----
function updateImagePreview(url) {
    const img = document.getElementById('cover-preview');
    if (!img) return;
    if (url && (url.startsWith('http') || url.startsWith('/'))) {
        img.src = url;
        img.style.display = 'block';
        img.onerror = () => { img.style.display = 'none'; };
    } else {
        img.style.display = 'none';
        img.src = '';
    }
}

// ---- TOAST ----
function toast(message, type) {
    type = type || 'success';
    var icons = { success: '✓', error: '✕', info: 'ℹ' };
    var el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.innerHTML = '<span>' + (icons[type] || '') + '</span> ' + message;
    document.getElementById('toast-container').appendChild(el);
    setTimeout(function() {
        el.style.opacity = '0';
        el.style.transition = 'opacity 0.3s';
        setTimeout(function() { el.remove(); }, 300);
    }, 3200);
}

// ---- TOGGLE FIELDS VIA AJAX ----
function toggleField(bookId, field, checkbox) {
    fetch('books_api.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'action=toggle&book_id=' + encodeURIComponent(bookId) +
              '&field=' + encodeURIComponent(field) +
              '&value=' + (checkbox.checked ? 1 : 0)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.ok) toast(field.replace('is_','').replace('_',' ') + ' updated');
        else { toast('Update failed', 'error'); checkbox.checked = !checkbox.checked; }
    })
    .catch(function() { toast('Network error', 'error'); checkbox.checked = !checkbox.checked; });
}

// ---- DELETE CONFIRM ----
function confirmDelete(title) {
    return confirm('Delete "' + title + '"?\nThis cannot be undone.');
}

// ---- SAVE AND ADD ANOTHER ----
function saveAndAddAnother() {
    var form = document.getElementById('book-form');
    var h = document.createElement('input');
    h.type = 'hidden'; h.name = '_add_another'; h.value = '1';
    form.appendChild(h);
    form.submit();
}

// ---- KEYBOARD SHORTCUTS ----
document.addEventListener('keydown', function(e) {
    var tag = document.activeElement.tagName;
    var inInput = ['INPUT','TEXTAREA','SELECT'].indexOf(tag) > -1;

    if (e.key === 'n' && !inInput && document.querySelector('[data-shortcut="new-book"]')) {
        e.preventDefault();
        openDrawer('add');
    }
    if ((e.key === '/' || (e.key === 'k' && (e.metaKey || e.ctrlKey))) && !inInput) {
        e.preventDefault();
        var s = document.getElementById('global-search');
        if (s) s.focus();
    }
    if (e.key === 'Escape') closeDrawer();
});

// ---- CHAR COUNTER ----
document.querySelectorAll('textarea[data-maxlen]').forEach(function(ta) {
    var max = parseInt(ta.dataset.maxlen);
    var counter = document.createElement('div');
    counter.className = 'char-count';
    ta.parentNode.insertBefore(counter, ta.nextSibling);
    function update() { counter.textContent = ta.value.length + ' / ' + max; }
    ta.addEventListener('input', update);
    update();
});

// ---- SHOW TOAST FROM URL PARAMS ----
(function() {
    var p = new URLSearchParams(window.location.search);
    if (p.has('added'))   toast('Book added successfully');
    if (p.has('updated')) toast('Book updated');
    if (p.has('deleted')) toast('Book deleted');
    if (p.has('added') || p.has('updated') || p.has('deleted')) {
        var url = new URL(window.location);
        ['added','updated','deleted'].forEach(function(k) { url.searchParams.delete(k); });
        history.replaceState({}, '', url);
    }
    if (p.get('open') === 'add') {
        requestAnimationFrame(function() { openDrawer('add'); });
    }
})();
