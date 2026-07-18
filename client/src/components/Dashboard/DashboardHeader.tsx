import React from "react";
import { Button } from "../ui/Button";

type Props = {
    onOpenSidebar: () => void;
    username: string;
    onLogout: () => void;
};

const DashboardHeader: React.FC<Props> = ({ onOpenSidebar, username, onLogout }) => {
    return (
        <header className="flex justify-between items-center mb-8 p-6 bg-surface rounded-2xl shadow-xl border border-white/5">
            <h1 className="text-3xl font-bold text-white tracking-tight">
                Banking <span className="text-primary">Dashboard</span>
            </h1>

            {/* Hamburger — visible only on mobile */}
            <button
                className="md:hidden bg-transparent border-none text-text-muted hover:text-text-main text-2xl cursor-pointer p-2 rounded-md transition-all duration-200"
                onClick={onOpenSidebar}
                aria-label="Open menu"
            >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
            </button>

            {/* Desktop nav — hidden on mobile */}
            <div className="hidden md:flex items-center gap-6">
                <span className="text-sm font-medium text-text-muted">
                    Welcome, <strong className="text-text-main">{username}</strong>
                </span>
                <Button variant="ghost" onClick={onLogout} size="sm">
                    Logout
                </Button>
            </div>
        </header>
    );
};

export default DashboardHeader;

