
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backendTarget =
    env.VITE_DEV_API_PROXY || 'http://localhost:8080';

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
