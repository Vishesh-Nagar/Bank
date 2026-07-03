import React, { useEffect, useState } from "react";
import { Container, Box, Paper, Typography, Button, Alert, CircularProgress } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import api from "../services/api";

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
        <Container maxWidth="sm">
            <Box
                sx={{
                    minHeight: "100vh",
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    paddingY: 2,
                }}
            >
                <Paper
                    elevation={6}
                    sx={{
                        padding: 4,
                        width: "100%",
                        backgroundColor: "#1e1e1e",
                        borderRadius: 2,
                        textAlign: "center",
                    }}
                >
                    <Typography variant="h5" sx={{ color: "#ffffff", mb: 2, fontWeight: "bold" }}>
                        Email Verification
                    </Typography>

                    {status === "loading" && <CircularProgress sx={{ my: 3 }} />}
                    {status === "success" && <Alert severity="success" sx={{ mb: 3 }}>{message}</Alert>}
                    {status === "error" && <Alert severity="error" sx={{ mb: 3 }}>{message}</Alert>}

                    {status !== "loading" && (
                        <Button
                            variant="contained"
                            onClick={() => navigate("/login")}
                            sx={{
                                backgroundColor: "#1976d2",
                                color: "#ffffff",
                                "&:hover": { backgroundColor: "#1565c0" },
                            }}
                        >
                            Go to Login
                        </Button>
                    )}
                </Paper>
            </Box>
        </Container>
    );
};

export default VerifyEmail;
