import React, { useState } from "react";
import type { AccountDto } from "../../types";

type Props = {
    account: AccountDto;
    refreshing?: boolean;
    onPay: () => void;
    onDelete: () => void | Promise<void>;
};

const AccountCard: React.FC<Props> = ({
    account,
    refreshing = false,
    onPay,
    onDelete,
}) => {
    const [blocked, setBlocked] = useState(false);

    const handleAction = async (
        action: () => void | Promise<void>,
        shortDelay = false
    ) => {
        if (blocked) return;
        setBlocked(true);
        try {
            const res = action();
            if (res && typeof (res as any).then === "function") {
                await res;
            }
            if (shortDelay) {
                setTimeout(() => setBlocked(false), 800);
            } else {
                setBlocked(false);
            }
        } catch (err) {
            setBlocked(false);
            throw err;
        }
    };

    const typeLower = account.accountType.toLowerCase();
    const typeBadgeClass =
        typeLower === "savings"
            ? "bg-blue-500/20 text-blue-400 border border-blue-500/30"
            : "bg-violet-500/20 text-violet-400 border border-violet-500/30";

    return (
        <div
            className={`relative overflow-hidden bg-gradient-to-br from-[#1e293b] to-[#0f172a] rounded-2xl p-6 shadow-[0_4px_20px_rgba(0,0,0,0.4)] border border-white/10 transition-all duration-300 hover:-translate-y-1.5 hover:shadow-[0_12px_40px_rgba(59,130,246,0.3)] hover:border-blue-500/30 ${refreshing ? "animate-card-pulse" : ""}`}
        >
            {/* Gradient top accent bar */}
            <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-500 to-purple-500" />

            {/* Header */}
            <div className="flex justify-between items-start mb-5 pb-4 border-b border-white/10">
                <h3 className="text-xl font-semibold text-white">{account.accountHolderName}</h3>
                <span className={`px-[14px] py-1.5 rounded-full text-[11px] font-bold uppercase ${typeBadgeClass}`}>
                    {account.accountType}
                </span>
            </div>

            {/* Account ID */}
            <div className="mb-4">
                <small className="text-xs font-medium text-slate-500">Account ID: {account.id}</small>
            </div>

            {/* Balance */}
            <div className="flex justify-between items-center p-5 bg-slate-800/50 rounded-xl mb-5">
                <span className="text-sm font-semibold text-slate-400">Balance:</span>
                <span
                    className="text-[28px] font-bold text-emerald-500"
                    style={{ textShadow: "0 2px 10px rgba(16,185,129,0.3)" }}
                >
                    ${account.balance.toFixed(2)}
                </span>
            </div>

            {/* Actions */}
            <div className="flex gap-2 flex-wrap [&>button]:flex-1 [&>button]:min-w-[90px]">
                <button
                    onClick={() => handleAction(onPay, true)}
                    disabled={blocked}
                    className="btn btn-pay btn-sm"
                    title="Send payment to another user"
                >
                    💳 Pay
                </button>
                <button
                    onClick={() => handleAction(onDelete)}
                    disabled={blocked}
                    className="btn btn-danger btn-sm"
                    title="Delete account"
                >
                    🗑️ Delete
                </button>
            </div>
        </div>
    );
};

export default AccountCard;
