import {Table, Typography} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';

interface DataTableProps<T> {
    columns: ColumnsType<T>;
    dataSource: T[];
    loading?: boolean;
    scrollX?: number;
    pageSize?: number;
    excelFileName?: string;
    /** false면 테이블 숨김 (조회 전) */
    visible?: boolean;
    /** 서버 페이징: 전체 건수. 지정 시 서버사이드 페이징 모드로 동작 */
    total?: number;
    /** 서버 페이징: 현재 페이지(1-based) */
    current?: number;
    /** 서버 페이징: 페이지/사이즈 변경 콜백 */
    onChange?: (page: number, pageSize: number) => void;
}

function DataTable<T extends Record<string, unknown>>({
    columns,
    dataSource,
    loading = false,
    scrollX = 1200,
    pageSize = 10,
    visible = true,
    total,
    current,
    onChange,
}: DataTableProps<T>){
    if(!visible) return null;

    const isServer = total !== undefined;

    const pagination: TablePaginationConfig = {
        pageSize,
        current: isServer ? current : undefined,
        total: isServer ? total : undefined,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50', '100'],
        showTotal: (t, range) => `${range[0]}-${range[1]} / 총 ${t}건`,
        onChange,
    };

    const count = isServer ? total : dataSource.length;

    return(
        <div>
            <div className='result-count'>
                <Typography.Text type="secondary">총 {count}건</Typography.Text>
            </div>
            <Table
                columns={columns}
                dataSource={dataSource}
                loading={loading}
                size='small'
                bordered
                scroll={{x: scrollX}}
                pagination={pagination}
                rowKey={(record) => (record.key as string) || (record.no as string) || Math.random().toString()}
            />
        </div>
    )
}

export default DataTable;
