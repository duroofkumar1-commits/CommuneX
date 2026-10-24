
const CACHE_NAME = 'communex-shell-v2';
const SHELL_URLS = ['/', '/manifest.webmanifest', '/favicon.svg', '/icons.svg'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(SHELL_URLS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key !== CACHE_NAME)
            .map((key) => caches.delete(key))
        )
      )
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);

  if (
    request.method !== 'GET' ||
    url.origin !== self.location.origin ||
    url.pathname.startsWith('/api/')
  ) {
    return;
  }

  if (request.mode === 'navigate') {
    event.respondWith(
      (async () => {
        try {
          const response = await fetch(request);

          if (response.ok) {
            // Clone immediately, before asynchronous cache operations.
            const cacheCopy = response.clone();

            caches.open(CACHE_NAME)
              .then((cache) => cache.put('/', cacheCopy))
              .catch(() => {});
          }

          return response;
        } catch {
          return (
            await caches.match('/')
          ) || new Response('Page unavailable offline', {
            status: 503,
            headers: { 'Content-Type': 'text/plain; charset=utf-8' }
          });
        }
      })()
    );

    return;
  }

  if (/\.(?:js|css|svg|png|webp|woff2?)$/i.test(url.pathname)) {
    event.respondWith(
      (async () => {
        const cached = await caches.match(request);

        if (cached) {
          return cached;
        }

        const response = await fetch(request);

        if (response.ok) {
          // Clone before starting the asynchronous cache write.
          const cacheCopy = response.clone();

          caches.open(CACHE_NAME)
            .then((cache) => cache.put(request, cacheCopy))
            .catch(() => {});
        }

        return response;
      })()
    );
  }
});
