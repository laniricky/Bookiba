const CACHE_NAME = 'bookiba-admin-v1';
const ASSETS = [
    './',
    './index.php',
    './style.css',
    './app.js',
    './manifest.json',
    './icon.svg'
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
        .then(cache => {
            // We do a non-blocking cache add so it doesn't fail if some files don't exist
            return Promise.all(
                ASSETS.map(url => {
                    return fetch(url).then(response => {
                        if (!response.ok) throw new Error('Request failed');
                        return cache.put(url, response);
                    }).catch(error => {
                        console.log('Failed to cache ' + url);
                    });
                })
            );
        })
    );
});

self.addEventListener('fetch', event => {
    // Only cache GET requests
    if (event.request.method !== 'GET') return;
    
    event.respondWith(
        fetch(event.request).catch(() => caches.match(event.request))
    );
});
