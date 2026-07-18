import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { login, register } from "../services/userService";
import type { UserCreateDto } from "../types";
import { Button } from "./ui/Button";
import { Input } from "./ui/Input";
import { Card } from "./ui/Card";

interface AuthFormProps {
    mode: "login" | "signup";
    onModeChange: (mode: "login" | "signup") => void;
}

const AuthForm: React.FC<AuthFormProps> = ({ mode, onModeChange }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [email, setEmail] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (location.state?.message) {
            setSuccessMessage(location.state.message);
        }
    }, [location.state]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setIsLoading(true);

        try {
            if (mode === "login") {
                await login({ username, password });
                navigate("/dashboard");
            } else {
                const user: UserCreateDto = { username, password, email };
                await register(user);
                navigate("/login", { state: { message: "Registration successful. Please check your email to verify your account before logging in." } });
                onModeChange("login");
            }
        } catch (err: any) {
            const defaultMessage =
                mode === "login"
                    ? "Failed to login. Please try again."
                    : "Failed to register. Please try again.";
            setError(err.response?.data?.message || err.message || defaultMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex-1 flex flex-col justify-center items-center px-4 py-12 sm:px-6 lg:px-8">
            <div className="w-full max-w-md animate-modal-slide">
                <Card padding="lg">
                    <div className="text-center mb-8">
                        <h2 className="text-3xl font-extrabold text-text-main capitalize">
                            {mode === "login" ? "Welcome Back" : "Create Account"}
                        </h2>
                        <p className="mt-2 text-sm text-text-muted">
                            {mode === "login" 
                                ? "Enter your credentials to access your account" 
                                : "Sign up to start managing your finances"}
                        </p>
                    </div>

                    {successMessage && (
                        <div className="mb-6 p-4 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm">
                            {successMessage}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-5" noValidate>
                        {mode === "signup" && (
                            <Input
                                id="email"
                                name="email"
                                type="email"
                                label="Email Address"
                                autoComplete="email"
                                autoFocus
                                required
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        )}

                        <Input
                            id="username"
                            name="username"
                            label="Username"
                            autoComplete="username"
                            required
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                        />

                        <Input
                            id="password"
                            name="password"
                            type="password"
                            label="Password"
                            autoComplete="current-password"
                            required
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />

                        {error && (
                            <div className="text-sm text-red-400">
                                {error}
                            </div>
                        )}

                        {mode === "login" && (
                            <div className="flex items-center justify-end">
                                <button
                                    type="button"
                                    onClick={() => navigate("/forgot-password")}
                                    className="text-sm font-medium text-text-muted hover:text-primary transition-colors"
                                >
                                    Forgot your password?
                                </button>
                            </div>
                        )}

                        <div className="pt-2">
                            <Button
                                type="submit"
                                fullWidth
                                isLoading={isLoading}
                                className="h-11"
                            >
                                {mode === "login" ? "Sign In" : "Sign Up"}
                            </Button>
                        </div>
                    </form>

                    <div className="mt-8 pt-6 border-t border-white/10 text-center">
                        <p className="text-sm text-text-muted">
                            {mode === "login" ? "New here? " : "Already registered? "}
                            <button
                                type="button"
                                onClick={() => onModeChange(mode === "login" ? "signup" : "login")}
                                className="font-semibold text-primary hover:text-primary-hover transition-colors focus:outline-none"
                            >
                                {mode === "login" ? "Sign up for an account" : "Sign in instead"}
                            </button>
                        </p>
                    </div>
                </Card>
            </div>
        </div>
    );
};

export default AuthForm;
