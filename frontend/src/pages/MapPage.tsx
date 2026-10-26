import { useEffect, useRef, useState } from 'react';
import { destinationsApi } from '../api/client';
import type { Destination } from '../api/types';
import { t } from '../i18n';
import { getCountryCoords } from '../lib/countries';

// Leaflet is loaded dynamically so it doesn't bloat the main bundle
async function initMap(container: HTMLDivElement, destinations: Destination[]) {
  const L = await import('leaflet');
  await import('leaflet/dist/leaflet.css');

  // Fix default marker icons broken by bundlers
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  delete (L.Icon.Default.prototype as any)._getIconUrl;
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  });

  const map = L.map(container).setView([20, 0], 2);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 18,
  }).addTo(map);

  for (const dest of destinations) {
    const coords = getCountryCoords(dest.country);
    if (!coords) continue;
    const popup = `<strong>${dest.city}</strong><br>${dest.country}${dest.description ? `<br><em>${dest.description}</em>` : ''}`;
    L.marker([coords.lat, coords.lng]).addTo(map).bindPopup(popup);
  }

  return map;
}

export function MapPage() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [destinations, setDestinations] = useState<Destination[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    destinationsApi
      .list({ page: 0, size: 500 })
      .then((p) => setDestinations(p.content))
      .catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!destinations || !containerRef.current) return;
    let leafletMap: Awaited<ReturnType<typeof initMap>> | null = null;
    initMap(containerRef.current, destinations).then((m) => {
      leafletMap = m;
    });
    return () => {
      leafletMap?.remove();
    };
  }, [destinations]);

  return (
    <section className="page-section">
      <h1 className="page-title">{t('map.title')}</h1>
      {error && <p className="error-text">{error}</p>}
      {!destinations && !error && <p className="muted">{t('map.loading')}</p>}
      {destinations?.length === 0 && <p className="muted">{t('map.empty')}</p>}
      <div
        ref={containerRef}
        style={{ height: '520px', borderRadius: '10px', overflow: 'hidden', marginTop: '1rem' }}
      />
    </section>
  );
}
