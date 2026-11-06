import React from "react";
import type {ReactNode} from 'react';

interface PageTemplateProps{
    title: string;
    children: ReactNode;
}

const PageTemplate: React.FC<PageTemplateProps> = ({title, children}) => {
    return (
        <div>
            <div className="current-menu-title">{title}</div>
            {children}
        </div>
    );
};

export default PageTemplate;