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
import ScheduledPaymentModal from "../components/Dashboard/modals/ScheduledPaymentModal";
import PaymentHistory from "../components/Dashboard/PaymentHistory";
import NotificationToast from "../components/Notifications/NotificationToast";
import ActivePaymentsSection from "../components/Dashboard/ActivePaymentsSection";
import { Button } from "../components/ui/Button";

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState<AccountDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [refreshTrigger, setRefreshTrigger] = useState(0);
    const [error, setError] = useState<string>("");
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Payment state
    const [paymentAccount, setPaymentAccount] = useState<AccountDto | null>(null);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [historyAccount, setHistoryAccount] = useState<AccountDto | null>(null);
    const [showHistory, setShowHistory] = useState(false);
    
    // Scheduled payment state
    const [scheduleAccount, setScheduleAccount] = useState<AccountDto | null>(null);
    const [showScheduleModal, setShowScheduleModal] = useState(false);

    // Notification state
    const [notifications, setNotifications] = useState<NotificationDto[]>([]);

    const currentUser = getCurrentUser();
    const [newAccount, setNewAccount] = useState<AccountCreateDto>({
        accountHolderName: currentUser?.username || "",
        balance: 0,
        accountType: "SAVINGS",
    });
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    const addNotification = useCallback(
        (notification: NotificationDto) => {
            if (notification.type === "BALANCE_CHANGED") {
                fetchAccounts(true);
                return;
            }
            setNotifications((prev) => [...prev, notification]);
            if (notification.type !== "PAYMENT_QUEUED") {
                fetchAccounts(true);
            }
        },
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
                setRefreshTrigger(prev => prev + 1);
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
            fetchAccounts(true);
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [addNotification]
    );

    if (loading) {
        return (
            <div className="h-screen flex flex-col justify-center items-center gap-4 text-4xl">
                <svg className="animate-spin h-12 w-12 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <p className="text-base text-text-muted">Loading accounts...</p>
            </div>
        );
    }

    return (
        <div className="px-6 py-6 max-w-[1400px] mx-auto min-h-screen bg-background text-text-main">
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
                <div className="bg-red-500/10 border border-red-500/20 text-red-400 px-5 py-4 rounded-xl mb-6 flex justify-between items-center animate-modal-slide">
                    <span>⚠️ {error}</span>
                    <button
                        onClick={() => setError("")}
                        className="bg-transparent border-none text-red-400 text-2xl cursor-pointer w-7 h-7 flex items-center justify-center rounded-md hover:bg-red-500/20 transition-colors"
                    >
                        &times;
                    </button>
                </div>
            )}

            {/* Action buttons */}
            <div className="flex gap-3 mb-8">
                <Button
                    onClick={() => setShowCreateModal(true)}
                >
                    + Create New Account
                </Button>
                <Button onClick={() => fetchAccounts()} variant="secondary">
                    ↺ Refresh
                </Button>
            </div>

            <SummaryCards accounts={accounts} refreshing={refreshing} />

            <ActivePaymentsSection accounts={accounts} refreshTrigger={refreshTrigger} />

            <AccountsGrid
                accounts={accounts}
                refreshing={refreshing}
                onPay={(acc) => {
                    setPaymentAccount(acc);
                    setShowPaymentModal(true);
                }}
                onSchedule={(acc) => {
                    setScheduleAccount(acc);
                    setShowScheduleModal(true);
                }}
                onDelete={(id) => handleDeleteAccount(id)}
                onHistory={(acc) => {
                    setHistoryAccount(acc);
                    setShowHistory(true);
                }}
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

            {/* Scheduled Payment Modal */}
            <ScheduledPaymentModal
                visible={showScheduleModal}
                sourceAccount={scheduleAccount}
                onClose={() => {
                    setShowScheduleModal(false);
                    setScheduleAccount(null);
                }}
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
