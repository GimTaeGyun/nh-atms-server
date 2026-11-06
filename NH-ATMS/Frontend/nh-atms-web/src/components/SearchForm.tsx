import React from 'react';
import {Form, Input, Select, DatePicker, Button, Space, Radio, Checkbox} from 'antd';
import {SearchOutlined, FileExcelOutlined} from '@ant-design/icons';
import type { SearchFormProps, SearchField } from '@/types/search';

const {RangePicker} = DatePicker;

const renderField = (field: SearchField) => {
    const width = field.width || 160;

    switch(field.type){
        case 'input':
            return <Input size='small' placeholder={field.placeholder} style={{width}}/>
        case 'select':
            return (
                <Select
                    size='small'
                    placeholder={field.placeholder || '전체'}
                    style={{width}}
                    allowClear={field.allowClear !== false}
                    options={field.options}
                />
            );
        case 'datePicker':
            return <DatePicker size='small' style={{width}}/>
        case 'rangePicker':
            return <RangePicker size='small'/>
        case 'radio':
            return (
                <Radio.Group size='small' optionType='button' buttonStyle='solid' options={field.options}/>
            );
        case 'checkbox':
            return <Checkbox.Group options={field.options}/>
        default:
            return <Input size='small' style={{width}}/>
    }
}

const SearchForm: React.FC<SearchFormProps> = ({
    fields,
    onSearch,
    onExcel,
    form: externalForm,
    loading = false,
    extraButtons
}) => {
    const [internalForm] = Form.useForm();
    const form = externalForm || internalForm;

    const handleFinish = (values: Record<string, unknown>) => {
        onSearch(values);
    };

   return (
    <div className="search-panel">
      <Form form={form} layout="inline" onFinish={handleFinish} style={{ gap: 8, flexWrap: 'wrap' }}>
        {fields.map((field) => (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            initialValue={field.defaultValue}
          >
            {renderField(field)}
          </Form.Item>
        ))}
        <Form.Item>
          <Space>
            <Button
              type="primary"
              size="small"
              icon={<SearchOutlined />}
              htmlType="submit"
              loading={loading}
            >
              조회
            </Button>
            {onExcel && (
              <Button
                size="small"
                icon={<FileExcelOutlined />}
                onClick={onExcel}
                style={{ background: '#217346', color: '#fff', border: 'none' }}
              >
                엑셀
              </Button>
            )}
            {extraButtons}
          </Space>
        </Form.Item>
      </Form>
    </div>
  );
};

export default SearchForm;

