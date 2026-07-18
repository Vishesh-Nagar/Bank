import React from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label?: string;
    error?: string;
    helperText?: string;
    fullWidth?: boolean;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(({
    label,
    error,
    helperText,
    fullWidth = true,
    className = '',
    id,
    ...props
}, ref) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);
    const widthStyle = fullWidth ? 'w-full' : '';
    
    return (
        <div className={`flex flex-col gap-1.5 ${widthStyle} ${className}`}>
            {label && (
                <label htmlFor={inputId} className="text-sm font-medium text-text-muted">
                    {label}
                </label>
            )}
            <div className="relative">
                <input
                    ref={ref}
                    id={inputId}
                    className={`
                        block w-full rounded-lg px-4 py-2.5 
                        bg-surface/50 border transition-colors duration-200
                        text-text-main placeholder-text-muted/50
                        focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent
                        disabled:opacity-50 disabled:cursor-not-allowed
                        ${error ? 'border-red-500/50 focus:ring-red-500' : 'border-white/10 hover:border-white/20'}
                    `}
                    {...props}
                />
            </div>
            {(error || helperText) && (
                <p className={`text-xs ${error ? 'text-red-400' : 'text-text-muted'}`}>
                    {error || helperText}
                </p>
            )}
        </div>
    );
});

Input.displayName = 'Input';
