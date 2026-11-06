import type {FormInstance} from 'antd';
import type {ReactNode} from 'react';

export type SearchFieldType =
    | 'input'
    | 'select'
    | 'datePicker'
    | 'rangePicker'
    | 'radio'
    | 'checkbox';

export interface SelectOption{
    value: string;
    label: string;
}

export interface SearchField {
    name: string;
    label: string;
    type: SearchFieldType;
    placeholder?: string;
    width?: number;
    options?: SelectOption[];       // select / radio / checkbox용
    allowClear?: boolean;           // select용
    defaultValue?: unknown;
}

export interface SearchFormProps{
    fields: SearchField[];
    onSearch: (values: Record<string, unknown>) => void;
    onExcel?: () => void;
    form?: FormInstance;
    loading?: boolean;
    extraButtons?: ReactNode;
}