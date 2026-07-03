import React, { useState } from "react";
import { Container, Box, Paper, Typography, TextField, Button, Alert } from "@mui/material";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

const ForgotPassword: React.FC = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage(null);
        setError(null);
        try {
            await api.post("/api/v1/users/forgot-password", { email });
            setMessage("If an account exists with that email, a password reset link has been sent.");
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to request password reset.");
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
                        Forgot Password
                    </Typography>

                    {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}
                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                    <Box component="form" onSubmit={handleSubmit} noValidate>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="email"
                            label="Email Address"
                            name="email"
                            autoComplete="email"
                            autoFocus
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
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
                            sx={{
                                marginTop: 3,
                                marginBottom: 2,
                                backgroundColor: "#1976d2",
                                color: "#ffffff",
                                fontWeight: 600,
                                padding: "10px",
                                "&:hover": { backgroundColor: "#1565c0" },
                            }}
                        >
                            Send Reset Link
                        </Button>

                        <Button
                            fullWidth
                            onClick={() => navigate("/login")}
                            sx={{ color: "#aaaaaa", textTransform: "none" }}
                        >
                            Back to Login
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default ForgotPassword;
