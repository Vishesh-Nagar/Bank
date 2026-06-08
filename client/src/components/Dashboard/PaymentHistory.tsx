import React, { useEffect, useState } from "react";
import type { AccountDto, PaymentStatusDto } from "../../types";
import { getPaymentHistory } from "../../services/paymentService";
import "./PaymentHistory.css";

type Props = {
    account: AccountDto;
    currentUserId: number;
    onClose: () => void;
};

const statusBadge = (status: string) => {
    const map: Record<string, { cls: string; label: string }> = {
        COMPLETED: { cls: "ph-badge--completed", label: "Completed" },
        PENDING: { cls: "ph-badge--pending", label: "Pending" },
        FAILED: { cls: "ph-badge--failed", label: "Failed" },
    };
    return map[status] ?? { cls: "", label: status };
};

const formatDate = (iso: string | null) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleString();
};

const PaymentHistory: React.FC<Props> = ({ account, onClose }) => {
    const [payments, setPayments] = useState<PaymentStatusDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError("");
        getPaymentHistory(account.id)
            .then((data) => {
                if (!cancelled) setPayments(data);
            })
            .catch(() => {
                if (!cancelled) setError("Failed to load payment history.");
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => { cancelled = true; };
    }, [account.id]);

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal ph-modal" onClick={(e) => e.stopPropagation()}>
                <div className="ph-header">
                    <div>
                        <h2>📋 Payment History</h2>
                        <p className="ph-subtitle">
                            Account #{account.id} — {account.accountHolderName}
                        </p>
                    </div>
                    <button className="ph-close" onClick={onClose} aria-label="Close">×</button>
                </div>

                {loading && (
                    <div className="ph-state">
                        <div className="spinner" style={{ width: 32, height: 32 }} />
                        <p>Loading payments…</p>
                    </div>
                )}

                {!loading && error && (
                    <div className="error-message">
                        <span>⚠️ {error}</span>
                    </div>
                )}

                {!loading && !error && payments.length === 0 && (
                    <div className="ph-state">
                        <p style={{ fontSize: 32 }}>🏦</p>
                        <p>No payment history yet.</p>
                    </div>
                )}

                {!loading && !error && payments.length > 0 && (
                    <div className="ph-list">
                        {payments.map((p) => {
                            const isSender = p.sourceAccountId === account.id;
                            const badge = statusBadge(p.status);
                            return (
                                <div key={p.paymentId} className="ph-item">
                                    <div className="ph-item__direction">
                                        {isSender ? (
                                            <span className="ph-direction ph-direction--out" title="Sent">↑</span>
                                        ) : (
                                            <span className="ph-direction ph-direction--in" title="Received">↓</span>
                                        )}
                                    </div>
                                    <div className="ph-item__info">
                                        <span className="ph-item__counterparty">
                                            {isSender
                                                ? `To Account #${p.targetAccountId}`
                                                : `From Account #${p.sourceAccountId}`}
                                        </span>
                                        <span className="ph-item__date">
                                            {formatDate(p.submittedAt)}
                                        </span>
                                        {p.failureReason && (
                                            <span className="ph-item__reason">
                                                {p.failureReason}
                                            </span>
                                        )}
                                    </div>
                                    <div className="ph-item__right">
                                        <span className={`ph-item__amount ${isSender ? "ph-amount--out" : "ph-amount--in"}`}>
                                            {isSender ? "-" : "+"}${Number(p.amount).toFixed(2)}
                                        </span>
                                        <span className={`ph-badge ${badge.cls}`}>
                                            {badge.label}
                                        </span>
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
