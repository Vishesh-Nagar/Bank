import React from "react";
import type { AccountDto } from "../../types";
import "./AccountCard.css";

type Props = {
    account: AccountDto;
    onDeposit: () => void;
    onWithdraw: () => void;
    onDelete: () => void;
};

const AccountCard: React.FC<Props> = ({ account, onDeposit, onWithdraw, onDelete }) => {
    return (
        <div className="account-card">
            <div className="account-header">
                <h3>{account.accountHolderName}</h3>
                <span className={`account-type ${account.accountType.toLowerCase()}`}>
                    {account.accountType}
                </span>
            </div>

            <div className="account-id">
                <small>Account ID: {account.id}</small>
            </div>

            <div className="account-balance">
                <span className="balance-label">Balance:</span>
                <span className="balance-amount">${account.balance.toFixed(2)}</span>
            </div>

            <div className="account-actions">
                <button onClick={onDeposit} className="btn btn-success btn-sm" title="Deposit money">
                    💰 Deposit
                </button>
                <button onClick={onWithdraw} className="btn btn-warning btn-sm" title="Withdraw money">
                    💸 Withdraw
                </button>
                <button onClick={onDelete} className="btn btn-danger btn-sm" title="Delete account">
                    🗑️ Delete
                </button>
            </div>
        </div>
    );
};

export default AccountCard;
