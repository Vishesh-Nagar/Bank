import React, { useEffect, useRef, useState } from "react";
import type { NotificationDto } from "../../types";

type Toast = NotificationDto & { id: number; exiting: boolean };

type Props = {
    notifications: NotificationDto[];
    onDismiss: (index: number) => void;
};

/* Per-type styling maps */
const toastBg: Record<string, string> = {
    PAYMENT_RECEIVED: "bg-gradient-to-br from-emerald-500/15 to-[rgba(6,95,70,0.15)]",
    PAYMENT_COMPLETED: "bg-gradient-to-br from-blue-500/15 to-[rgba(29,78,216,0.15)]",
    PAYMENT_FAILED:   "bg-gradient-to-br from-red-500/15 to-[rgba(153,27,27,0.15)]",
    PAYMENT_QUEUED:   "bg-gradient-to-br from-amber-500/15 to-[rgba(120,53,15,0.15)]",
    BALANCE_CHANGED:  "bg-gradient-to-br from-purple-500/15 to-[rgba(76,29,149,0.15)]",
};

const barColor: Record<string, string> = {
    PAYMENT_RECEIVED: "bg-emerald-500",
    PAYMENT_COMPLETED:"bg-blue-500",
    PAYMENT_FAILED:   "bg-red-500",
    PAYMENT_QUEUED:   "bg-amber-500",
    BALANCE_CHANGED:  "bg-purple-500",
};

/**
 * NotificationToast
 *
 * Watches the `notifications` array for NEW items by comparing it against
 * a `lastLength` ref. Every item that was not previously shown gets its own
 * toast entry. This correctly handles batch additions (e.g., when two
 * notifications are appended between renders).
 */
const NotificationToast: React.FC<Props> = ({ notifications, onDismiss }) => {
    const [toasts, setToasts] = useState<Toast[]>([]);
    const counterRef = useRef(0);
    // Tracks how many items from `notifications` we have already rendered.
    const lastLengthRef = useRef(0);

    useEffect(() => {
        const prevLength = lastLengthRef.current;
        const currentLength = notifications.length;

        if (currentLength <= prevLength) {
            // Array shrank (items dismissed) — nothing new to show.
            lastLengthRef.current = currentLength;
            return;
        }

        // Slice out the genuinely new notifications
        const newItems = notifications.slice(prevLength, currentLength);
        lastLengthRef.current = currentLength;

        const timers: ReturnType<typeof setTimeout>[] = [];

        newItems.forEach((notif) => {
            const id = counterRef.current++;
            const newToast: Toast = { ...notif, id, exiting: false };

            setToasts((prev) => [...prev, newToast]);

            // Auto-dismiss after 5 seconds
            const timer = setTimeout(() => {
                dismiss(id);
            }, 5000);
            timers.push(timer);
        });

        return () => timers.forEach(clearTimeout);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [notifications.length]);

    const dismiss = (toastId: number) => {
        setToasts((prev) =>
            prev.map((t) => (t.id === toastId ? { ...t, exiting: true } : t))
        );
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== toastId));
            // Note: we intentionally do NOT call onDismiss here for auto-dismiss
            // because we manage toast visibility independently of the notifications array.
        }, 350);
    };

    const dismissByButton = (toastId: number, notifIndex: number) => {
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
        if (type === "PAYMENT_QUEUED") return "⏳";
        if (type === "BALANCE_CHANGED") return "💰";
        return "❌";
    };

    const getLabel = (type: NotificationDto["type"]) => {
        if (type === "PAYMENT_RECEIVED") return "Received";
        if (type === "PAYMENT_COMPLETED") return "Sent";
        if (type === "PAYMENT_QUEUED") return "Queued";
        if (type === "BALANCE_CHANGED") return "Balance";
        return "Failed";
    };

    if (toasts.length === 0) return null;

    return (
        <div
            className="fixed bottom-7 right-7 flex flex-col gap-3 z-[9999] max-w-[380px] pointer-events-none max-sm:bottom-4 max-sm:right-4 max-sm:left-4 max-sm:max-w-none"
            aria-live="polite"
        >
            {toasts.map((toast, idx) => (
                <div
                    key={toast.id}
                    className={[
                        "relative overflow-hidden flex items-start gap-3.5 px-[18px] py-4 rounded-[14px]",
                        "border border-white/10 backdrop-blur-[12px]",
                        "shadow-[0_8px_32px_rgba(0,0,0,0.5),0_2px_8px_rgba(0,0,0,0.3)]",
                        "pointer-events-auto",
                        toastBg[toast.type] ?? "bg-white/10",
                        toast.exiting ? "animate-toast-out" : "animate-toast-in",
                    ].join(" ")}
                >
                    {/* Coloured left accent bar */}
                    <div
                        className={`absolute left-0 top-0 bottom-0 w-1 rounded-l-[14px] ${barColor[toast.type] ?? "bg-white"}`}
                    />

                    {/* Icon */}
                    <div className="text-xl shrink-0 mt-px">{getIcon(toast.type)}</div>

                    {/* Body */}
                    <div className="flex-1 flex flex-col gap-1 min-w-0">
                        <span className="text-[11px] font-bold uppercase tracking-[0.8px] text-slate-400">
                            {getLabel(toast.type)}
                        </span>
                        <span className="text-sm font-medium text-slate-200 leading-snug break-words">
                            {toast.message}
                        </span>
                    </div>

                    {/* Close button */}
                    <button
                        className="bg-transparent border-none text-slate-500 text-xl leading-none cursor-pointer p-0 shrink-0 self-start transition-colors duration-200 hover:text-slate-300"
                        onClick={() => dismissByButton(toast.id, idx)}
                        aria-label="Dismiss notification"
                    >
                        ×
                    </button>

                    {/* Timer progress bar — drains in 5 s then the auto-dismiss fires */}
                    <div
                        className={`absolute bottom-0 left-0 h-[3px] w-full rounded-b-[14px] origin-left ${
                            barColor[toast.type] ?? "bg-white"
                        } ${toast.exiting ? "" : "animate-timer-drain"}`}
                        style={toast.exiting ? { animationPlayState: "paused" } : undefined}
                    />
                </div>
            ))}
        </div>
    );
};

export default NotificationToast;
