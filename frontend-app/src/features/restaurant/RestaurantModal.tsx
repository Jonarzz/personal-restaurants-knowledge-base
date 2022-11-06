import {Form, Input, Modal, Select, Switch} from 'antd';
import React, {useState} from 'react';
import {RestaurantData} from '../../api';
import {restaurantEntryApi, restaurantsApi} from '../../api/ApiFacade';
import {CATEGORY_SELECT_OPTIONS, RATING_SELECT_OPTIONS} from './common';

type Props = { restaurantData: RestaurantData, onClose: Function };

type FormData = Omit<RestaurantData, 'notes'> & { notes: string };

const formToRestaurantData = (values: FormData): RestaurantData => ({
  ...values,
  notes: values.notes?.split('\n'),
});

export const RestaurantModal = ({restaurantData, onClose}: Props) => {

  const [name, setName] = useState(restaurantData.name);
  const [triedBefore, setTriedBefore] = useState(restaurantData.triedBefore);

  const [modalForm] = Form.useForm();

  // TODO use the responses to update the state

  const onValuesChange = (changedFields: FormData) => {
    changedFields.name && setName(changedFields.name);
    changedFields.triedBefore && setTriedBefore(changedFields.triedBefore);
  };

  const createRestaurant = (formData: FormData) => {
    restaurantsApi.createRestaurant(formToRestaurantData(formData))
                  .then(response => {
                    console.log('Created', response);
                  });
  };

  const updateRestaurant = (restaurantName: string, formData: FormData) => {
    restaurantEntryApi.updateRestaurant(restaurantName, formToRestaurantData(formData))
                      .then(response => {
                        console.log('Updated', response);
                      });
  };

  const submitForm = (values: any) => {
    if (!values.triedBefore) {
      delete values.rating;
      delete values.review;
    }
    const restaurantName = restaurantData.name;
    if (restaurantName) {
      updateRestaurant(restaurantName, values);
    } else {
      createRestaurant(values);
    }
  };
  return (
    <Modal open
           title={restaurantData.name || 'New restaurant'}
           okText={restaurantData.name ? 'Update' : 'Create'}
           okButtonProps={{disabled: !name}}
           onOk={() => {
             modalForm.submit();
             onClose();
           }}
           onCancel={() => onClose()}>

      <Form form={modalForm}
            onFinish={submitForm}
            onValuesChange={onValuesChange}
            autoComplete="off">

        <Form.Item name="name" initialValue={restaurantData.name}>
          <Input placeholder="Name"/>
        </Form.Item>

        <Form.Item name="categories" initialValue={restaurantData.categories}>
          <Select mode="multiple"
                  placeholder="Categories">
            {CATEGORY_SELECT_OPTIONS}
          </Select>
        </Form.Item>

        <Form.Item name="triedBefore" initialValue={restaurantData.triedBefore}>
          <Switch checkedChildren="Tried before"
                  unCheckedChildren="Not tried before"
                  defaultChecked={restaurantData.triedBefore}
                  style={{width: '100%'}}/>
        </Form.Item>

        <Form.Item name="rating" initialValue={restaurantData.rating}>
          <Select placeholder="Rating" disabled={!triedBefore}>
            {RATING_SELECT_OPTIONS}
          </Select>
        </Form.Item>

        <Form.Item name="review" initialValue={restaurantData.review}>
          <Input.TextArea placeholder="Review" disabled={!triedBefore}/>
        </Form.Item>

        <Form.Item name="notes" initialValue={restaurantData.notes?.join('\n')}>
          <Input.TextArea placeholder="Notes (separated with newlines)" autoSize/>
        </Form.Item>

      </Form>
    </Modal>
  );
};