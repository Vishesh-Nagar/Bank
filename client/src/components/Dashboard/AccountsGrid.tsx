import React from "react";
import type { AccountDto } from "../../types";
import AccountCard from "./AccountCard";

type Props = {
    accounts: AccountDto[];
    refreshing?: boolean;
    onPay: (acc: AccountDto) => void;
    onSchedule: (acc: AccountDto) => void;
    onDelete: (id: number) => void;
    onHistory: (acc: AccountDto) => void;
};

const AccountsGrid: React.FC<Props> = ({ accounts, refreshing = false, onPay, onSchedule, onDelete, onHistory }) => {
    if (accounts.length === 0) {
        return (
            <div className="grid accounts-grid gap-6 mb-8">
                <div className="text-center text-slate-500 text-lg py-20 px-10 bg-gradient-to-br from-[#1e293b] to-[#0f172a] rounded-2xl border-2 border-dashed border-white/10">
                    <p className="text-2xl font-semibold text-slate-400 mb-3">No accounts found.</p>
                    <p>Create your first account to get started!</p>
                </div>
            </div>
        );
    }

    return (
        <div className="grid accounts-grid gap-6 mb-8">
            {accounts.map((acc) => (
                <AccountCard
                    key={acc.id}
                    account={acc}
                    refreshing={refreshing}
                    onPay={() => onPay(acc)}
                    onSchedule={() => onSchedule(acc)}
                    onDelete={() => onDelete(acc.id)}
                    onHistory={() => onHistory(acc)}
                />
            ))}
        </div>
    );
};

export default AccountsGrid;
