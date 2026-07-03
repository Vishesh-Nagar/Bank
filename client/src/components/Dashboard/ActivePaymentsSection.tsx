import React, { useEffect, useState } from "react";
import { getPaymentHistory, getScheduledPayments, cancelScheduledPayment } from "../../services/paymentService";
import type { AccountDto, PaymentStatusDto, ScheduledPaymentDto } from "../../types";

type Props = {
    accounts: AccountDto[];
    refreshTrigger: number; // to trigger re-fetch when accounts refresh
};

const ActivePaymentsSection: React.FC<Props> = ({ accounts, refreshTrigger }) => {
    const [pendingPayments, setPendingPayments] = useState<PaymentStatusDto[]>([]);
    const [scheduledPayments, setScheduledPayments] = useState<ScheduledPaymentDto[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchAll = async () => {
            if (!accounts || accounts.length === 0) {
                setPendingPayments([]);
                setScheduledPayments([]);
                return;
            }
            
            setLoading(true);
            try {
                const pending: PaymentStatusDto[] = [];
                const scheduled: ScheduledPaymentDto[] = [];
                
                // Fetch for each account
                for (const acc of accounts) {
                    const [history, sched] = await Promise.all([
                        getPaymentHistory(acc.id, 0, 50).catch(() => []),
                        getScheduledPayments(acc.id).catch(() => [])
                    ]);
                    
                    if (Array.isArray(history)) {
                        pending.push(...history.filter(p => p.status === "PENDING" || p.status === "PROCESSING" || p.status === "QUEUED"));
                    }
                    if (Array.isArray(sched)) {
                        scheduled.push(...sched.filter(s => s.status === "ACTIVE"));
                    }
                }
                
                // Sort by date
                pending.sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime());
                scheduled.sort((a, b) => new Date(a.nextExecutionTime).getTime() - new Date(b.nextExecutionTime).getTime());
                
                setPendingPayments(pending);
                setScheduledPayments(scheduled);
            } catch (err) {
                console.error("Failed to fetch active payments", err);
            } finally {
                setLoading(false);
            }
        };
        
        fetchAll();
    }, [accounts, refreshTrigger]);

    const handleCancel = async (id: number) => {
        if (!window.confirm("Are you sure you want to cancel this scheduled payment?")) return;
        try {
            await cancelScheduledPayment(id);
            setScheduledPayments(prev => prev.filter(sp => sp.id !== id));
        } catch (err) {
            console.error("Failed to cancel", err);
            alert("Failed to cancel scheduled payment");
        }
    };

    if (pendingPayments.length === 0 && scheduledPayments.length === 0) {
        return null; // Don't show the section if nothing is active
    }

    return (
        <div className="mb-8 bg-gradient-to-br from-[#1e293b] to-[#0f172a] rounded-2xl p-6 shadow-[0_4px_20px_rgba(0,0,0,0.4)] border border-white/10 animate-fade-in">
            <h2 className="text-xl font-semibold text-white mb-6 flex items-center gap-3">
                <span className="text-2xl">⏳</span> 
                Active Payments
                {loading && <div className="w-4 h-4 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin" />}
            </h2>
            
            <div className="grid gap-6 md:grid-cols-2">
                {/* Pending Payments Column */}
                {pendingPayments.length > 0 && (
                    <div>
                        <h3 className="text-[13px] font-bold text-amber-500/80 uppercase tracking-[1.5px] mb-4">Processing</h3>
                        <div className="flex flex-col gap-3">
                            {pendingPayments.map(p => (
                                <div key={p.paymentId} className="bg-slate-800/40 p-4 rounded-xl border border-amber-500/10 flex justify-between items-center transition-all duration-300 hover:border-amber-500/30">
                                    <div>
                                        <div className="text-amber-400 font-bold text-lg">${p.amount.toFixed(2)}</div>
                                        <div className="text-slate-400 text-xs mt-1">From #{p.sourceAccountId} to #{p.targetAccountId}</div>
                                    </div>
                                    <div className="text-right">
                                        <div className="text-amber-500 text-sm font-medium animate-pulse">Pending</div>
                                        <div className="text-slate-500 text-[10px] mt-1">{new Date(p.submittedAt).toLocaleDateString()}</div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
                
                {/* Scheduled Payments Column */}
                {scheduledPayments.length > 0 && (
                    <div>
                        <h3 className="text-[13px] font-bold text-blue-400/80 uppercase tracking-[1.5px] mb-4">Scheduled</h3>
                        <div className="flex flex-col gap-3">
                            {scheduledPayments.map(sp => (
                                <div key={sp.id} className="bg-slate-800/40 p-4 rounded-xl border border-blue-500/10 flex justify-between items-center transition-all duration-300 hover:border-blue-500/30">
                                    <div>
                                        <div className="text-emerald-400 font-bold text-lg">${sp.amount.toFixed(2)}</div>
                                        <div className="text-slate-400 text-xs mt-1">From #{sp.sourceAccountId} to #{sp.targetAccountId}</div>
                                    </div>
                                    <div className="text-right flex flex-col items-end">
                                        <div className="text-blue-400 text-[13px] font-semibold tracking-wide">{sp.recurrenceType}</div>
                                        <div className="text-slate-500 text-[10px] mt-1 mb-2">Next: {new Date(sp.nextExecutionTime).toLocaleDateString()}</div>
                                        <button 
                                            onClick={() => handleCancel(sp.id!)} 
                                            className="px-2 py-1 bg-red-500/10 hover:bg-red-500/20 text-red-400 text-[11px] font-semibold rounded transition-colors"
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ActivePaymentsSection;
