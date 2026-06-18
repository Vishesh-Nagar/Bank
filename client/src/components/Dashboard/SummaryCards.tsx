import React from "react";
import type { AccountDto } from "../../types";

type Props = {
    accounts: AccountDto[];
    refreshing?: boolean;
};

const cardBase =
    "bg-gradient-to-br from-[#1e293b] to-[#0f172a] rounded-2xl p-7 " +
    "shadow-[0_4px_20px_rgba(0,0,0,0.4)] border border-blue-500/20 " +
    "transition-all duration-300 hover:-translate-y-1 " +
    "hover:shadow-[0_8px_30px_rgba(59,130,246,0.3)] hover:border-blue-500/40";

const SummaryCards: React.FC<Props> = ({ accounts, refreshing = false }) => {
    const totalBalance = accounts.reduce((s, a) => s + a.balance, 0);
    return (
        <div className="grid summary-grid gap-6 mb-10">
            {/* Total Balance */}
            <div className={`${cardBase} ${refreshing ? "animate-balance-pulse" : ""}`}>
                <h3 className="mb-4 text-sm font-semibold text-slate-400 uppercase tracking-[1px]">
                    Total Balance
                </h3>
                <p className="text-4xl font-bold bg-gradient-to-br from-blue-500 to-purple-500 bg-clip-text text-transparent">
                    ${totalBalance.toFixed(2)}
                </p>
            </div>

            {/* Total Accounts */}
            <div className={`${cardBase} ${refreshing ? "animate-balance-pulse" : ""}`}>
                <h3 className="mb-4 text-sm font-semibold text-slate-400 uppercase tracking-[1px]">
                    Total Accounts
                </h3>
                <p className="text-4xl font-bold bg-gradient-to-br from-blue-500 to-purple-500 bg-clip-text text-transparent">
                    {accounts.length}
                </p>
            </div>
        </div>
    );
};

export default SummaryCards;
