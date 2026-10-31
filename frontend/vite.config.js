
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  // Supports multiple comma-separated backend URLs.
  // The first healthy backend gets priority.
  const backendCandidates = (
    env.VITE_DEV_API_PROXY || 'http://localhost:8080'
  )
    .split(',')
    .map((url) => url.trim().replace(/\/+$/, ''))
    .filter(Boolean);

  let backendTarget = null;

  // Find the first backend whose health endpoint responds successfully.
  for (const target of backendCandidates) {
    try {
      const response = await fetch(`${target}/api/health`, {
        method: 'GET',
        signal: AbortSignal.timeout(4000),
      });

      if (response.ok) {
        backendTarget = target;
        console.log(`[Vite] Using backend: ${backendTarget}`);
        break;
      }

      console.warn(
        `[Vite] Backend returned HTTP ${response.status}: ${target}`
      );
    } catch (error) {
      console.warn(
        `[Vite] Backend unavailable: ${target} - ${error.message}`
      );
    }
  }

  // Keep Vite running even when neither backend is available yet.
  if (!backendTarget) {
    backendTarget = backendCandidates[0] || 'http://localhost:8080';

    console.warn(
      '[Vite] No healthy backend found. Selected default:',
      backendTarget
    );
  }

  return {
    plugins: [react(), tailwindcss()],

    server: {
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
          secure: true,
        },
      },
    },

    test: {
      include: [
        'src/test/**/*.test.{js,jsx,ts,tsx}',
        'src/test/**/*.spec.{js,jsx,ts,tsx}',
      ],
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.js'],
      clearMocks: true,
    },
  };
});
