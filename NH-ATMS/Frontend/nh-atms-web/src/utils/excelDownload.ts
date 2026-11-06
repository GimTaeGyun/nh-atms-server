import * as XLSX from 'xlsx';
import {saveAs} from 'file-saver';
import dayjs from 'dayjs';

interface ExcelColumn{
    title: string;
    dataIndex: string;
}

/**
 * 
 * @param column 컬럼 정의
 * @param data 테이블 데이터 배열
 * @param fileName 파일명 (확장자 제외)
 */
export const downloadExcel = (
    columns: ExcelColumn[],
    data: Record<string, unknown>[],
    fileName: string
) => {
    //헤더
    const headers = columns.map((col) => col.title);

    //데이터 행 변환
    const rows = data.map((row) => 
        columns.map((col) => row[col.dataIndex] ?? '')
    )

    const worksheet = XLSX.utils.aoa_to_sheet([headers, ...rows]);

    //컬럼 너비 자동 조정
    worksheet['!cols'] = columns.map((col) => ({
        wch: Math.max(col.title.length * 2, 12)
    }));

    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');

    const excelBuffer = XLSX.write(workbook, {bookType: 'xlsx', type: 'array'});
    const blob = new Blob([excelBuffer], {type: 'application/octet-stream'});

    const dateStr = dayjs().format('YYYYMMDD');
    saveAs(blob, `${fileName}_${dateStr}.xlsx`);
    
};