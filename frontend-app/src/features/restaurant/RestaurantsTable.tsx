import {CheckOutlined, CloseOutlined} from '@ant-design/icons';
import {Button, Popover, Table} from 'antd';
import {Breakpoint} from 'antd/es/_util/responsiveObserve';
import React, {useCallback, useMemo} from 'react';
import {Category, RestaurantData} from '../../api';
import {prettifyCategoryName} from './common';

type Props = {
  restaurants?: RestaurantData[],
  openRestaurantDetails: Function
};

const renderCategories = (value: string[]) => value.map((category, index) => {
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
}).filter(e => e).join(', ');

const createPopover = (popoverContent: any) => (
  <Popover content={popoverContent}>
    <div style={{cursor: 'pointer'}}>Show</div>
  </Popover>
);

const renderNotes = (value: string[]) => value?.length
                                         ? createPopover(<ul>{
                                           value.map(line => <li>{line}</li>)
                                         }</ul>)
                                         : null;

export const RestaurantsTable = ({restaurants, openRestaurantDetails}: Props) => {

  const renderName = useCallback((value: string, restaurant: RestaurantData) => (
    <Button type="link"
            onClick={() => {openRestaurantDetails(restaurant)}}>
      {value}
    </Button>
  ), [openRestaurantDetails]);

  const tableColumns = useMemo(() => [{
    title: 'Name',
    dataIndex: 'name',
    render: renderName,
  }, {
    title: 'Categories',
    dataIndex: 'categories',
    render: renderCategories,
    responsive: ['sm' as Breakpoint],
  }, {
    title: 'Tried before',
    dataIndex: 'triedBefore',
    render: (value: boolean) => value ? <CheckOutlined/> : <CloseOutlined/>,
    responsive: ['md' as Breakpoint],
  }, {
    title: 'Rating',
    dataIndex: 'rating',
  }, {
    title: 'Review',
    dataIndex: 'review',
    render: (value: string) => value ? createPopover(value) : null,
    responsive: ['md' as Breakpoint],
  }, {
    title: 'Notes',
    dataIndex: 'notes',
    render: renderNotes,
    responsive: ['md' as Breakpoint],
  }], [renderName]);

  if (!restaurants) {
    return null;
  }

  return (
    <Table columns={tableColumns}
           dataSource={restaurants}
           pagination={{size: 'small'}}/>
  );
};