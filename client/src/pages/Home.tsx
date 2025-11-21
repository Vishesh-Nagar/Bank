import React from "react";
import { Container, Box, Typography, Button } from "@mui/material";
import { useNavigate } from "react-router-dom";

const Home: React.FC = () => {
    const navigate = useNavigate();

    return (
        <Container maxWidth="md">
            <Box
                sx={{
                    minHeight: "calc(100vh - 64px)",
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    textAlign: "center",
                    paddingY: 4,
                }}
            >
                <Typography
                    variant="h2"
                    sx={{
                        fontWeight: 700,
                        marginBottom: 2,
                        color: "#ffffff",
                    }}
                >
                    Welcome to the Bank
                </Typography>
                <Typography
                    variant="h6"
                    sx={{
                        color: "#aaaaaa",
                        marginBottom: 4,
                        maxWidth: 600,
                    }}
                >
                    Manage your accounts securely and efficiently with our modern banking platform.
                </Typography>
                <Box sx={{ display: "flex", gap: 2 }}>
                    <Button
                        variant="contained"
                        size="large"
                        onClick={() => navigate("/login")}
                        sx={{
                            backgroundColor: "#1976d2",
                            color: "#ffffff",
                            fontWeight: 600,
                            padding: "12px 32px",
                            "&:hover": {
                                backgroundColor: "#1565c0",
                            },
                        }}
                    >
                        Login
                    </Button>
                    <Button
                        variant="outlined"
                        size="large"
                        onClick={() => navigate("/register")}
                        sx={{
                            borderColor: "#1976d2",
                            color: "#1976d2",
                            fontWeight: 600,
                            padding: "12px 32px",
                            "&:hover": {
                                borderColor: "#1565c0",
                                color: "#1565c0",
                            },
                        }}
                    >
                        Sign Up
                    </Button>
                </Box>
            </Box>
        </Container>
    );
};

export default Home;
