import React, { useState, useEffect } from "react";
import type { AccountDto, ScheduledPaymentDto, RecurrenceType } from "../../../types";
import { createScheduledPayment, getScheduledPayments, cancelScheduledPayment } from "../../../services/paymentService";

type Props = {
    visible: boolean;
    sourceAccount: AccountDto | null;
    onClose: () => void;
};

const inputClass =
    "w-full px-4 py-[14px] bg-[rgba(15,23,42,0.6)] border border-white/10 rounded-[10px] text-white text-[15px] transition-all duration-300 " +
    "focus:outline-none focus:border-blue-500 focus:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] focus:bg-[rgba(15,23,42,0.8)] " +
    "placeholder:text-slate-500 disabled:opacity-60";

const ScheduledPaymentModal: React.FC<Props> = ({
    visible,
    sourceAccount,
    onClose,
}) => {
    const [accountIdStr, setAccountIdStr] = useState("");
    const [amount, setAmount] = useState("");
    const [recurrenceType, setRecurrenceType] = useState<RecurrenceType>("DAILY");
    const [nextExecutionTime, setNextExecutionTime] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    
    const [scheduledPayments, setScheduledPayments] = useState<ScheduledPaymentDto[]>([]);
    const [loadingList, setLoadingList] = useState(false);

    useEffect(() => {
        if (visible && sourceAccount) {
            fetchScheduledPayments();
            const now = new Date();
            now.setMinutes(now.getMinutes() + 5);
            setNextExecutionTime(now.toISOString().slice(0, 16));
        }
    }, [visible, sourceAccount]);

    const fetchScheduledPayments = async () => {
        if (!sourceAccount) return;
        setLoadingList(true);
        try {
            const data = await getScheduledPayments(sourceAccount.id);
            setScheduledPayments(data);
        } catch (err: any) {
            console.error("Failed to load scheduled payments", err);
        } finally {
            setLoadingList(false);
        }
    };

    if (!visible || !sourceAccount) return null;

    const reset = () => {
        setAccountIdStr("");
        setAmount("");
        setError("");
        setSubmitting(false);
    };

    const handleClose = () => {
        reset();
        onClose();
    };

    const handleSubmit = async () => {
        const parsedAmount = parseFloat(amount);
        const parsedTarget = parseInt(accountIdStr, 10);

        if (!accountIdStr || isNaN(parsedTarget) || parsedTarget <= 0) {
            setError("Please enter a valid target account ID.");
            return;
        }
        if (parsedTarget === sourceAccount.id) {
            setError("You cannot schedule a payment to yourself.");
            return;
        }
        if (isNaN(parsedAmount) || parsedAmount <= 0) {
            setError("Please enter a valid positive amount.");
            return;
        }
        if (!nextExecutionTime) {
            setError("Please specify when to execute the first payment.");
            return;
        }

        setSubmitting(true);
        setError("");

        try {
            await createScheduledPayment({
                sourceAccountId: sourceAccount.id,
                targetAccountId: parsedTarget,
                amount: parsedAmount,
                recurrenceType,
                nextExecutionTime: new Date(nextExecutionTime).toISOString(),
            });
            reset();
            fetchScheduledPayments();
        } catch (err: any) {
            setError(
                err.response?.data?.message ||
                    "Failed to schedule payment. Please try again."
            );
            setSubmitting(false);
        }
    };

    const handleCancelScheduled = async (id: number) => {
        if (!window.confirm("Are you sure you want to cancel this scheduled payment?")) return;
        try {
            await cancelScheduledPayment(id);
            fetchScheduledPayments();
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to cancel scheduled payment.");
        }
    };

    return (
        /* Overlay */
        <div
            className="fixed inset-0 bg-black/85 backdrop-blur-[8px] flex justify-center items-center z-[1000] animate-modal-fade p-4"
            onClick={handleClose}
        >
            {/* Modal */}
            <div
                className="bg-gradient-to-br from-[#1e293b] to-[#0f172a] p-8 rounded-[20px] w-full max-w-[700px] shadow-[0_20px_60px_rgba(0,0,0,0.6)] border border-white/10 animate-modal-slide max-h-[90vh] overflow-y-auto"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex justify-between items-center mb-6">
                    <h2 className="m-0 text-2xl font-bold">⏱️ Scheduled Payments</h2>
                    <button
                        className="bg-transparent border-none text-slate-500 text-[28px] leading-none cursor-pointer px-1 transition-colors duration-200 hover:text-slate-200"
                        onClick={handleClose}
                    >
                        ×
                    </button>
                </div>

                <div className="flex flex-col md:flex-row gap-8">
                    {/* Left: Create Form */}
                    <div className="flex-1">
                        <h3 className="text-lg font-semibold mb-4 text-white">Create New</h3>
                        
                        <div className="mb-4">
                            <label className="block mb-1 text-sm text-slate-300">Target Account ID</label>
                            <input
                                type="text"
                                inputMode="numeric"
                                value={accountIdStr}
                                onChange={(e) => {
                                    const val = e.target.value;
                                    if (val === "" || /^\d+$/.test(val)) setAccountIdStr(val);
                                }}
                                className={inputClass}
                                placeholder="e.g. 7"
                            />
                        </div>

                        <div className="mb-4">
                            <label className="block mb-1 text-sm text-slate-300">Amount ($)</label>
                            <input
                                type="text"
                                inputMode="decimal"
                                value={amount}
                                onChange={(e) => {
                                    const val = e.target.value;
                                    if (val === "" || /^\d*\.?\d{0,4}$/.test(val)) setAmount(val);
                                }}
                                className={inputClass}
                                placeholder="0.00"
                            />
                        </div>

                        <div className="mb-4">
                            <label className="block mb-1 text-sm text-slate-300">Recurrence</label>
                            <select
                                value={recurrenceType}
                                onChange={(e) => setRecurrenceType(e.target.value as RecurrenceType)}
                                className={inputClass}
                            >
                                <option value="DAILY">Daily</option>
                                <option value="WEEKLY">Weekly</option>
                                <option value="MONTHLY">Monthly</option>
                            </select>
                        </div>

                        <div className="mb-4">
                            <label className="block mb-1 text-sm text-slate-300">First Execution Time</label>
                            <input
                                type="datetime-local"
                                value={nextExecutionTime}
                                onChange={(e) => setNextExecutionTime(e.target.value)}
                                className={inputClass}
                            />
                        </div>

                        {error && (
                            <div className="bg-red-500/10 border border-red-500/30 text-red-300 px-4 py-2 rounded mt-2 mb-4">
                                {error}
                            </div>
                        )}

                        <button
                            className="btn btn-primary w-full"
                            onClick={handleSubmit}
                            disabled={submitting || !accountIdStr || !amount}
                        >
                            {submitting ? "⏳ Scheduling..." : "Schedule Payment"}
                        </button>
                    </div>

                    {/* Right: Existing Scheduled Payments */}
                    <div className="flex-1 border-t md:border-t-0 md:border-l border-white/10 pt-6 md:pt-0 md:pl-6">
                        <h3 className="text-lg font-semibold mb-4 text-white">Active Schedules</h3>
                        
                        {loadingList ? (
                            <p className="text-slate-400">Loading...</p>
                        ) : scheduledPayments.length === 0 ? (
                            <p className="text-slate-500 italic">No active scheduled payments.</p>
                        ) : (
                            <div className="flex flex-col gap-3">
                                {scheduledPayments.map((sp) => (
                                    <div key={sp.id} className="bg-[rgba(15,23,42,0.5)] p-3 rounded-lg border border-white/5">
                                        <div className="flex justify-between items-start mb-2">
                                            <div>
                                                <span className="font-bold text-white">${sp.amount.toFixed(2)}</span>
                                                <span className="text-slate-400 text-sm ml-2">to #{sp.targetAccountId}</span>
                                            </div>
                                            {sp.status === "ACTIVE" && (
                                                <button
                                                    onClick={() => handleCancelScheduled(sp.id!)}
                                                    className="text-xs text-red-400 hover:text-red-300"
                                                >
                                                    Cancel
                                                </button>
                                            )}
                                        </div>
                                        <div className="text-xs text-slate-400">
                                            {sp.recurrenceType} • Next: {new Date(sp.nextExecutionTime).toLocaleString()}
                                        </div>
                                        <div className="text-xs mt-1">
                                            Status: <span className={sp.status === "ACTIVE" ? "text-emerald-400" : "text-slate-500"}>{sp.status}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ScheduledPaymentModal;
