/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
    define: {
        global: "window",
    },
    plugins: [react(), tailwindcss()],
    server: {
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, "/api"),
            },
        },
    },
    test: {
        globals: true,
        environment: "jsdom",
        setupFiles: "./src/setupTests.ts",
    },
});
