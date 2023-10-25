import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';
import { viteSingleFile } from "vite-plugin-singlefile"

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    preact(),
    viteSingleFile({
      removeViteModuleLoader: true,
    })
  ],
  build: {
    outDir: 'template'
  }
});
