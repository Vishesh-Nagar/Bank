import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import api from "../services/api";
import { AppLayout } from "../components/layout/AppLayout";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";

const VerifyEmail: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
    const [message, setMessage] = useState<string | null>(null);

    useEffect(() => {
        const query = new URLSearchParams(location.search);
        const token = query.get("token");

        if (!token) {
            setStatus("error");
            setMessage("Invalid or missing verification token.");
            return;
        }

        const verify = async () => {
            try {
                await api.get(`/api/v1/users/verify-email?token=${token}`);
                setStatus("success");
                setMessage("Email verified successfully. You can now log in.");
            } catch (err: any) {
                setStatus("error");
                setMessage(err.response?.data?.message || "Verification failed. The token may be expired.");
            }
        };

        verify();
    }, [location]);

    return (
        <AppLayout>
            <div className="flex-1 flex flex-col justify-center items-center px-4 py-12 sm:px-6 lg:px-8">
                <div className="w-full max-w-md animate-modal-slide">
                    <Card padding="lg" className="text-center">
                        <h2 className="text-2xl font-bold text-text-main mb-6">
                            Email Verification
                        </h2>

                        {status === "loading" && (
                            <div className="flex justify-center my-8">
                                <svg className="animate-spin h-10 w-10 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                            </div>
                        )}

                        {status === "success" && (
                            <div className="mb-8 p-4 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                                {message}
                            </div>
                        )}
                        
                        {status === "error" && (
                            <div className="mb-8 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400">
                                {message}
                            </div>
                        )}

                        {status !== "loading" && (
                            <Button
                                onClick={() => navigate("/login")}
                                fullWidth
                                className="h-11"
                            >
                                Go to Login
                            </Button>
                        )}
                    </Card>
                </div>
            </div>
        </AppLayout>
    );
};

export default VerifyEmail;

