import {Button, Col, Form, Input, Row, Select, Switch} from 'antd';
import React, {useEffect} from 'react';
import {CATEGORY_SELECT_OPTIONS, RATING_SELECT_OPTIONS} from './common';

type Props = {
  onSubmit: (values: any) => void,
  resetTrigger?: any
};

export const RestaurantsSearchForm = ({onSubmit, resetTrigger}: Props) => {

  const [form] = Form.useForm();

  useEffect(() => form.resetFields(), [form, resetTrigger]);

  return (
    <Form onFinish={onSubmit}
          form={form}
          autoComplete="off">
      <Row gutter={{xxl: 32, xl: 16, lg: 16, md: 8, sm: 8, xs: 4}}>

        <Col lg={5} md={4} sm={24} xs={24}>
          <Form.Item name="nameBeginsWith">
            <Input placeholder="Name"/>
          </Form.Item>
        </Col>

        <Col lg={5} md={4} sm={24} xs={24}>
          <Form.Item name="category">
            <Select placeholder="Category">
              {CATEGORY_SELECT_OPTIONS}
            </Select>
          </Form.Item>
        </Col>

        <Col lg={5} md={6} sm={24} xs={24}>
          <Form.Item name="ratingAtLeast">
            <Select placeholder="Rating at least">
              {RATING_SELECT_OPTIONS}
            </Select>
          </Form.Item>
        </Col>

        <Col lg={4} md={5} sm={24} xs={24}>
          <Form.Item name="triedBefore" initialValue={false}>
            <Switch checkedChildren="Tried before"
                    unCheckedChildren="Not tried before"
                    style={{width: '100%'}}/>
          </Form.Item>
        </Col>

        <Col lg={5} md={5} sm={24} xs={24}>
          <Form.Item>
            <Button type="primary" htmlType="submit" style={{width: '100%'}}>
              Search
            </Button>
          </Form.Item>
        </Col>

      </Row>
    </Form>
  );
};