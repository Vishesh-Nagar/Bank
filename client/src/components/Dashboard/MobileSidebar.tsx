import React from "react";

type Props = {
    username: string;
    onClose: () => void;
    onLogout: () => void;
};

const MobileSidebar: React.FC<Props> = ({ username, onClose, onLogout }) => {
    return (
        <div
            className="fixed inset-0 bg-black/70 z-[1000] flex justify-end"
            onClick={onClose}
        >
            <div
                className="bg-gradient-to-br from-[#1a1a1a] to-[#2d2d2d] w-[280px] h-screen p-6 shadow-[-4px_0_20px_rgba(0,0,0,0.5)] rounded-tl-2xl rounded-bl-2xl"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="flex flex-col gap-6">
                    <button
                        className="self-end bg-transparent border-none text-white text-2xl p-2 rounded-md cursor-pointer transition-all duration-300 hover:bg-white/10"
                        onClick={onClose}
                        aria-label="Close menu"
                    >
                        ✕
                    </button>
                    <span className="py-4 px-6 text-base text-slate-300 bg-black/40 rounded-xl text-center">
                        Welcome, {username}
                    </span>
                    <button onClick={onLogout} className="btn btn-secondary">
                        Logout
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MobileSidebar;
