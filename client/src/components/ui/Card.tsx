import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
    hoverable?: boolean;
    padding?: 'none' | 'sm' | 'md' | 'lg';
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(({
    children,
    hoverable = false,
    padding = 'md',
    className = '',
    ...props
}, ref) => {
    const paddings = {
        none: '',
        sm: 'p-4',
        md: 'p-6',
        lg: 'p-8',
    };

    return (
        <div
            ref={ref}
            className={`
                bg-surface rounded-xl border border-white/5 shadow-xl
                ${hoverable ? 'transition-all duration-300 hover:shadow-2xl hover:border-white/10 hover:-translate-y-1' : ''}
                ${paddings[padding]}
                ${className}
            `}
            {...props}
        >
            {children}
        </div>
    );
});

Card.displayName = 'Card';
