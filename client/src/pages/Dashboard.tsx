import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
    getAllAccounts,
    createAccount,
    deposit,
    withdraw,
    deleteAccount,
} from "../services/accountService";
import type { AccountDto, AccountCreateDto } from "../types";
import "./Dashboard.css";
import {
    isAuthenticated,
    logout,
    getCurrentUser,
} from "../services/userService";
import DashboardHeader from "../components/Dashboard/DashboardHeader";
import MobileSidebar from "../components/Dashboard/MobileSidebar";
import SummaryCards from "../components/Dashboard/SummaryCards";
import AccountsGrid from "../components/Dashboard/AccountsGrid";
import CreateAccountModal from "../components/Dashboard/modals/CreateAccountModal";
import TransactionModal from "../components/Dashboard/modals/TransactionModal";

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState<AccountDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>("");
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [selectedAccount, setSelectedAccount] = useState<AccountDto | null>(
        null
    );
    const [transactionModal, setTransactionModal] = useState<
        "deposit" | "withdraw" | null
    >(null);
    const currentUser = getCurrentUser();
    const [newAccount, setNewAccount] = useState<AccountCreateDto>({
        accountHolderName: currentUser?.username || "",
        balance: 0,
        accountType: "SAVINGS",
    });
    const [transactionAmount, setTransactionAmount] = useState<string>("");
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    useEffect(() => {
        fetchAccounts();
    }, []);

    const fetchAccounts = async () => {
        if (!isAuthenticated()) {
            navigate("/login");
            return;
        }
        try {
            setLoading(true);
            setError("");
            const data = await getAllAccounts();
            const cur = getCurrentUser();
            if (Array.isArray(data)) {
                setAccounts(
                    data.filter(
                        (acc) => acc.accountHolderName === cur?.username
                    )
                );
            } else {
                setAccounts([]);
                setError("Received invalid data format from server");
            }
        } catch (err: any) {
            if (err.response?.status === 401) {
                logout();
                navigate("/login");
            } else {
                setError(err.response?.data?.message || "Failed to fetch accounts");
                setAccounts([]);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCreateAccount = async (payload: AccountCreateDto) => {
        try {
            setError("");
            await createAccount(payload);
            setShowCreateModal(false);
            setNewAccount({
                accountHolderName: currentUser?.username || "",
                balance: 0,
                accountType: "SAVINGS",
            });
            await fetchAccounts();
        } catch (err: any) {
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
            await fetchAccounts();
        } catch (err: any) {
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
            await fetchAccounts();
        } catch (err: any) {
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
            await fetchAccounts();
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to delete account");
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("user");
        sessionStorage.clear();
        navigate("/");
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
            <DashboardHeader
                onOpenSidebar={() => setIsSidebarOpen(true)}
                username={currentUser?.username || ""}
                onLogout={handleLogout}
            />

            {isSidebarOpen && (
                <MobileSidebar
                    username={currentUser?.username || ""}
                    onClose={() => setIsSidebarOpen(false)}
                    onLogout={() => {
                        setIsSidebarOpen(false);
                        handleLogout();
                    }}
                />
            )}

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

            <SummaryCards accounts={accounts} />

            <AccountsGrid
                accounts={accounts}
                onDeposit={(acc) => {
                    setSelectedAccount(acc);
                    setTransactionModal("deposit");
                    setTransactionAmount("");
                }}
                onWithdraw={(acc) => {
                    setSelectedAccount(acc);
                    setTransactionModal("withdraw");
                    setTransactionAmount("");
                }}
                onDelete={(id) => handleDeleteAccount(id)}
            />

            <CreateAccountModal
                visible={showCreateModal}
                onClose={() => {
                    setShowCreateModal(false);
                    setNewAccount({
                        accountHolderName: currentUser?.username || "",
                        balance: 0,
                        accountType: "SAVINGS",
                    });
                    setError("");
                }}
                newAccount={newAccount}
                setNewAccount={setNewAccount}
                onCreate={(payload) => handleCreateAccount(payload)}
            />

            <TransactionModal
                visible={!!transactionModal && !!selectedAccount}
                mode={transactionModal}
                account={selectedAccount}
                amount={transactionAmount}
                setAmount={setTransactionAmount}
                onCancel={() => {
                    setTransactionModal(null);
                    setTransactionAmount("");
                    setError("");
                    setSelectedAccount(null);
                }}
                onConfirm={transactionModal === "deposit" ? handleDeposit : handleWithdraw}
                error={error}
                setError={setError}
            />
        </div>
    );
};

export default Dashboard;
