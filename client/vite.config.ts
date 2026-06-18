import { defineConfig } from "vitest/config";
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
                target: "http://127.0.0.1:8080",
                changeOrigin: true,
            },
            "/ws": {
                target: "http://127.0.0.1:8080",
                ws: true,
            },
        },
    },
    test: {
        globals: true,
        environment: "jsdom",
        setupFiles: "./src/setupTests.ts",
    },
});
