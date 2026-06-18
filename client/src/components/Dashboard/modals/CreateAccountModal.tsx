import React, { useState, useEffect } from "react";
import type { AccountCreateDto, AccountType } from "../../../types";

type Props = {
    visible: boolean;
    onClose: () => void;
    newAccount: AccountCreateDto;
    setNewAccount: (s: AccountCreateDto) => void;
    onCreate: (payload: AccountCreateDto) => void;
};

/* Shared input/select style */
const inputClass =
    "w-full px-4 py-[14px] bg-[rgba(15,23,42,0.6)] border border-white/10 rounded-[10px] text-white text-[15px] transition-all duration-300 " +
    "focus:outline-none focus:border-blue-500 focus:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] focus:bg-[rgba(15,23,42,0.8)] " +
    "placeholder:text-slate-500";

const CreateAccountModal: React.FC<Props> = ({ visible, onClose, newAccount, setNewAccount, onCreate }) => {
    const [balanceStr, setBalanceStr] = useState("");

    // Reset local state when modal becomes visible
    useEffect(() => {
        if (visible) {
            setBalanceStr(newAccount.balance ? newAccount.balance.toString() : "");
        }
    }, [visible, newAccount.balance]);

    if (!visible) return null;

    return (
        /* Overlay */
        <div
            className="fixed inset-0 bg-black/85 backdrop-blur-[8px] flex justify-center items-center z-[1000] animate-modal-fade"
            onClick={onClose}
        >
            {/* Modal */}
            <div
                className="bg-gradient-to-br from-[#1e293b] to-[#0f172a] p-8 rounded-[20px] min-w-[450px] max-w-[550px] w-full shadow-[0_20px_60px_rgba(0,0,0,0.6)] border border-white/10 animate-modal-slide"
                onClick={(e) => e.stopPropagation()}
            >
                <h2 className="m-0 mb-6 text-2xl font-bold text-white">Create New Account</h2>

                <form onSubmit={(e) => { e.preventDefault(); onCreate(newAccount); }}>
                    {/* Initial Balance */}
                    <div className="mb-6">
                        <label htmlFor="initialBalance" className="block mb-2 text-sm font-semibold text-slate-300">
                            Initial Balance:
                        </label>
                        <input
                            id="initialBalance"
                            type="text"
                            inputMode="decimal"
                            value={balanceStr}
                            onChange={(e) => {
                                const val = e.target.value;
                                if (val === "" || /^\d*\.?\d{0,2}$/.test(val)) {
                                    setBalanceStr(val);
                                    setNewAccount({
                                        ...newAccount,
                                        balance: parseFloat(val) || 0,
                                    });
                                }
                            }}
                            placeholder="0.00"
                            required
                            className={inputClass}
                        />
                    </div>

                    {/* Account Type */}
                    <div className="mb-6">
                        <label htmlFor="accountType" className="block mb-2 text-sm font-semibold text-slate-300">
                            Account Type:
                        </label>
                        <select
                            id="accountType"
                            value={newAccount.accountType}
                            onChange={(e) =>
                                setNewAccount({
                                    ...newAccount,
                                    accountType: e.target.value as AccountType,
                                })
                            }
                            className={inputClass}
                        >
                            <option value="SAVINGS">Savings Account</option>
                            <option value="CURRENT">Current Account</option>
                        </select>
                    </div>

                    {/* Actions */}
                    <div className="flex gap-3 mt-8 [&>button]:flex-1">
                        <button type="submit" className="btn btn-primary">
                            Create Account
                        </button>
                        <button type="button" onClick={onClose} className="btn btn-secondary">
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateAccountModal;
