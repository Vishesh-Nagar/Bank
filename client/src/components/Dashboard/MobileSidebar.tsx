import React from "react";
import { Button } from "../ui/Button";

type Props = {
    username: string;
    onClose: () => void;
    onLogout: () => void;
};

const MobileSidebar: React.FC<Props> = ({ username, onClose, onLogout }) => {
    return (
        <div
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[1000] flex justify-end animate-modal-fade"
            onClick={onClose}
        >
            <div
                className="bg-surface w-[280px] h-screen p-6 shadow-2xl border-l border-white/5 flex flex-col"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="flex items-center justify-between mb-8">
                    <h2 className="text-xl font-bold text-white tracking-tight">
                        Banking <span className="text-primary">App</span>
                    </h2>
                    <button
                        className="bg-transparent border-none text-text-muted hover:text-white text-2xl p-2 rounded-md cursor-pointer transition-colors"
                        onClick={onClose}
                        aria-label="Close menu"
                    >
                        ✕
                    </button>
                </div>
                
                <div className="flex flex-col gap-6 mt-4 flex-1">
                    <div className="py-4 px-6 text-sm text-text-main bg-background rounded-xl border border-white/5 text-center">
                        Welcome, <strong className="text-white">{username}</strong>
                    </div>
                </div>

                <div className="mt-auto">
                    <Button variant="secondary" onClick={onLogout} fullWidth>
                        Logout
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default MobileSidebar;

