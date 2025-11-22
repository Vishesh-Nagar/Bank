import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
    getAllAccounts,
    createAccount,
    deposit,
    withdraw,
    deleteAccount,
} from "../services/accountService";
import type { AccountDto, AccountCreateDto, AccountType } from "../types";
import "./Dashboard.css";
import {
    isAuthenticated,
    logout,
    getCurrentUser,
} from "../services/userService";

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState<AccountDto[]>([]); // Ensure initial value is array
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>("");
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [selectedAccount, setSelectedAccount] = useState<AccountDto | null>(
        null
    );
    const [transactionModal, setTransactionModal] = useState<
        "deposit" | "withdraw" | null
    >(null);

    // Form states
    const [newAccount, setNewAccount] = useState<AccountCreateDto>({
        accountHolderName: "",
        balance: 0,
        accountType: "SAVINGS",
    });
    const [transactionAmount, setTransactionAmount] = useState<string>("");

    useEffect(() => {
        fetchAccounts();
    }, []);

    const fetchAccounts = async () => {
        // Check if user is authenticated
        if (!isAuthenticated()) {
            navigate("/login");
            return;
        }

        try {
            setLoading(true);
            setError("");
            const data = await getAllAccounts();

            // Ensure data is always an array
            if (Array.isArray(data)) {
                setAccounts(data);
            } else {
                console.error("API returned non-array data:", data);
                setAccounts([]);
                setError("Received invalid data format from server");
            }
        } catch (err: any) {
            console.error("Error fetching accounts:", err);
            if (err.response?.status === 401) {
                logout(); // Clear stored user data
                navigate("/login");
            } else {
                setError(
                    err.response?.data?.message || "Failed to fetch accounts"
                );
                setAccounts([]);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCreateAccount = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setError("");
            await createAccount(newAccount);
            setShowCreateModal(false);
            setNewAccount({
                accountHolderName: "",
                balance: 0,
                accountType: "SAVINGS",
            });
            await fetchAccounts(); // Refresh accounts list
        } catch (err: any) {
            console.error("Error creating account:", err);
            setError(err.response?.data?.message || "Failed to create account");
        }
    };

    const handleDeposit = async () => {
        if (!selectedAccount || !transactionAmount) return;

        const amount = parseFloat(transactionAmount);
        if (isNaN(amount) || amount <= 0) {
            setError("Please enter a valid amount");
            return;
        }

        try {
            setError("");
            await deposit(selectedAccount.id, amount);
            setTransactionModal(null);
            setSelectedAccount(null);
            setTransactionAmount("");
            await fetchAccounts(); // Refresh accounts list
        } catch (err: any) {
            console.error("Error depositing:", err);
            setError(err.response?.data?.message || "Failed to deposit");
        }
    };

    const handleWithdraw = async () => {
        if (!selectedAccount || !transactionAmount) return;

        const amount = parseFloat(transactionAmount);
        if (isNaN(amount) || amount <= 0) {
            setError("Please enter a valid amount");
            return;
        }

        try {
            setError("");
            await withdraw(selectedAccount.id, amount);
            setTransactionModal(null);
            setSelectedAccount(null);
            setTransactionAmount("");
            await fetchAccounts(); // Refresh accounts list
        } catch (err: any) {
            console.error("Error withdrawing:", err);
            setError(
                err.response?.data?.message ||
                    err.response?.data ||
                    "Failed to withdraw"
            );
        }
    };

    const handleDeleteAccount = async (id: number) => {
        if (!window.confirm("Are you sure you want to delete this account?"))
            return;

        try {
            setError("");
            await deleteAccount(id);
            await fetchAccounts(); // Refresh accounts list
        } catch (err: any) {
            console.error("Error deleting account:", err);
            setError(err.response?.data?.message || "Failed to delete account");
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("user");
        sessionStorage.clear();
        navigate("/login");
    };

    if (loading) {
        return (
            <div className="loading-container">
                <div className="spinner"></div>
                <p>Loading accounts...</p>
            </div>
        );
    }

    return (
        <div className="dashboard">
            <header className="dashboard-header">
                <h1>Banking Dashboard</h1>
                <div className="header-right">
                    <span className="username">
                        Welcome, {getCurrentUser()?.username}
                    </span>
                    <div className="header-buttons">
                        <button className="btn btn-primary">New Button</button>
                        <button
                            onClick={handleLogout}
                            className="btn btn-secondary"
                        >
                            Logout
                        </button>
                    </div>
                </div>
            </header>

            {error && (
                <div className="error-message">
                    <span>⚠️ {error}</span>
                    <button onClick={() => setError("")} className="close-btn">
                        &times;
                    </button>
                </div>
            )}

            <div className="dashboard-actions">
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="btn btn-primary"
                >
                    + Create New Account
                </button>
                <button onClick={fetchAccounts} className="btn btn-secondary">
                    🔄 Refresh
                </button>
            </div>

            <div className="accounts-summary">
                <div className="summary-card">
                    <h3>Total Accounts</h3>
                    <p className="summary-value">{accounts.length}</p>
                </div>
                <div className="summary-card">
                    <h3>Total Balance</h3>
                    <p className="summary-value">
                        $
                        {accounts
                            .reduce((sum, acc) => sum + acc.balance, 0)
                            .toFixed(2)}
                    </p>
                </div>
            </div>

            <div className="accounts-grid">
                {accounts.length === 0 ? (
                    <div className="no-accounts">
                        <p>No accounts found.</p>
                        <p>Create your first account to get started!</p>
                    </div>
                ) : (
                    accounts.map((account) => (
                        <div key={account.id} className="account-card">
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
                                    onClick={() => {
                                        setSelectedAccount(account);
                                        setTransactionModal("deposit");
                                        setTransactionAmount("");
                                    }}
                                    className="btn btn-success btn-sm"
                                    title="Deposit money"
                                >
                                    💰 Deposit
                                </button>
                                <button
                                    onClick={() => {
                                        setSelectedAccount(account);
                                        setTransactionModal("withdraw");
                                        setTransactionAmount("");
                                    }}
                                    className="btn btn-warning btn-sm"
                                    title="Withdraw money"
                                >
                                    💸 Withdraw
                                </button>
                                <button
                                    onClick={() =>
                                        handleDeleteAccount(account.id)
                                    }
                                    className="btn btn-danger btn-sm"
                                    title="Delete account"
                                >
                                    🗑️ Delete
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* Create Account Modal */}
            {showCreateModal && (
                <div
                    className="modal-overlay"
                    onClick={() => setShowCreateModal(false)}
                >
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <h2>Create New Account</h2>
                        <form onSubmit={handleCreateAccount}>
                            <div className="form-group">
                                <label htmlFor="accountHolder">
                                    Account Holder Name:
                                </label>
                                <input
                                    id="accountHolder"
                                    type="text"
                                    value={newAccount.accountHolderName}
                                    onChange={(e) =>
                                        setNewAccount({
                                            ...newAccount,
                                            accountHolderName: e.target.value,
                                        })
                                    }
                                    placeholder="Enter full name"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="initialBalance">
                                    Initial Balance:
                                </label>
                                <input
                                    id="initialBalance"
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    value={newAccount.balance}
                                    onChange={(e) =>
                                        setNewAccount({
                                            ...newAccount,
                                            balance:
                                                parseFloat(e.target.value) || 0,
                                        })
                                    }
                                    placeholder="0.00"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="accountType">
                                    Account Type:
                                </label>
                                <select
                                    id="accountType"
                                    value={newAccount.accountType}
                                    onChange={(e) =>
                                        setNewAccount({
                                            ...newAccount,
                                            accountType: e.target
                                                .value as AccountType,
                                        })
                                    }
                                >
                                    <option value="SAVINGS">
                                        Savings Account
                                    </option>
                                    <option value="CURRENT">
                                        Current Account
                                    </option>
                                </select>
                            </div>

                            <div className="modal-actions">
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                >
                                    Create Account
                                </button>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowCreateModal(false);
                                        setNewAccount({
                                            accountHolderName: "",
                                            balance: 0,
                                            accountType: "SAVINGS",
                                        });
                                    }}
                                    className="btn btn-secondary"
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Transaction Modal */}
            {transactionModal && selectedAccount && (
                <div
                    className="modal-overlay"
                    onClick={() => {
                        setTransactionModal(null);
                        setTransactionAmount("");
                        setError("");
                    }}
                >
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <h2>
                            {transactionModal === "deposit"
                                ? "💰 Deposit Money"
                                : "💸 Withdraw Money"}
                        </h2>

                        <div className="transaction-info">
                            <p>
                                <strong>Account:</strong>{" "}
                                {selectedAccount.accountHolderName}
                            </p>
                            <p>
                                <strong>Type:</strong>{" "}
                                {selectedAccount.accountType}
                            </p>
                            <p>
                                <strong>Current Balance:</strong> $
                                {selectedAccount.balance.toFixed(2)}
                            </p>
                        </div>

                        <div className="form-group">
                            <label htmlFor="transactionAmount">Amount:</label>
                            <input
                                id="transactionAmount"
                                type="number"
                                step="0.01"
                                min="0.01"
                                value={transactionAmount}
                                onChange={(e) =>
                                    setTransactionAmount(e.target.value)
                                }
                                placeholder="Enter amount"
                                autoFocus
                            />
                        </div>

                        {transactionAmount &&
                            parseFloat(transactionAmount) > 0 && (
                                <div className="transaction-preview">
                                    <p>
                                        New Balance: $
                                        {transactionModal === "deposit"
                                            ? (
                                                  selectedAccount.balance +
                                                  parseFloat(transactionAmount)
                                              ).toFixed(2)
                                            : (
                                                  selectedAccount.balance -
                                                  parseFloat(transactionAmount)
                                              ).toFixed(2)}
                                    </p>
                                </div>
                            )}

                        <div className="modal-actions">
                            <button
                                onClick={
                                    transactionModal === "deposit"
                                        ? handleDeposit
                                        : handleWithdraw
                                }
                                className={`btn ${
                                    transactionModal === "deposit"
                                        ? "btn-success"
                                        : "btn-warning"
                                }`}
                                disabled={
                                    !transactionAmount ||
                                    parseFloat(transactionAmount) <= 0
                                }
                            >
                                {transactionModal === "deposit"
                                    ? "Deposit"
                                    : "Withdraw"}
                            </button>
                            <button
                                onClick={() => {
                                    setTransactionModal(null);
                                    setTransactionAmount("");
                                    setError("");
                                }}
                                className="btn btn-secondary"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;
