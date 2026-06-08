import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { NotificationDto } from "../types";

type NotificationHandler = (notification: NotificationDto) => void;

class WebSocketService {
    private client: Client | null = null;
    private subscription: StompSubscription | null = null;
    private connected = false;

    connect(token: string, onNotification: NotificationHandler): void {
        if (this.connected) return;

        const backendUrl = import.meta.env.VITE_BACKEND_URL || "";
        // Derive WebSocket base URL by stripping /api
        const wsBase = backendUrl.replace(/\/api\/?$/, "");

        this.client = new Client({
            webSocketFactory: () => new SockJS(`${wsBase}/ws`),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 5000,
            onConnect: () => {
                this.connected = true;
                // Subscribe to the user-specific notification queue
                this.subscription = this.client!.subscribe(
                    "/user/queue/notifications",
                    (message) => {
                        try {
                            const notification: NotificationDto = JSON.parse(
                                message.body
                            );
                            onNotification(notification);
                        } catch (e) {
                            console.error("Failed to parse notification:", e);
                        }
                    }
                );
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

    disconnect(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }
        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
        this.connected = false;
    }
}

// Export a singleton instance
export const websocketService = new WebSocketService();
