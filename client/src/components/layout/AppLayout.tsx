import React from 'react';
import Header from '../Header';

interface AppLayoutProps {
    children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
    return (
        <div className="min-h-screen flex flex-col bg-background text-text-main">
            <Header />
            <main className="flex-1 flex flex-col">
                {children}
            </main>
        </div>
    );
};
