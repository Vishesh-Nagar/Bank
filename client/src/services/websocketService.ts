import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { NotificationDto } from "../types";

type NotificationHandler = (notification: NotificationDto) => void;

/** How long (ms) a message fingerprint is kept in the dedup set. */
const DEDUP_TTL_MS = 8_000;

/**
 * Build a stable fingerprint for a server notification.
 * We use type + the payment ID (if present) + message text as the key.
 * This is deterministic: the same STOMP frame always produces the same key.
 */
function fingerprint(n: NotificationDto): string {
    const paymentId = n.payment?.paymentId ?? "";
    return `${n.type}|${paymentId}|${n.message}`;
}

class WebSocketService {
    private client: Client | null = null;
    private subscriptions: Map<string, StompSubscription> = new Map();
    private pendingAccountIds: Set<number> = new Set();
    private connected = false;
    private onNotificationCallback: NotificationHandler | null = null;

    /**
     * Dedup set: fingerprint -> expiry timestamp.
     * A notification whose fingerprint is already in this map is dropped silently.
     */
    private seenMessages: Map<string, number> = new Map();

    // ─── Public API ────────────────────────────────────────────────────────────

    connect(token: string, onNotification: NotificationHandler): void {
        // Always update the callback so the latest React closure is used.
        this.onNotificationCallback = onNotification;

        // Already have a client — reuse it. Only update pending subscriptions.
        if (this.client) {
            if (this.connected) {
                this.pendingAccountIds.forEach((id) => this._subscribe(id));
                this.pendingAccountIds.clear();
            }
            return;
        }

        const backendUrl = import.meta.env.VITE_BACKEND_URL || "";
        const wsBase = backendUrl.replace(/\/api(\/v1)?\/?$/, "");

        this.client = new Client({
            webSocketFactory: () => new SockJS(`${wsBase}/ws?token=${token}`),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 5000,
            onConnect: () => {
                this.connected = true;
                this.pendingAccountIds.forEach((id) => this._subscribe(id));
                this.pendingAccountIds.clear();
            },
            onDisconnect: () => {
                this.connected = false;
            },
            onStompError: (frame) => {
                console.error("STOMP error:", frame.headers["message"]);
            },
        });

        this.client.activate();
    }

    subscribeToAccount(accountId: number): void {
        if (!this.connected || !this.client) {
            this.pendingAccountIds.add(accountId);
            return;
        }
        this._subscribe(accountId);
    }

    /**
     * Called on React unmount. Does NOT tear down the socket —
     * just clears the callback so stale closures don't fire.
     */
    disconnect(): void {
        this.onNotificationCallback = null;
    }

    /**
     * Hard shutdown — only called on explicit user logout.
     */
    forceDisconnect(): void {
        this.subscriptions.forEach((sub) => sub.unsubscribe());
        this.subscriptions.clear();
        this.pendingAccountIds.clear();
        this.seenMessages.clear();

        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
        this.connected = false;
        this.onNotificationCallback = null;
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private _subscribe(accountId: number): void {
        const topic = `/topic/account/${accountId}`;
        if (this.subscriptions.has(topic)) return;

        const sub = this.client!.subscribe(topic, (message) => {
            try {
                const notification: NotificationDto = JSON.parse(message.body);
                this._dispatch(notification);
            } catch (e) {
                console.error("Failed to parse notification:", e);
            }
        });
        this.subscriptions.set(topic, sub);
    }

    /**
     * Central dispatch gate.
     * 1. Prune expired entries from the dedup map.
     * 2. If this fingerprint was seen recently → drop it.
     * 3. Otherwise record it and forward to the React callback.
     */
    private _dispatch(notification: NotificationDto): void {
        const now = Date.now();

        // Prune stale entries (keeps the map small)
        this.seenMessages.forEach((expiry, key) => {
            if (now > expiry) this.seenMessages.delete(key);
        });

        const key = fingerprint(notification);
        if (this.seenMessages.has(key)) {
            // Duplicate — silently drop
            console.debug("[WS] Dropped duplicate notification:", key);
            return;
        }

        this.seenMessages.set(key, now + DEDUP_TTL_MS);

        if (this.onNotificationCallback) {
            this.onNotificationCallback(notification);
        }
    }
}

// Export a true module-level singleton
export const websocketService = new WebSocketService();
