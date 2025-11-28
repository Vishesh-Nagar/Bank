import React from "react";
import type { AccountDto } from "../../types";
import "./SummaryCards.css";

type Props = {
    accounts: AccountDto[];
};

const SummaryCards: React.FC<Props> = ({ accounts }) => {
    const totalBalance = accounts.reduce((s, a) => s + a.balance, 0);
    return (
        <div className="accounts-summary">
            <div className="summary-card">
                <h3>Total Accounts</h3>
                <p className="summary-value">{accounts.length}</p>
            </div>
            <div className="summary-card">
                <h3>Total Balance</h3>
                <p className="summary-value">${totalBalance.toFixed(2)}</p>
            </div>
        </div>
    );
};

export default SummaryCards;
