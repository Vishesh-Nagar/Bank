import React, { useState, useEffect } from "react";
import { Container, Box, Paper, Typography, TextField, Button, Alert } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import api from "../services/api";

const ResetPassword: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [newPassword, setNewPassword] = useState("");
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [token, setToken] = useState<string | null>(null);

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

        try {
            await api.post("/api/v1/users/reset-password", { token, newPassword });
            setMessage("Password reset successfully.");
            setTimeout(() => {
                navigate("/login", { state: { message: "Password reset successfully. Please login." } });
            }, 2000);
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to reset password.");
        }
    };

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
                    }}
                >
                    <Typography variant="h5" sx={{ color: "#ffffff", mb: 2, fontWeight: "bold" }}>
                        Reset Password
                    </Typography>

                    {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}
                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                    <Box component="form" onSubmit={handleSubmit} noValidate>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="newPassword"
                            label="New Password"
                            name="newPassword"
                            type="password"
                            autoFocus
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            disabled={!token || !!message}
                            sx={{
                                "& .MuiOutlinedInput-root": {
                                    color: "#ffffff",
                                    "& fieldset": { borderColor: "#444444" },
                                    "&:hover fieldset": { borderColor: "#666666" },
                                    "&.Mui-focused fieldset": { borderColor: "#1976d2" },
                                },
                                "& .MuiInputLabel-root": {
                                    color: "#aaaaaa",
                                    "&.Mui-focused": { color: "#1976d2" },
                                },
                            }}
                        />

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            disabled={!token || !!message}
                            sx={{
                                marginTop: 3,
                                marginBottom: 2,
                                backgroundColor: "#1976d2",
                                color: "#ffffff",
                                fontWeight: 600,
                                padding: "10px",
                                "&:hover": { backgroundColor: "#1565c0" },
                                "&.Mui-disabled": {
                                    backgroundColor: "#333333",
                                    color: "#777777",
                                }
                            }}
                        >
                            Reset Password
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default ResetPassword;
