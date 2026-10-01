import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Giao diện KHÔNG có tiến trình riêng: bản dựng ra tệp tĩnh, máy chủ ứng dụng phục vụ chúng
// (kiến trúc Mục 1). Vì vậy đích dựng trỏ thẳng vào thư mục tài nguyên tĩnh của máy chủ.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../app/src/main/resources/static',
    emptyOutDir: true,
    sourcemap: false,
  },
  server: {
    port: 5173,
    // Lúc phát triển thì chạy hai tiến trình cho nhanh; bản dựng thật vẫn là một.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
