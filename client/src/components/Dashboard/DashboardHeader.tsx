import React from "react";

type Props = {
    onOpenSidebar: () => void;
    username: string;
    onLogout: () => void;
};

const DashboardHeader: React.FC<Props> = ({ onOpenSidebar, username, onLogout }) => {
    return (
        <header className="flex justify-between items-center mb-10 p-6 bg-gradient-to-br from-[#1e1e1e] to-[#2a2a2a] rounded-2xl shadow-[0_4px_20px_rgba(0,0,0,0.6)] border border-white/[0.06]">
            <h1 className="text-[32px] font-bold bg-gradient-to-br from-blue-500 to-purple-500 bg-clip-text text-transparent">
                Banking Dashboard
            </h1>

            {/* Hamburger — visible only on mobile */}
            <button
                className="md:hidden bg-transparent border-none text-white text-2xl cursor-pointer p-2 rounded-md transition-all duration-300 hover:bg-white/10"
                onClick={onOpenSidebar}
                aria-label="Open menu"
            >
                ☰
            </button>

            {/* Desktop nav — hidden on mobile */}
            <div className="hidden md:flex items-center gap-4">
                <span className="inline-flex items-center justify-center text-sm font-semibold text-slate-300 bg-black/40 px-6 py-3 rounded-xl min-h-[44px]">
                    Welcome, {username}
                </span>
                <div className="flex gap-3">
                    <button onClick={onLogout} className="btn btn-secondary">
                        Logout
                    </button>
                </div>
            </div>
        </header>
    );
};

export default DashboardHeader;
