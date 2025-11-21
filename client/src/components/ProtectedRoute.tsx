import React from "react";
import { Navigate } from "react-router-dom";
import { isAuthenticated } from "../services/userService";

interface ProtectedRouteProps {
    children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
    if (!isAuthenticated()) {
        // Redirect to login if not authenticated
        return <Navigate to="/login" replace />;
    }

    return <>{children}</>;
};

export default ProtectedRoute;
