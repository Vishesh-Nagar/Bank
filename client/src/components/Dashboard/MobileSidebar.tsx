import React from "react";
import "./MobileSidebar.css";

type Props = {
    username: string;
    onClose: () => void;
    onLogout: () => void;
};

const MobileSidebar: React.FC<Props> = ({ username, onClose, onLogout }) => {
    return (
        <div className="sidebar-overlay" onClick={onClose}>
            <div className="sidebar" onClick={(e) => e.stopPropagation()}>
                <div className="sidebar-content">
                    <button
                        className="close-sidebar"
                        onClick={onClose}
                        aria-label="Close menu"
                    >
                        ✕
                    </button>
                    <span className="username">Welcome, {username}</span>
                    <button onClick={onLogout} className="btn btn-secondary">
                        Logout
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MobileSidebar;
