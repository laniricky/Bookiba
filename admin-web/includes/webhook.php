<?php

function notifyKtorWebhook($orderId, $status) {
    // Ktor backend URL on Render — override via env var for other environments.
    $ktorUrl = getenv('KTOR_WEBHOOK_URL') ?: 'https://bookiba-backend.onrender.com/api/v1/internal/notify-order';

    // The internal secret must match the value checked in FcmRoutes.kt
    $secret = getenv('INTERNAL_WEBHOOK_SECRET') ?: 'my-internal-secret';

    $payload = json_encode([
        'orderId' => $orderId,
        'status'  => $status
    ]);

    $ch = curl_init($ktorUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $secret
    ]);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);

    // Give enough time for Render cold-start (up to 8 s), but don't block the UI
    curl_setopt($ch, CURLOPT_TIMEOUT, 8);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($curlError) {
        error_log("[Bookiba] FCM webhook curl error: $curlError");
    } elseif ($httpCode !== 200) {
        error_log("[Bookiba] FCM webhook returned HTTP $httpCode: $response");
    }

    return $httpCode === 200;
}
