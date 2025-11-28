import React from "react";
import "./DashboardHeader.css";

type Props = {
    onOpenSidebar: () => void;
    username: string;
    onLogout: () => void;
};

const DashboardHeader: React.FC<Props> = ({ onOpenSidebar, username, onLogout }) => {
    return (
        <header className="dashboard-header">
            <button
                className="hamburger-menu"
                onClick={onOpenSidebar}
                aria-label="Open menu"
            >
                ☰
            </button>
            <h1>Banking Dashboard</h1>
            <div className="header-right">
                <span className="username">Welcome, {username}</span>
                <div className="header-buttons">
                    <button onClick={onLogout} className="btn btn-secondary">
                        Logout
                    </button>
                </div>
            </div>
        </header>
    );
};

export default DashboardHeader;
