import { Link } from "react-router-dom";
import { AppBar, Toolbar, Typography, Button } from "@mui/material";

function Header() {
    return (
        <AppBar
            position="static"
            sx={{
                backgroundColor: "#1a1a1a",
                borderBottom: "1px solid #333333",
            }}
        >
            <Toolbar>
                <Typography
                    variant="h6"
                    component="div"
                    sx={{
                        flexGrow: 1,
                        fontWeight: 700,
                        letterSpacing: "0.5px",
                    }}
                >
                    <Link
                        to="/"
                        style={{
                            textDecoration: "none",
                            color: "#ffffff",
                        }}
                    >
                        Bank
                    </Link>
                </Typography>
                <Button
                    color="inherit"
                    component={Link}
                    to="/login"
                    sx={{
                        color: "#ffffff",
                        marginRight: 1,
                        "&:hover": {
                            backgroundColor: "rgba(255, 255, 255, 0.1)",
                        },
                    }}
                >
                    Login
                </Button>
                <Button
                    color="inherit"
                    component={Link}
                    to="/register"
                    sx={{
                        color: "#ffffff",
                        "&:hover": {
                            backgroundColor: "rgba(255, 255, 255, 0.1)",
                        },
                    }}
                >
                    Register
                </Button>
            </Toolbar>
        </AppBar>
    );
}

export default Header;
