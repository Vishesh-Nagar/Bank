import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../services/userService";
import { register } from "../services/userService";
import type { UserCreateDto } from "../types";
import {
    Container,
    TextField,
    Button,
    Typography,
    Box,
    Paper,
} from "@mui/material";

interface AuthFormProps {
    mode: "login" | "signup";
    onModeChange: (mode: "login" | "signup") => void;
}

const AuthForm: React.FC<AuthFormProps> = ({ mode, onModeChange }) => {
    const navigate = useNavigate();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [email, setEmail] = useState("");
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        try {
            if (mode === "login") {
                await login({ username, password });
                navigate("/dashboard");
            } else {
                const user: UserCreateDto = { username, password, email };
                await register(user);
                navigate("/dashboard");
            }
        } catch (err) {
            setError(
                mode === "login"
                    ? "Failed to login. Please try again."
                    : "Failed to register. Please try again."
            );
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
                    <Box
                        sx={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            marginBottom: 3,
                        }}
                    >
                        <Typography
                            component="h1"
                            variant="h4"
                            sx={{
                                fontWeight: 700,
                                color: "#ffffff",
                                textTransform: "capitalize",
                            }}
                        >
                            {mode === "login" ? "Login" : "Sign Up"}
                        </Typography>
                    </Box>

                    <Box component="form" onSubmit={handleSubmit} noValidate>
                        {mode === "signup" && (
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
                                        "& fieldset": {
                                            borderColor: "#444444",
                                        },
                                        "&:hover fieldset": {
                                            borderColor: "#666666",
                                        },
                                        "&.Mui-focused fieldset": {
                                            borderColor: "#1976d2",
                                        },
                                    },
                                    "& .MuiInputBase-input::placeholder": {
                                        color: "#999999",
                                        opacity: 1,
                                    },
                                    "& .MuiInputLabel-root": {
                                        color: "#aaaaaa",
                                        "&.Mui-focused": {
                                            color: "#1976d2",
                                        },
                                    },
                                }}
                            />
                        )}

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="username"
                            label="Username"
                            name="username"
                            autoComplete="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            sx={{
                                "& .MuiOutlinedInput-root": {
                                    color: "#ffffff",
                                    "& fieldset": {
                                        borderColor: "#444444",
                                    },
                                    "&:hover fieldset": {
                                        borderColor: "#666666",
                                    },
                                    "&.Mui-focused fieldset": {
                                        borderColor: "#1976d2",
                                    },
                                },
                                "& .MuiInputBase-input::placeholder": {
                                    color: "#999999",
                                    opacity: 1,
                                },
                                "& .MuiInputLabel-root": {
                                    color: "#aaaaaa",
                                    "&.Mui-focused": {
                                        color: "#1976d2",
                                    },
                                },
                            }}
                        />

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            name="password"
                            label="Password"
                            type="password"
                            id="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            sx={{
                                "& .MuiOutlinedInput-root": {
                                    color: "#ffffff",
                                    "& fieldset": {
                                        borderColor: "#444444",
                                    },
                                    "&:hover fieldset": {
                                        borderColor: "#666666",
                                    },
                                    "&.Mui-focused fieldset": {
                                        borderColor: "#1976d2",
                                    },
                                },
                                "& .MuiInputBase-input::placeholder": {
                                    color: "#999999",
                                    opacity: 1,
                                },
                                "& .MuiInputLabel-root": {
                                    color: "#aaaaaa",
                                    "&.Mui-focused": {
                                        color: "#1976d2",
                                    },
                                },
                            }}
                        />


                        {error && (
                            <Typography
                                color="error"
                                variant="body2"
                                sx={{ marginTop: 2 }}
                            >
                                {error}
                            </Typography>
                        )}

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
                                "&:hover": {
                                    backgroundColor: "#1565c0",
                                },
                            }}
                        >
                            {mode === "login" ? "Login" : "Sign Up"}
                        </Button>
                    </Box>

                    <Box
                        sx={{
                            textAlign: "center",
                            marginTop: 2,
                            borderTop: "1px solid #444444",
                            paddingTop: 2,
                        }}
                    >
                        <Typography variant="body2" sx={{ color: "#aaaaaa" }}>
                            {mode === "login" ? (
                                <>
                                    New here?{" "}
                                    <Button
                                        onClick={() => onModeChange("signup")}
                                        sx={{
                                            textTransform: "none",
                                            color: "#1976d2",
                                            padding: 0,
                                            fontWeight: 600,
                                            "&:hover": {
                                                textDecoration: "underline",
                                            },
                                        }}
                                    >
                                        Sign up
                                    </Button>
                                </>
                            ) : (
                                <>
                                    Already registered?{" "}
                                    <Button
                                        onClick={() => onModeChange("login")}
                                        sx={{
                                            textTransform: "none",
                                            color: "#1976d2",
                                            padding: 0,
                                            fontWeight: 600,
                                            "&:hover": {
                                                textDecoration: "underline",
                                            },
                                        }}
                                    >
                                        Login
                                    </Button>
                                </>
                            )}
                        </Typography>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default AuthForm;
