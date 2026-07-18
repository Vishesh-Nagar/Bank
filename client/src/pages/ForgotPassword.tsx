import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import { AppLayout } from "../components/layout/AppLayout";
import { Card } from "../components/ui/Card";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";

const ForgotPassword: React.FC = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage(null);
        setError(null);
        setIsLoading(true);
        try {
            await api.post("/api/v1/users/forgot-password", { email });
            setMessage("If an account exists with that email, a password reset link has been sent.");
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to request password reset.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AppLayout>
            <div className="flex-1 flex flex-col justify-center items-center px-4 py-12 sm:px-6 lg:px-8">
                <div className="w-full max-w-md animate-modal-slide">
                    <Card padding="lg">
                        <div className="text-center mb-8">
                            <h2 className="text-2xl font-bold text-text-main">
                                Forgot Password
                            </h2>
                            <p className="mt-2 text-sm text-text-muted">
                                Enter your email address and we'll send you a link to reset your password.
                            </p>
                        </div>

                        {message && (
                            <div className="mb-6 p-4 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm">
                                {message}
                            </div>
                        )}
                        {error && (
                            <div className="mb-6 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
                                {error}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
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

                            <div className="pt-2">
                                <Button
                                    type="submit"
                                    fullWidth
                                    isLoading={isLoading}
                                    className="h-11"
                                >
                                    Send Reset Link
                                </Button>
                            </div>

                            <div className="text-center mt-4">
                                <button
                                    type="button"
                                    onClick={() => navigate("/login")}
                                    className="text-sm font-medium text-text-muted hover:text-white transition-colors focus:outline-none"
                                >
                                    Back to Login
                                </button>
                            </div>
                        </form>
                    </Card>
                </div>
            </div>
        </AppLayout>
    );
};

export default ForgotPassword;

