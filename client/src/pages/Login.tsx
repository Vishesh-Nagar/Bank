import React, { useState } from "react";
import AuthForm from "../components/AuthForm";

const Login: React.FC = () => {
    const [mode, setMode] = useState<"login" | "signup">("login");

    const handleModeChange = (newMode: "login" | "signup") => {
        setMode(newMode);
    };

    return <AuthForm mode={mode} onModeChange={handleModeChange} />;
};

export default Login;
