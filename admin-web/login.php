<?php
require 'db.php';
session_start();

// If already logged in, skip to dashboard
if (!empty($_SESSION['admin_token'])) {
    $stmt = $pdo->prepare("SELECT token FROM admin_sessions WHERE token = ? AND expires_at > ?");
    $stmt->execute([$_SESSION['admin_token'], time()]);
    if ($stmt->fetch()) {
        header('Location: index.php'); exit;
    }
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    // Credentials from environment variables — never hardcode in source
    // Set ADMIN_USER and ADMIN_PASS in your web server or .env
    $validUser = getenv('ADMIN_USER') ?: 'admin';
    $validHash = getenv('ADMIN_PASS_HASH') ?: password_hash('changeme', PASSWORD_BCRYPT);

    if ($username === $validUser && password_verify($password, $validHash)) {
        $token = bin2hex(random_bytes(32));
        $pdo->prepare("INSERT INTO admin_sessions (token, expires_at) VALUES (?, ?)")
            ->execute([$token, time() + 14400]); // 4 hours

        $_SESSION['admin_token'] = $token;
        header('Location: index.php'); exit;
    } else {
        $error = 'Invalid username or password.';
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login | Bookiba</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        :root {
            --dark-brown: #1A1512;
            --warm-brown: #8B7355;
            --cream: #F5F0E8;
            --soft-white: #FEFCF9;
            --border: #E8DFD0;
        }
        body {
            font-family: 'Inter', -apple-system, sans-serif;
            background: var(--dark-brown);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-card {
            background: var(--soft-white);
            border-radius: 16px;
            padding: 48px 40px;
            width: 100%;
            max-width: 400px;
            box-shadow: 0 24px 64px rgba(0,0,0,0.4);
        }
        .logo {
            text-align: center;
            margin-bottom: 36px;
        }
        .logo-text { font-size: 26px; font-weight: 800; color: var(--dark-brown); letter-spacing: -0.5px; }
        .logo-sub { font-size: 12px; color: var(--warm-brown); font-weight: 500; letter-spacing: 2px; text-transform: uppercase; margin-top: 4px; }
        .form-group { margin-bottom: 18px; }
        label { display: block; font-size: 12px; font-weight: 700; color: var(--warm-brown); text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 6px; }
        input[type="text"], input[type="password"] {
            width: 100%; padding: 12px 14px;
            border: 1.5px solid var(--border);
            border-radius: 8px;
            font-size: 14px; font-family: inherit;
            background: var(--cream);
            color: var(--dark-brown);
            transition: border-color 0.15s, background 0.15s;
            outline: none;
        }
        input:focus { border-color: var(--warm-brown); background: var(--soft-white); }
        .btn-login {
            width: 100%; padding: 14px;
            background: var(--dark-brown);
            color: var(--cream);
            border: none; border-radius: 8px;
            font-size: 15px; font-weight: 700;
            font-family: inherit; cursor: pointer;
            margin-top: 8px;
            transition: background 0.15s;
        }
        .btn-login:hover { background: var(--warm-brown); }
        .error-msg {
            background: #FFF0F0; border: 1px solid #FFCDD2;
            color: #B71C1C; border-radius: 8px;
            padding: 12px 14px; font-size: 13px;
            margin-bottom: 18px;
            display: flex; align-items: center; gap: 8px;
        }
    </style>
</head>
<body>
    <div class="login-card">
        <div class="logo">
            <div class="logo-text">Bookiba</div>
            <div class="logo-sub">Admin Portal</div>
        </div>

        <?php if ($error): ?>
        <div class="error-msg">
            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            </svg>
            <?= htmlspecialchars($error) ?>
        </div>
        <?php endif; ?>

        <form method="POST" action="login.php">
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" autocomplete="username" required>
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" autocomplete="current-password" required>
            </div>
            <button type="submit" class="btn-login">Sign in to Admin</button>
        </form>
    </div>
</body>
</html>
