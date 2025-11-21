import React, { useState } from "react";
import AuthForm from "../components/AuthForm";

const Register: React.FC = () => {
    const [mode, setMode] = useState<"login" | "signup">("signup");

    const handleModeChange = (newMode: "login" | "signup") => {
        setMode(newMode);
    };

    return <AuthForm mode={mode} onModeChange={handleModeChange} />;
};

export default Register;
