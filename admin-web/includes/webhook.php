<?php

function notifyKtorWebhook($orderId, $status) {
    // Determine the Ktor webhook URL from an environment variable, or use a default.
    $ktorUrl = getenv('KTOR_WEBHOOK_URL') ?: 'http://localhost:8080/api/v1/internal/notify-order';
    
    // The internal secret hardcoded in our Ktor backend
    $secret = 'my-internal-secret';

    $payload = json_encode([
        'orderId' => $orderId,
        'status' => $status
    ]);

    $ch = curl_init($ktorUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $secret
    ]);

    // Fast timeout so the dashboard doesn't block if Ktor is down
    curl_setopt($ch, CURLOPT_TIMEOUT, 2);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    return $httpCode === 200;
}
