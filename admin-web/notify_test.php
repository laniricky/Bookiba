<?php
require 'db.php';
require 'includes/auth_gate.php';

// ── Test 1: Call the Ktor webhook directly and show the raw response ───────────
$testOrderId   = $_GET['order_id'] ?? '';
$testStatus    = $_GET['status']   ?? 'Processing';

$rawResponse   = null;
$httpCode      = null;
$curlError     = null;
$webhookResult = null;

if ($testOrderId) {
    $ktorUrl = getenv('KTOR_WEBHOOK_URL') ?: 'https://bookiba-backend.onrender.com/api/v1/internal/notify-order';
    $secret  = getenv('INTERNAL_WEBHOOK_SECRET') ?: 'my-internal-secret';
    $payload = json_encode(['orderId' => $testOrderId, 'status' => $testStatus]);

    $ch = curl_init($ktorUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $secret
    ]);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);  // Long timeout for debugging
    curl_setopt($ch, CURLOPT_VERBOSE, false);

    $rawResponse = curl_exec($ch);
    $httpCode    = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError   = curl_error($ch);
    curl_close($ch);
    $webhookResult = json_decode($rawResponse, true);
}

// ── Test 2: Check if FCM tokens exist for any user ──────────────────────────
$tokenRows = [];
try {
    $stmt = $pdo->query("SELECT user_id, fcm_token, updated_at FROM user_tokens ORDER BY updated_at DESC LIMIT 20");
    $tokenRows = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (Exception $e) {
    $tokenRows = [['error' => $e->getMessage()]];
}

// ── Test 3: Get some real order IDs to use ──────────────────────────────────
$orders = [];
try {
    $stmt   = $pdo->query("SELECT id, user_id, status FROM orders ORDER BY created_at DESC LIMIT 10");
    $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (Exception $e) {}

$ktorUrl = getenv('KTOR_WEBHOOK_URL') ?: 'https://bookiba-backend.onrender.com/api/v1/internal/notify-order';
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Notification Diagnostics – Bookiba</title>
<style>
  body { font-family: monospace; background: #0f0f0f; color: #e0e0e0; padding: 2rem; }
  h1 { color: #f5a623; }
  h2 { color: #7dd3fc; margin-top: 2rem; border-bottom: 1px solid #333; padding-bottom: .4rem; }
  .ok   { color: #4ade80; }
  .fail { color: #f87171; }
  .warn { color: #fbbf24; }
  pre  { background: #1a1a1a; padding: 1rem; border-radius: 6px; overflow-x: auto; }
  table { border-collapse: collapse; width: 100%; }
  th, td { border: 1px solid #333; padding: .4rem .8rem; text-align: left; }
  th { background: #1e293b; }
  form { background: #1a1a1a; padding: 1rem; border-radius: 8px; }
  input, select, button { padding: .4rem .8rem; margin: .2rem; font-family: monospace; background: #0f172a; color: #e2e8f0; border: 1px solid #475569; border-radius: 4px; }
  button { background: #f5a623; color: #000; font-weight: bold; cursor: pointer; }
</style>
</head>
<body>
<h1>🔔 Notification Diagnostics</h1>

<h2>Env Config</h2>
<pre>KTOR_WEBHOOK_URL = <?= htmlspecialchars($ktorUrl) ?>
</pre>

<!-- ── Webhook Test Form ─────────────────────────────────────────────── -->
<h2>Step 1 – Test Webhook Call</h2>
<form method="GET">
  <input type="hidden" name="page" value="notify_test">
  Order ID: <input type="text" name="order_id" value="<?= htmlspecialchars($testOrderId) ?>" size="40" placeholder="paste a real order UUID">
  Status:
  <select name="status">
    <?php foreach (['Processing','Shipped','Delivered','Cancelled'] as $s): ?>
      <option value="<?= $s ?>" <?= $testStatus === $s ? 'selected' : '' ?>><?= $s ?></option>
    <?php endforeach; ?>
  </select>
  <button type="submit">Send Test Notification</button>
</form>

<?php if ($testOrderId): ?>
<h3>Result</h3>
<?php if ($curlError): ?>
  <p class="fail">❌ CURL ERROR: <?= htmlspecialchars($curlError) ?></p>
<?php else: ?>
  <p class="<?= $httpCode === 200 ? 'ok' : 'fail' ?>">
    HTTP <?= $httpCode ?>
    <?php if ($httpCode === 200): ?> ✅ OK<?php else: ?> ❌ FAILED<?php endif; ?>
  </p>
  <pre><?= htmlspecialchars(json_encode($webhookResult, JSON_PRETTY_PRINT)) ?></pre>
  <?php if (isset($webhookResult['sent']) && $webhookResult['sent'] > 0): ?>
    <p class="ok">✅ FCM message sent to <?= $webhookResult['sent'] ?> device(s)</p>
  <?php elseif (isset($webhookResult['reason'])): ?>
    <p class="warn">⚠️ Skipped: <?= htmlspecialchars($webhookResult['reason']) ?></p>
    <p>👉 <strong>The user has no FCM token stored.</strong> The app must be installed, opened while logged in, and connected to the internet for a token to be registered.</p>
  <?php elseif (isset($webhookResult['error']) && $webhookResult['error'] === 'Order not found'): ?>
    <p class="fail">❌ Order not found in Ktor's database. The order ID may only exist in the PHP/SQLite DB.</p>
  <?php endif; ?>
<?php endif; ?>
<?php endif; ?>

<!-- ── FCM Token Table ──────────────────────────────────────────────── -->
<h2>Step 2 – FCM Tokens in DB (user_tokens)</h2>
<?php if (empty($tokenRows)): ?>
  <p class="fail">❌ No FCM tokens found in user_tokens table. The app has not registered any tokens yet.</p>
  <p>Possible reasons:
    <ul>
      <li>User hasn't opened the app while logged in</li>
      <li>The <code>POST /api/v1/fcm-token</code> endpoint is failing silently</li>
      <li>Notification permission not granted by the user</li>
    </ul>
  </p>
<?php else: ?>
  <p class="ok">✅ Found <?= count($tokenRows) ?> token(s)</p>
  <table>
    <tr><th>user_id</th><th>fcm_token (first 40 chars)</th><th>updated_at</th></tr>
    <?php foreach ($tokenRows as $row): ?>
    <tr>
      <td><?= htmlspecialchars($row['user_id'] ?? $row['error'] ?? '') ?></td>
      <td><?= htmlspecialchars(isset($row['fcm_token']) ? substr($row['fcm_token'], 0, 40).'…' : '') ?></td>
      <td><?= htmlspecialchars($row['updated_at'] ?? '') ?></td>
    </tr>
    <?php endforeach; ?>
  </table>
<?php endif; ?>

<!-- ── Recent Orders ───────────────────────────────────────────────── -->
<h2>Step 3 – Recent Orders (for quick copy-paste)</h2>
<table>
  <tr><th>order_id</th><th>user_id</th><th>status</th><th>Test</th></tr>
  <?php foreach ($orders as $o): ?>
  <tr>
    <td><?= htmlspecialchars($o['id']) ?></td>
    <td><?= htmlspecialchars($o['user_id']) ?></td>
    <td><?= htmlspecialchars($o['status']) ?></td>
    <td><a href="?order_id=<?= urlencode($o['id']) ?>&status=Processing" style="color:#f5a623">Test →</a></td>
  </tr>
  <?php endforeach; ?>
</table>

<hr style="margin-top:3rem;border-color:#333">
<p style="color:#555">This page is admin-only and not linked from the nav.</p>
</body>
</html>
