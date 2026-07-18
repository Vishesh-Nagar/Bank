import React from "react";
import { useNavigate } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { Button } from "../components/ui/Button";

const Home: React.FC = () => {
    const navigate = useNavigate();

    return (
        <AppLayout>
            <div className="flex-1 flex flex-col items-center justify-center text-center px-6 py-20 relative overflow-hidden">
                {/* Subtle background glow */}
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary/10 rounded-full blur-[100px] pointer-events-none -z-10" />

                <h1 className="text-5xl md:text-7xl font-extrabold text-white tracking-tight mb-6 animate-modal-slide">
                    Modern Banking, <br />
                    <span className="text-primary">Redefined.</span>
                </h1>
                
                <p className="text-lg md:text-xl text-text-muted max-w-2xl mb-12 animate-modal-slide" style={{ animationDelay: '0.1s', animationFillMode: 'both' }}>
                    Experience the future of finance with our secure, fast, and beautifully designed platform. Manage your wealth with absolute clarity.
                </p>
                
                <div className="flex flex-col sm:flex-row gap-4 animate-modal-slide" style={{ animationDelay: '0.2s', animationFillMode: 'both' }}>
                    <Button 
                        size="lg" 
                        onClick={() => navigate("/register")}
                        className="min-w-[160px]"
                    >
                        Get Started
                    </Button>
                    <Button 
                        variant="secondary" 
                        size="lg" 
                        onClick={() => navigate("/login")}
                        className="min-w-[160px]"
                    >
                        Sign In
                    </Button>
                </div>
            </div>
        </AppLayout>
    );
};

export default Home;

