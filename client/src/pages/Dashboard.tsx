import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
    getAllAccounts,
    createAccount,
    deleteAccount,
} from "../services/accountService";
import type {
    AccountDto,
    AccountCreateDto,
    NotificationDto,
} from "../types";
import {
    isAuthenticated,
    logout,
    getCurrentUser,
} from "../services/userService";
import { websocketService } from "../services/websocketService";
import DashboardHeader from "../components/Dashboard/DashboardHeader";
import MobileSidebar from "../components/Dashboard/MobileSidebar";
import SummaryCards from "../components/Dashboard/SummaryCards";
import AccountsGrid from "../components/Dashboard/AccountsGrid";
import CreateAccountModal from "../components/Dashboard/modals/CreateAccountModal";
import PaymentModal from "../components/Dashboard/modals/PaymentModal";
import PaymentHistory from "../components/Dashboard/PaymentHistory";
import NotificationToast from "../components/Notifications/NotificationToast";

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState<AccountDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string>("");
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Payment state
    const [paymentAccount, setPaymentAccount] = useState<AccountDto | null>(null);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [historyAccount, setHistoryAccount] = useState<AccountDto | null>(null);
    const [showHistory, setShowHistory] = useState(false);

    // Notification state
    const [notifications, setNotifications] = useState<NotificationDto[]>([]);

    const currentUser = getCurrentUser();
    const [newAccount, setNewAccount] = useState<AccountCreateDto>({
        accountHolderName: currentUser?.username || "",
        balance: 0,
        accountType: "SAVINGS",
    });
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    /**
     * Stable ref that tracks whether a balance-refresh is already scheduled.
     * Avoids hammering fetchAccounts when multiple notifications arrive closely.
     */
    const refreshScheduled = React.useRef(false);

    /**
     * Central notification handler — the ONLY place that writes to `notifications`
     * state (except dismiss). Called for both server WebSocket events AND the
     * synthetic PAYMENT_QUEUED optimistic toast.
     *
     * Deduplication is already handled at the service layer (websocketService._dispatch).
     * Here we only filter out BALANCE_CHANGED events caused by payments (they are
     * redundant with PAYMENT_COMPLETED / PAYMENT_RECEIVED toasts).
     */
    const addNotification = useCallback(
        (notification: NotificationDto) => {
            // Filter: hide payment-triggered balance changes — they are noisy
            // and already covered by the PAYMENT_COMPLETED / PAYMENT_RECEIVED toast.
            if (
                notification.type === "BALANCE_CHANGED" &&
                notification.message.toUpperCase().includes("PAYMENT")
            ) {
                // Still trigger a silent background refresh, but show no toast.
                fetchAccounts(true);
                return;
            }

            setNotifications((prev) => [...prev, notification]);

            // Trigger a single debounced background refresh for money-movement events.
            if (
                ["PAYMENT_COMPLETED", "PAYMENT_RECEIVED", "BALANCE_CHANGED"].includes(
                    notification.type
                ) &&
                !refreshScheduled.current
            ) {
                refreshScheduled.current = true;
                setTimeout(() => {
                    fetchAccounts(true);
                    refreshScheduled.current = false;
                }, 500);
            }
        },
        // fetchAccounts is stable (defined below with no deps that change)
        // eslint-disable-next-line react-hooks/exhaustive-deps
        []
    );

    // Mount: connect WebSocket
    useEffect(() => {
        const userData = localStorage.getItem("user");
        if (!userData) return;
        try {
            const parsed = JSON.parse(userData);
            const token = parsed.token;
            if (token) {
                websocketService.connect(token, addNotification);
            }
        } catch {
            console.error("Could not parse user token for WebSocket.");
        }
        return () => {
            websocketService.disconnect();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        fetchAccounts();
    }, []);

    const fetchAccounts = async (isBackgroundRefresh = false) => {
        if (!isAuthenticated()) {
            navigate("/login");
            return;
        }
        // On the initial load (accounts array is empty) show the full-page spinner.
        // On every subsequent background refresh (triggered by notifications),
        // only set `refreshing` so the page layout stays intact.
        if (isBackgroundRefresh) {
            setRefreshing(true);
        } else {
            setLoading(true);
        }
        setError("");
        try {
            const data = await getAllAccounts();
            const cur = getCurrentUser();
            if (Array.isArray(data)) {
                const userAccounts = data.filter(
                    (acc) => acc.accountHolderName === cur?.username
                );
                setAccounts(userAccounts);
                // Subscribe to each account's WebSocket topic
                userAccounts.forEach((acc) => websocketService.subscribeToAccount(acc.id));
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
                if (!isBackgroundRefresh) setAccounts([]);
            }
        } finally {
            if (isBackgroundRefresh) {
                setRefreshing(false);
            } else {
                setLoading(false);
            }
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
        websocketService.forceDisconnect();
        localStorage.removeItem("user");
        sessionStorage.clear();
        navigate("/");
    };

    const handleDismissNotification = (index: number) => {
        setNotifications((prev) => prev.filter((_, i) => i !== index));
    };

    // Payment queued: funnel through addNotification so it uses the SAME code path
    // as server-pushed events. This guarantees correct ordering.
    const handlePaymentQueued = useCallback(
        (message: string) => {
            addNotification({
                type: "PAYMENT_QUEUED",
                message,
                payment: null as any,
            });
        },
        [addNotification]
    );

    if (loading) {
        return (
            <div className="h-screen flex flex-col justify-center items-center gap-2.5 text-4xl">
                <div className="w-12 h-12 border-4 border-blue-500/15 border-t-blue-500 rounded-full animate-spin" />
                <p className="text-base text-slate-400">Loading accounts...</p>
            </div>
        );
    }

    return (
        <div className="px-6 py-6 max-w-[1400px] mx-auto min-h-screen bg-[#0a0a0a] text-white">
            {/* Real-time toast notifications */}
            <NotificationToast
                notifications={notifications}
                onDismiss={handleDismissNotification}
            />

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

            {/* Error banner */}
            {error && (
                <div className="bg-red-500/10 border border-red-500/30 text-red-300 px-5 py-4 rounded-xl mb-6 flex justify-between items-center animate-error-slide">
                    <span>⚠️ {error}</span>
                    <button
                        onClick={() => setError("")}
                        className="bg-transparent border-none text-red-300 text-2xl cursor-pointer w-7 h-7 flex items-center justify-center rounded-md hover:bg-red-500/20"
                    >
                        &times;
                    </button>
                </div>
            )}

            {/* Action buttons */}
            <div className="flex gap-3 mb-8">
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="btn btn-primary"
                >
                    + Create New Account
                </button>
                <button onClick={() => fetchAccounts()} className="btn btn-secondary">
                    ↺ Refresh
                </button>
            </div>

            <SummaryCards accounts={accounts} refreshing={refreshing} />

            <AccountsGrid
                accounts={accounts}
                refreshing={refreshing}
                onPay={(acc) => {
                    setPaymentAccount(acc);
                    setShowPaymentModal(true);
                }}
                onDelete={(id) => handleDeleteAccount(id)}
            />

            {/* Create Account Modal */}
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

            {/* Send Payment Modal */}
            <PaymentModal
                visible={showPaymentModal}
                sourceAccount={paymentAccount}
                onClose={() => {
                    setShowPaymentModal(false);
                    setPaymentAccount(null);
                }}
                onQueued={handlePaymentQueued}
            />

            {/* Payment History Modal */}
            {showHistory && historyAccount && (
                <PaymentHistory
                    account={historyAccount}
                    currentUserId={currentUser?.id ?? 0}
                    onClose={() => {
                        setShowHistory(false);
                        setHistoryAccount(null);
                    }}
                />
            )}
        </div>
    );
};

export default Dashboard;
