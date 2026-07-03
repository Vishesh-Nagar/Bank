import React, { useEffect, useState } from "react";
import type { AccountDto, PaymentStatusDto, ScheduledPaymentDto } from "../../types";
import { getPaymentHistory, disputePayment, getScheduledPayments, cancelScheduledPayment } from "../../services/paymentService";

type Props = {
    account: AccountDto;
    currentUserId: number;
    onClose: () => void;
};

const statusConfig: Record<string, { label: string; className: string }> = {
    COMPLETED: {
        label: "Completed",
        className: "bg-emerald-500/15 text-emerald-400 border border-emerald-500/25",
    },
    PENDING: {
        label: "Pending",
        className: "bg-amber-500/15 text-amber-400 border border-amber-500/25",
    },
    FAILED: {
        label: "Failed",
        className: "bg-red-500/15 text-red-400 border border-red-500/25",
    },
    SCHEDULED: {
        label: "Scheduled",
        className: "bg-blue-500/15 text-blue-400 border border-blue-500/25",
    },
};

const formatDate = (iso: string | null) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleString();
};

type HistoryItem = 
    | (PaymentStatusDto & { _type: 'payment' })
    | (ScheduledPaymentDto & { _type: 'scheduled' });

const PaymentHistory: React.FC<Props> = ({ account, onClose }) => {
    const [items, setItems] = useState<HistoryItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError("");
        
        Promise.all([
            getPaymentHistory(account.id).catch(() => []),
            getScheduledPayments(account.id).catch(() => [])
        ]).then(([historyData, scheduledData]) => {
            if (cancelled) return;
            const combined: HistoryItem[] = [
                ...(historyData || []).map((p: any) => ({ ...p, _type: 'payment' as const })),
                ...(scheduledData || []).map((s: any) => ({ ...s, _type: 'scheduled' as const }))
            ];
            
            combined.sort((a, b) => {
                const dateA = new Date(a._type === 'payment' ? a.submittedAt : a.nextExecutionTime).getTime();
                const dateB = new Date(b._type === 'payment' ? b.submittedAt : b.nextExecutionTime).getTime();
                return dateB - dateA;
            });
            
            setItems(combined);
            setLoading(false);
        }).catch(() => {
            if (!cancelled) {
                setError("Failed to load payment history.");
                setLoading(false);
            }
        });

        return () => { cancelled = true; };
    }, [account.id]);

    const handleCancelPayment = async (paymentId: string) => {
        try {
            setError("");
            const updated = await disputePayment(paymentId);
            setItems(prev => prev.map(item => (item._type === 'payment' && item.paymentId === paymentId) ? { ...updated, _type: 'payment' } : item));
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to cancel payment.");
        }
    };

    const handleCancelScheduled = async (id: number) => {
        if (!window.confirm("Are you sure you want to cancel this scheduled payment?")) return;
        try {
            setError("");
            await cancelScheduledPayment(id);
            setItems(prev => prev.filter(item => !(item._type === 'scheduled' && item.id === id)));
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to cancel scheduled payment.");
        }
    };

    return (
        /* Overlay */
        <div
            className="fixed inset-0 bg-black/85 backdrop-blur-[8px] flex justify-center items-center z-[1000] animate-modal-fade"
            onClick={onClose}
        >
            {/* Modal */}
            <div
                className="bg-gradient-to-br from-[#1e293b] to-[#0f172a] w-full max-w-[600px] max-h-[80vh] flex flex-col p-7 rounded-[20px] shadow-[0_20px_60px_rgba(0,0,0,0.6)] border border-white/10 animate-modal-slide sm:max-h-[90vh] sm:p-[20px_16px]"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex justify-between items-start mb-6 shrink-0">
                    <div>
                        <h2 className="m-0 mb-1 text-[22px] font-bold">📋 Payment History</h2>
                        <p className="m-0 text-[13px] text-slate-500">
                            Account #{account.id} — {account.accountHolderName}
                        </p>
                    </div>
                    <button
                        className="bg-transparent border-none text-slate-500 text-[28px] leading-none cursor-pointer px-1 transition-colors duration-200 hover:text-slate-200 shrink-0"
                        onClick={onClose}
                        aria-label="Close"
                    >
                        ×
                    </button>
                </div>

                {/* Loading */}
                {loading && (
                    <div className="flex flex-col items-center justify-center gap-3 py-12 text-slate-500 text-[15px]">
                        <div className="w-8 h-8 border-[3px] border-blue-500/15 border-t-blue-500 rounded-full animate-spin" />
                        <p>Loading payments…</p>
                    </div>
                )}

                {/* Error */}
                {!loading && error && (
                    <div className="bg-red-500/10 border border-red-500/30 text-red-300 px-5 py-4 rounded-xl mb-6 flex justify-between items-center animate-error-slide">
                        <span>⚠️ {error}</span>
                    </div>
                )}

                {/* Empty */}
                {!loading && !error && items.length === 0 && (
                    <div className="flex flex-col items-center justify-center gap-3 py-12 text-slate-500 text-[15px]">
                        <p className="text-[32px]">🏦</p>
                        <p>No payment history yet.</p>
                    </div>
                )}

                {/* Payment list */}
                {!loading && !error && items.length > 0 && (
                    <div className="overflow-y-auto flex flex-col gap-2.5 flex-1 pr-1 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-white/[0.04] [&::-webkit-scrollbar-track]:rounded [&::-webkit-scrollbar-thumb]:bg-white/10 [&::-webkit-scrollbar-thumb]:rounded">
                        {items.map((item) => {
                            if (item._type === 'scheduled') {
                                const isSender = item.sourceAccountId === account.id;
                                const badge = statusConfig['SCHEDULED'];
                                return (
                                    <div
                                        key={`sched-${item.id}`}
                                        className="flex items-center gap-3.5 px-4 py-3.5 rounded-xl bg-[rgba(15,23,42,0.5)] border border-white/[0.06] transition-colors duration-200 hover:border-white/[0.12]"
                                    >
                                        {/* Direction arrow */}
                                        <div
                                            className={`w-[34px] h-[34px] rounded-full flex items-center justify-center text-base font-bold shrink-0 ${
                                                isSender
                                                    ? "bg-red-500/15 text-red-400 border border-red-500/25"
                                                    : "bg-emerald-500/15 text-emerald-400 border border-emerald-500/25"
                                            }`}
                                            title={isSender ? "Sent" : "Received"}
                                        >
                                            {isSender ? "↑" : "↓"}
                                        </div>

                                        {/* Info */}
                                        <div className="flex-1 flex flex-col gap-[3px] min-w-0">
                                            <span className="text-sm font-semibold text-slate-200 whitespace-nowrap overflow-hidden text-ellipsis">
                                                {isSender
                                                    ? `To Account #${item.targetAccountId}`
                                                    : `From Account #${item.sourceAccountId}`}
                                            </span>
                                            <span className="text-xs text-slate-500">Next: {formatDate(item.nextExecutionTime)} ({item.recurrenceType})</span>
                                        </div>

                                        {/* Right: amount + badge */}
                                        <div className="flex flex-col items-end gap-1.5 shrink-0">
                                            <span className={`text-base font-bold ${isSender ? "text-red-400" : "text-emerald-400"}`}>
                                                {isSender ? "-" : "+"}${Number(item.amount).toFixed(2)}
                                            </span>
                                            <div className="flex gap-2 items-center">
                                                {item.status === 'ACTIVE' && isSender && (
                                                    <button
                                                        onClick={() => handleCancelScheduled(item.id!)}
                                                        className="bg-transparent text-red-400 border border-red-500/50 hover:bg-red-500/10 text-[11px] font-bold uppercase tracking-[0.6px] px-2.5 py-[3px] rounded-full cursor-pointer transition-colors"
                                                    >
                                                        Cancel
                                                    </button>
                                                )}
                                                <span className={`text-[11px] font-bold uppercase tracking-[0.6px] px-2.5 py-[3px] rounded-full ${badge.className}`}>
                                                    {badge.label}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                );
                            }

                            const p = item;
                            const isSender = p.sourceAccountId === account.id;
                            const badge = statusConfig[p.status] ?? { label: p.status, className: "" };

                            return (
                                <div
                                    key={`pay-${p.paymentId}`}
                                    className="flex items-center gap-3.5 px-4 py-3.5 rounded-xl bg-[rgba(15,23,42,0.5)] border border-white/[0.06] transition-colors duration-200 hover:border-white/[0.12]"
                                >
                                    {/* Direction arrow */}
                                    <div
                                        className={`w-[34px] h-[34px] rounded-full flex items-center justify-center text-base font-bold shrink-0 ${
                                            isSender
                                                ? "bg-red-500/15 text-red-400 border border-red-500/25"
                                                : "bg-emerald-500/15 text-emerald-400 border border-emerald-500/25"
                                        }`}
                                        title={isSender ? "Sent" : "Received"}
                                    >
                                        {isSender ? "↑" : "↓"}
                                    </div>

                                    {/* Info */}
                                    <div className="flex-1 flex flex-col gap-[3px] min-w-0">
                                        <span className="text-sm font-semibold text-slate-200 whitespace-nowrap overflow-hidden text-ellipsis">
                                            {isSender
                                                ? `To Account #${p.targetAccountId}`
                                                : `From Account #${p.sourceAccountId}`}
                                        </span>
                                        <span className="text-xs text-slate-500">{formatDate(p.submittedAt)}</span>
                                        {p.failureReason && (
                                            <span className="text-xs text-red-400 italic">{p.failureReason}</span>
                                        )}
                                    </div>

                                    {/* Right: amount + badge */}
                                    <div className="flex flex-col items-end gap-1.5 shrink-0">
                                        <span className={`text-base font-bold ${isSender ? "text-red-400" : "text-emerald-400"}`}>
                                            {isSender ? "-" : "+"}${Number(p.amount).toFixed(2)}
                                        </span>
                                        <div className="flex gap-2 items-center">
                                            {p.status === "PENDING" && isSender && (
                                                <button
                                                    onClick={() => handleCancelPayment(p.paymentId!)}
                                                    className="bg-transparent text-red-400 border border-red-500/50 hover:bg-red-500/10 text-[11px] font-bold uppercase tracking-[0.6px] px-2.5 py-[3px] rounded-full cursor-pointer transition-colors"
                                                >
                                                    Cancel
                                                </button>
                                            )}
                                            <span className={`text-[11px] font-bold uppercase tracking-[0.6px] px-2.5 py-[3px] rounded-full ${badge.className}`}>
                                                {badge.label}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

export default PaymentHistory;
