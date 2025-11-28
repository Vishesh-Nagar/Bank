import React from "react";
import type { AccountDto } from "../../../types";
import "./TransactionModal.css";

type Props = {
    visible: boolean;
    mode: "deposit" | "withdraw" | null;
    account: AccountDto | null;
    amount: string;
    setAmount: (s: string) => void;
    onCancel: () => void;
    onConfirm: () => void;
    error: string;
    setError: (s: string) => void;
};

const TransactionModal: React.FC<Props> = ({
    visible,
    mode,
    account,
    amount,
    setAmount,
    onCancel,
    onConfirm,
    error,
    setError,
}) => {
    if (!visible || !account || !mode) return null;
    const isWithdraw = mode === "withdraw";
    return (
        <div className="modal-overlay" onClick={onCancel}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <h2>{isWithdraw ? "💸 Withdraw Money" : "💰 Deposit Money"}</h2>

                <div className="transaction-info">
                    <p><strong>Account:</strong> {account.accountHolderName}</p>
                    <p><strong>Type:</strong> {account.accountType}</p>
                    <p><strong>Current Balance:</strong> ${account.balance.toFixed(2)}</p>
                </div>

                <div className="form-group">
                    <label htmlFor="transactionAmount">Amount:</label>
                    <input
                        id="transactionAmount"
                        type="number"
                        step="0.01"
                        min="0.01"
                        value={amount}
                        onChange={(e) => {
                            setAmount(e.target.value);
                            if (isWithdraw) {
                                const v = parseFloat(e.target.value);
                                if (v > account.balance) setError("Cannot withdraw amount more than current balance");
                                else if (error === "Cannot withdraw amount more than current balance") setError("");
                            }
                        }}
                        placeholder="Enter amount"
                        autoFocus
                    />
                </div>

                {isWithdraw && error && (
                    <div className="error-message">
                        <span>⚠️ {error}</span>
                    </div>
                )}

                {amount && parseFloat(amount) > 0 && (
                    <div className="transaction-preview">
                        <p>
                            New Balance: $
                            {isWithdraw
                                ? (account.balance - parseFloat(amount)).toFixed(2)
                                : (account.balance + parseFloat(amount)).toFixed(2)}
                        </p>
                    </div>
                )}

                <div className="modal-actions">
                    <button
                        onClick={onConfirm}
                        className={`btn ${isWithdraw ? "btn-warning" : "btn-success"}`}
                        disabled={
                            !amount ||
                            parseFloat(amount) <= 0 ||
                            (isWithdraw && parseFloat(amount) > account.balance)
                        }
                    >
                        {isWithdraw ? "Withdraw" : "Deposit"}
                    </button>
                    <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
                </div>
            </div>
        </div>
    );
};

export default TransactionModal;
