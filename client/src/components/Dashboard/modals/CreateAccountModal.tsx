import React from "react";
import type { AccountCreateDto, AccountType } from "../../../types";
import "./CreateAccountModal.css";

type Props = {
    visible: boolean;
    onClose: () => void;
    newAccount: AccountCreateDto;
    setNewAccount: (s: AccountCreateDto) => void;
    onCreate: (payload: AccountCreateDto) => void;
};

const CreateAccountModal: React.FC<Props> = ({ visible, onClose, newAccount, setNewAccount, onCreate }) => {
    if (!visible) return null;
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <h2>Create New Account</h2>
                <form onSubmit={(e) => { e.preventDefault(); onCreate(newAccount); }}>
                    <div className="form-group">
                        <label htmlFor="initialBalance">Initial Balance:</label>
                        <input
                            id="initialBalance"
                            type="number"
                            step="0.01"
                            min="0"
                            value={newAccount.balance}
                            onChange={(e) =>
                                setNewAccount({
                                    ...newAccount,
                                    balance: parseFloat(e.target.value) || 0,
                                })
                            }
                            placeholder="0.00"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="accountType">Account Type:</label>
                        <select
                            id="accountType"
                            value={newAccount.accountType}
                            onChange={(e) =>
                                setNewAccount({
                                    ...newAccount,
                                    accountType: e.target.value as AccountType,
                                })
                            }
                        >
                            <option value="SAVINGS">Savings Account</option>
                            <option value="CURRENT">Current Account</option>
                        </select>
                    </div>

                    <div className="modal-actions">
                        <button type="submit" className="btn btn-primary">Create Account</button>
                        <button type="button" onClick={onClose} className="btn btn-secondary">Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateAccountModal;
