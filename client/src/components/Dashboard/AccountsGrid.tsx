import React from "react";
import type { AccountDto } from "../../types";
import AccountCard from "./AccountCard";
import "./AccountsGrid.css";

type Props = {
    accounts: AccountDto[];
    onDeposit: (acc: AccountDto) => void;
    onWithdraw: (acc: AccountDto) => void;
    onDelete: (id: number) => void;
};

const AccountsGrid: React.FC<Props> = ({ accounts, onDeposit, onWithdraw, onDelete }) => {
    if (accounts.length === 0) {
        return (
            <div className="accounts-grid">
                <div className="no-accounts">
                    <p>No accounts found.</p>
                    <p>Create your first account to get started!</p>
                </div>
            </div>
        );
    }

    return (
        <div className="accounts-grid">
            {accounts.map((acc) => (
                <AccountCard
                    key={acc.id}
                    account={acc}
                    onDeposit={() => onDeposit(acc)}
                    onWithdraw={() => onWithdraw(acc)}
                    onDelete={() => onDelete(acc.id)}
                />
            ))}
        </div>
    );
};

export default AccountsGrid;
