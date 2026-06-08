import React, { useEffect, useState } from "react";
import type { NotificationDto } from "../../types";
import "./NotificationToast.css";

type Toast = NotificationDto & { id: number; exiting: boolean };

type Props = {
    notifications: NotificationDto[];
    onDismiss: (index: number) => void;
};

const NotificationToast: React.FC<Props> = ({ notifications, onDismiss }) => {
    const [toasts, setToasts] = useState<Toast[]>([]);
    const [counter, setCounter] = useState(0);

    useEffect(() => {
        if (notifications.length === 0) return;
        const latest = notifications[notifications.length - 1];
        const id = counter;
        setCounter((c) => c + 1);

        const newToast: Toast = { ...latest, id, exiting: false };
        setToasts((prev) => [...prev, newToast]);

        // Auto-dismiss after 5 seconds
        const timer = setTimeout(() => {
            dismiss(id, notifications.length - 1);
        }, 5000);

        return () => clearTimeout(timer);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [notifications.length]);

    const dismiss = (toastId: number, notifIndex: number) => {
        setToasts((prev) =>
            prev.map((t) => (t.id === toastId ? { ...t, exiting: true } : t))
        );
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== toastId));
            onDismiss(notifIndex);
        }, 350);
    };

    const getIcon = (type: NotificationDto["type"]) => {
        if (type === "PAYMENT_RECEIVED") return "💚";
        if (type === "PAYMENT_COMPLETED") return "✅";
        return "❌";
    };

    const getLabel = (type: NotificationDto["type"]) => {
        if (type === "PAYMENT_RECEIVED") return "Received";
        if (type === "PAYMENT_COMPLETED") return "Sent";
        return "Failed";
    };

    if (toasts.length === 0) return null;

    return (
        <div className="toast-container" aria-live="polite">
            {toasts.map((toast, idx) => (
                <div
                    key={toast.id}
                    className={`toast toast--${toast.type.toLowerCase().replace("payment_", "")} ${toast.exiting ? "toast--exiting" : ""}`}
                >
                    <div className="toast__icon">{getIcon(toast.type)}</div>
                    <div className="toast__body">
                        <span className="toast__label">{getLabel(toast.type)}</span>
                        <span className="toast__message">{toast.message}</span>
                    </div>
                    <button
                        className="toast__close"
                        onClick={() => dismiss(toast.id, idx)}
                        aria-label="Dismiss notification"
                    >
                        ×
                    </button>
                </div>
            ))}
        </div>
    );
};

export default NotificationToast;
