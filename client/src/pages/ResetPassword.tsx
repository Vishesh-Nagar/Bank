import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import api from "../services/api";
import { AppLayout } from "../components/layout/AppLayout";
import { Card } from "../components/ui/Card";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";

const ResetPassword: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [newPassword, setNewPassword] = useState("");
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        const query = new URLSearchParams(location.search);
        const t = query.get("token");
        if (t) {
            setToken(t);
        } else {
            setError("Invalid password reset link.");
        }
    }, [location]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage(null);
        setError(null);

        if (!token) {
            setError("Missing token.");
            return;
        }

        setIsLoading(true);
        try {
            await api.post("/api/v1/users/reset-password", { token, newPassword });
            setMessage("Password reset successfully.");
            setTimeout(() => {
                navigate("/login", { state: { message: "Password reset successfully. Please login." } });
            }, 2000);
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to reset password.");
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
                                Reset Password
                            </h2>
                            <p className="mt-2 text-sm text-text-muted">
                                Please enter your new password below.
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
                                id="newPassword"
                                name="newPassword"
                                type="password"
                                label="New Password"
                                autoComplete="new-password"
                                autoFocus
                                required
                                disabled={!token || !!message}
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                            />

                            <div className="pt-2">
                                <Button
                                    type="submit"
                                    fullWidth
                                    isLoading={isLoading}
                                    disabled={!token || !!message}
                                    className="h-11"
                                >
                                    Reset Password
                                </Button>
                            </div>
                        </form>
                    </Card>
                </div>
            </div>
        </AppLayout>
    );
};

export default ResetPassword;

