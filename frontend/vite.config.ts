import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// In development the browser talks to Vite, and Vite forwards /api to the Spring API,
// so no CORS setup is needed locally. Deployed builds use VITE_API_URL instead.
export default defineConfig({
  plugins: [react()],
  build: {
    // Flag images are many and some are large: keep every SVG a separate file the browser loads on demand,
    // instead of inlining the small ones into the stylesheet that blocks the first paint
    assetsInlineLimit: (file) => (file.endsWith('.svg') ? false : undefined),
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
});
