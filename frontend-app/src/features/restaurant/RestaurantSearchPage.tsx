import {CheckOutlined, CloseOutlined} from '@ant-design/icons';
import {Button, Card, Col, Form, Input, Modal, Popover, Row, Select, Switch, Table} from 'antd';
import {Breakpoint} from 'antd/es/_util/responsiveObserve';
import React, {useEffect, useState} from 'react';
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

const CATEGORY_SELECT_OPTIONS = Object.entries(Category)
                                      .sort()
                                      .map(([name, value]) => (
                                        <Select.Option value={value} key={value}>
                                          {prettifyCategoryName(name)}
                                        </Select.Option>
                                      ));

const RATING_SELECT_OPTIONS = Array(10).fill(1)
                                       .map((x, y) => x + y)
                                       .map(value => (
                                         <Select.Option value={value} key={value}>
                                           {value}
                                         </Select.Option>
                                       ));

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

// TODO delete after dev
const deleteMe = [{
  'name': 'Super Tasty Burger',
  'categories': ['BURGER', 'SANDWICH'],
  'triedBefore': false,
}, {
  'name': 'Burger King City Centre',
  'categories': ['BURGER', 'FAST_FOOD'],
  'triedBefore': true,
  'rating': 5,
  'review': 'Not the greatest burger, but it\'s all right for a fast food I guess',
  'notes': [
    'Whooper has plenty of white onion',
    'Double cheeseburger is OK for the price',
    'Fries can be great, but can also be mediocre - it\'s a lottery',
  ],
}];

export const RestaurantSearchPage = () => {

  const [restaurants, setRestaurants] = useState<RestaurantData[] | undefined>(JSON.parse(JSON.stringify(deleteMe)));
  const [modalRestaurant, setModalRestaurant] = useState<RestaurantData | undefined>();
  const [triedBeforeInModal, setTriedBeforeInModal] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    if (!modalRestaurant) {
      return;
    }
    setTriedBeforeInModal(modalRestaurant.triedBefore || false);
  }, [modalRestaurant]);

  const onFinish = ({nameBeginsWith, category, ratingAtLeast, triedBefore = false}: SearchQuery) => {
    restaurantsApi.queryRestaurantsByCriteria(nameBeginsWith, category, triedBefore, ratingAtLeast)
                  .then(({data}: { data: RestaurantData[] }) => setRestaurants(data));
  };

  const tableColumns = [{
    title: 'Name',
    dataIndex: 'name',
    render: (value: string, restaurant: RestaurantData) => <Button type="link" onClick={() => {
      setModalRestaurant(restaurant);
      setModalOpen(true);
    }}>{value}</Button>,
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
    responsive: ['md' as Breakpoint],
  }, {
    title: 'Rating',
    dataIndex: 'rating',
    responsive: ['sm' as Breakpoint],
  }, {
    title: 'Review',
    dataIndex: 'review',
    render: (value: string) => value ? createPopover(value) : null,
    responsive: ['md' as Breakpoint],
  }, {
    title: 'Notes',
    dataIndex: 'notes',
    render: (value: string[]) => value?.length ? createPopover(<ul>{value.map(line => <li>{line}</li>)}</ul>) : null,
    responsive: ['md' as Breakpoint],
  }];

  const [modalForm] = Form.useForm(); // TODO po wyniesieniu do oddzielnego komponentu tworzyc przy kazdym otwarciu modala

  return (
    <>
      <Card title="Find restaurants">
        <Form onFinish={onFinish}
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
          <Table columns={tableColumns}
                 dataSource={restaurants}
                 pagination={{size: 'small'}}/>
        )}
      </Card>

      {modalRestaurant && (
        <Modal title={modalRestaurant.name}
               okText="Update"
               open={modalOpen}
               onOk={() => {
                 modalForm.submit();
                 setModalRestaurant(undefined);
                 setModalOpen(false);
               }}
               onCancel={() => {
                 setModalRestaurant(undefined);
                 setModalOpen(false);
               }}>
          <Form form={modalForm}
                onFinish={values => console.log('Saving', values)}
                onValuesChange={(changedFields: any) => {
                  if (!changedFields.hasOwnProperty('triedBefore')) {
                    return;
                  }
                  setTriedBeforeInModal(changedFields.triedBefore);
                }}
                autoComplete="off">
            <Form.Item name="name" initialValue={modalRestaurant.name}>
              <Input placeholder="Name"/>
            </Form.Item>
            <Form.Item name="categories" initialValue={modalRestaurant.categories}>
              <Select mode="multiple"
                      placeholder="Categories">
                {CATEGORY_SELECT_OPTIONS}
              </Select>
            </Form.Item>
            <Form.Item name="triedBefore" initialValue={modalRestaurant.triedBefore}>
              <Switch checkedChildren="Tried before"
                      unCheckedChildren="Not tried before"
                      defaultChecked={modalRestaurant.triedBefore}
                      style={{width: '100%'}}/>
            </Form.Item>
            {/* TODO wartosci pozostaja pomiedzy otwarciami modala */}
            <Form.Item name="rating" initialValue={modalRestaurant.rating}>
              <Select placeholder="Rating" disabled={!triedBeforeInModal}>
                {RATING_SELECT_OPTIONS}
              </Select>
            </Form.Item>
            <Form.Item name="review" initialValue={modalRestaurant.review}>
              <Input.TextArea placeholder="Review" disabled={!triedBeforeInModal}/>
            </Form.Item>
            <Form.Item name="notes" initialValue={modalRestaurant.notes?.join('\n')}>
              <Input.TextArea placeholder="Notes (separated with newlines)" autoSize/>
            </Form.Item>
          </Form>
        </Modal>
      )}
    </>
  );
};