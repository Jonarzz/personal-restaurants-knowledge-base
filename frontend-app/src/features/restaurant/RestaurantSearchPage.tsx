import {CheckOutlined, CloseOutlined} from '@ant-design/icons';
import {Button, Card, Col, Form, Input, Popover, Row, Select, Switch, Table} from 'antd';
import React, {useState} from 'react';
import {Category, RestaurantData} from '../../api';
import {restaurantsApi} from '../../api/ApiFacade';
import './RestaurantSearchPage.css';

const prettifyCategoryName = (name: string) => {
  let output = name[0];
  for (let i = 1; i < name.length; i++) {
    let currentChar = name[i];
    if (currentChar === currentChar.toUpperCase()) {
      output += ' ' + currentChar.toLowerCase();
    } else {
      output += currentChar;
    }
  }
  return output;
};

type SearchQuery = {
  nameBeginsWith?: string,
  category?: Category,
  triedBefore?: boolean,
  ratingAtLeast?: number
};

const createPopover = (popoverContent: any) => (
  <Popover content={popoverContent}>
    <div style={{cursor: 'pointer'}}>Show</div>
  </Popover>
);

const TABLE_COLUMNS = [{
  title: 'Name',
  dataIndex: 'name',
}, {
  title: 'Categories',
  dataIndex: 'categories',
  render: (value: string[]) => value.map((category, index) => {
    for (let [name, value] of Object.entries(Category)) {
      if (category === value) {
        let categoryName = prettifyCategoryName(name);
        if (index > 0) {
          categoryName = categoryName.toLowerCase();
        }
        return categoryName;
      }
    }
    console.error('Unknown category: ' + category);
    return null;
  }).filter(e => e).join(', '),
}, {
  title: 'Tried before',
  dataIndex: 'triedBefore',
  render: (value: boolean) => value ? <CheckOutlined/> : <CloseOutlined/>,
}, {
  title: 'Rating',
  dataIndex: 'rating',
}, {
  title: 'Review',
  dataIndex: 'review',
  render: (value: string) => value ? createPopover(value) : null,
}, {
  title: 'Notes',
  dataIndex: 'notes',
  render: (value: string[]) => value?.length ? createPopover(<ul>{value.map(line => <li>{line}</li>)}</ul>) : null,
}];

export const RestaurantSearchPage = () => {

  const [restaurants, setRestaurants] = useState<RestaurantData[] | undefined>();

  const onFinish = ({category, nameBeginsWith, ratingAtLeast, triedBefore = false}: SearchQuery) => {
    restaurantsApi.queryRestaurantsByCriteria(nameBeginsWith, category, triedBefore, ratingAtLeast)
                  .then(({data}: { data: RestaurantData[] }) => setRestaurants(data));
  };

  return (
    <Card title="Find restaurants">
      <Form onFinish={onFinish}
            autoComplete="off"
      >
        <Row gutter={{xxl: 32, xl: 16, lg: 16, md: 8, sm: 8, xs: 4}}>
          <Col lg={5} md={4} sm={24} xs={24}>
            <Form.Item name="nameBeginsWith">
              <Input placeholder="Name"/>
            </Form.Item>
          </Col>

          <Col lg={5} md={4} sm={24} xs={24}>
            <Form.Item name="category">
              <Select placeholder="Category">
                {Object.entries(Category)
                       .sort()
                       .map(([name, value]) => <Select.Option value={value} key={value}>
                         {prettifyCategoryName(name)}
                       </Select.Option>)}
              </Select>
            </Form.Item>
          </Col>

          <Col lg={5} md={6} sm={24} xs={24}>
            <Form.Item name="ratingAtLeast">
              <Select placeholder="Rating at least">
                {Array(10).fill(1)
                          .map((x, y) => x + y)
                          .map(value => <Select.Option value={value} key={value}>
                            {value}
                          </Select.Option>)}
              </Select>
            </Form.Item>
          </Col>

          <Col lg={4} md={5} sm={24} xs={24}>
            <Form.Item name="triedBefore">
              <Switch checkedChildren="Tried before"
                      unCheckedChildren="Not tried before"
                      style={{width: '100%'}}/>
            </Form.Item>
          </Col>

          <Col lg={5}
               md={5}
               sm={24}
               xs={24}>
            <Form.Item>
              <Button type="primary" htmlType="submit" style={{width: '100%'}}>
                Submit
              </Button>
            </Form.Item>
          </Col>
        </Row>
      </Form>

      {restaurants && (
        <Table columns={TABLE_COLUMNS}
               dataSource={restaurants}
               pagination={{size: 'small'}}/>
      )}
    </Card>
  );
};