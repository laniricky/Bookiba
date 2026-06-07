<?php
// ── Admin Auth Gate ───────────────────────────────────────────────────────────
// Include this file at the TOP of every admin page (after require 'db.php').
// If the session is invalid, redirects to login.php.

session_start();

// Check session token validity
$valid = false;
if (!empty($_SESSION['admin_token'])) {
    $stmt = $pdo->prepare(
        "SELECT token FROM admin_sessions WHERE token = ? AND expires_at > ?"
    );
    $stmt->execute([$_SESSION['admin_token'], time()]);
    $valid = (bool)$stmt->fetch();
}

if (!$valid) {
    header('Location: login.php');
    exit;
}

// Extend session by 4 hours on each page load
$pdo->prepare("UPDATE admin_sessions SET expires_at = ? WHERE token = ?")
    ->execute([time() + 14400, $_SESSION['admin_token']]);
