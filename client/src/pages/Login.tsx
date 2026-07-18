import React from "react";
import { useNavigate } from "react-router-dom";
import AuthForm from "../components/AuthForm";
import { AppLayout } from "../components/layout/AppLayout";

const Login: React.FC = () => {
    const navigate = useNavigate();

    const handleModeChange = (newMode: "login" | "signup") => {
        if (newMode === "signup") {
            navigate("/register");
        }
    };

    return (
        <AppLayout>
            <AuthForm mode="login" onModeChange={handleModeChange} />
        </AppLayout>
    );
};

export default Login;

