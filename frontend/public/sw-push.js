/**
 * Service Worker — Canal PUSH Web Push (VAPID)
 * Fichier servi statiquement depuis /sw-push.js (dossier public/).
 *
 * Ce Service Worker est DISTINCT du mécanisme IN_APP (WebSocket/bell icon) —
 * les deux canaux coexistent indépendamment.
 *
 * Vérification préalable : aucun autre Service Worker n'est présent dans public/.
 */

// ── Événement push : réception d'une notification depuis le backend ───────────

self.addEventListener('push', (event) => {
  let titre = 'Supervision cabine';
  let corps = 'Vous avez une nouvelle notification.';
  let urlCible = '/';
  let icone = '/images/logo.png';

  // Le payload est un JSON : { title, body, data: { url } }
  if (event.data) {
    try {
      const payload = event.data.json();
      titre = payload.title || titre;
      corps = payload.body || corps;
      urlCible = (payload.data && payload.data.url) ? payload.data.url : '/';
    } catch {
      // Payload non-JSON : on utilise le texte brut comme corps
      corps = event.data.text() || corps;
    }
  }

  const options = {
    body: corps,
    icon: icone,
    badge: icone,
    data: { url: urlCible },
    requireInteraction: false,
    vibrate: [150, 50, 150],
  };

  event.waitUntil(
    self.registration.showNotification(titre, options)
  );
});

// ── Événement notificationclick : clic sur la notification ───────────────────

self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const urlCible = (event.notification.data && event.notification.data.url)
    ? event.notification.data.url
    : '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // Si un onglet de l'app est déjà ouvert, le mettre en focus
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.focus();
          // Naviguer vers l'URL cible si possible
          if ('navigate' in client) {
            return client.navigate(urlCible);
          }
          return;
        }
      }
      // Aucun onglet ouvert : en ouvrir un nouveau
      if (clients.openWindow) {
        return clients.openWindow(urlCible);
      }
    })
  );
});
