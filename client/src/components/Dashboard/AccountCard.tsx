import React, { useState } from "react";
import type { AccountDto } from "../../types";
import "./AccountCard.css";

type Props = {
    account: AccountDto;
    onDeposit: () => void;
    onWithdraw: () => void;
    onPay: () => void;
    onDelete: () => void | Promise<void>;
};

const AccountCard: React.FC<Props> = ({
    account,
    onDeposit,
    onWithdraw,
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

    return (
        <div className="account-card">
            <div className="account-header">
                <h3>{account.accountHolderName}</h3>
                <span
                    className={`account-type ${account.accountType.toLowerCase()}`}
                >
                    {account.accountType}
                </span>
            </div>

            <div className="account-id">
                <small>Account ID: {account.id}</small>
            </div>

            <div className="account-balance">
                <span className="balance-label">Balance:</span>
                <span className="balance-amount">
                    ${account.balance.toFixed(2)}
                </span>
            </div>

            <div className="account-actions">
                <button
                    onClick={() => handleAction(onDeposit, true)}
                    disabled={blocked}
                    className="btn btn-success btn-sm"
                    title="Deposit money"
                >
                    💰 Deposit
                </button>
                <button
                    onClick={() => handleAction(onWithdraw, true)}
                    disabled={blocked}
                    className="btn btn-warning btn-sm"
                    title="Withdraw money"
                >
                    💸 Withdraw
                </button>
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
