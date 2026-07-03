import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import type { ReactNode } from 'react';
import { Snackbar, Alert } from '@mui/material';
import type { AlertColor } from '@mui/material';

// Global toast dispatcher for use outside React components (like Axios interceptors)
export const globalToast = {
    show: (msg: string, type: AlertColor = 'error') => {
        // This will be overridden when the provider mounts
        console.warn('globalToast called before provider mounted', msg, type);
    }
};

interface ToastContextProps {
    showToast: (message: string, severity?: AlertColor) => void;
}

const ToastContext = createContext<ToastContextProps | undefined>(undefined);

export const useToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useToast must be used within a ToastProvider');
    }
    return context;
};

interface ToastProviderProps {
    children: ReactNode;
}

export const ToastProvider: React.FC<ToastProviderProps> = ({ children }) => {
    const [open, setOpen] = useState(false);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<AlertColor>('error');

    const showToast = useCallback((msg: string, type: AlertColor = 'error') => {
        setMessage(msg);
        setSeverity(type);
        setOpen(true);
    }, []);

    useEffect(() => {
        // Bind the global toast
        globalToast.show = showToast;
    }, [showToast]);

    const handleClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        setOpen(false);
    };

    return (
        <ToastContext.Provider value={{ showToast }}>
            {children}
            <Snackbar
                open={open}
                autoHideDuration={6000}
                onClose={handleClose}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert onClose={handleClose} severity={severity} sx={{ width: '100%', fontSize: '1rem', fontWeight: 500 }}>
                    {message}
                </Alert>
            </Snackbar>
        </ToastContext.Provider>
    );
};
