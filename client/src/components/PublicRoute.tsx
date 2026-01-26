import React from "react";
import { Navigate } from "react-router-dom";
import { isAuthenticated } from "../services/userService";

interface PublicRouteProps {
    children: React.ReactNode;
}

const PublicRoute: React.FC<PublicRouteProps> = ({ children }) => {
    if (isAuthenticated()) {
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
};

export default PublicRoute;
