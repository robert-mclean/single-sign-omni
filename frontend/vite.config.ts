import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // base: process.env.isPages ? '/single-sign-omni/' : '',
  base: '/single-sign-omni/',
});
