import React from "react";
import { useNavigate } from "react-router-dom";
import AuthForm from "../components/AuthForm";
import { AppLayout } from "../components/layout/AppLayout";

const Register: React.FC = () => {
    const navigate = useNavigate();

    const handleModeChange = (newMode: "login" | "signup") => {
        if (newMode === "login") {
            navigate("/login");
        }
    };

    return (
        <AppLayout>
            <AuthForm mode="signup" onModeChange={handleModeChange} />
        </AppLayout>
    );
};

export default Register;

