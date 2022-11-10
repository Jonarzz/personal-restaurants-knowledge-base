import {Form, Input, Modal, notification, Select, Switch} from 'antd';
import React, {useEffect, useState} from 'react';
import {Category, RestaurantData} from '../../api';
import {restaurantEntryApi, restaurantsApi} from '../../api/ApiFacade';
import {CATEGORY_SELECT_OPTIONS, RATING_SELECT_OPTIONS} from './common';

type Props = {
  restaurantData: RestaurantData,
  onClose: Function,
  onSaveSuccess: Function
};

export type FormData = Omit<RestaurantData, 'notes'> & { notes?: string };

const formToRestaurantData = (values: FormData): RestaurantData => ({
  ...values,
  notes: values.notes?.split('\n'),
});

const categoriesAsStrings = (categories?: Set<Category>): string[] => Array.from(categories || []);

const notificationDisplayDurationSeconds = 2;
const openRestaurantSavedNotification = (header: string) =>
  notification.success({
    message: header,
    description: 'You can find the restaurant using the search form',
    duration: notificationDisplayDurationSeconds,
  });

const handleApiError = (error: any) => {
  console.error(error);
  notification.error({
    message: 'Error',
    description: 'An error occurred. Please, contact the administrator.',
    duration: notificationDisplayDurationSeconds
  });
};

export const RestaurantModal = ({restaurantData, onClose, onSaveSuccess}: Props) => {

  const [name, setName] = useState(restaurantData.name);
  const [categories, setCategories] = useState(categoriesAsStrings(restaurantData.categories));
  const [triedBefore, setTriedBefore] = useState(restaurantData.triedBefore);

  const [loading, setLoading] = useState(false);

  const [modalForm] = Form.useForm();

  useEffect(() => {
    setName(restaurantData.name);
    setCategories(categoriesAsStrings(restaurantData.categories));
    setTriedBefore(restaurantData.triedBefore);
  }, [restaurantData]);

  const onValuesChange = (changedFields: FormData, values: FormData) => {
    setName(values.name);
    setCategories(categoriesAsStrings(values.categories));
    setTriedBefore(values.triedBefore);
  };

  const createRestaurant = (formData: FormData) => {
    setLoading(true);
    restaurantsApi.createRestaurant(formToRestaurantData(formData))
                  .then(() => {
                    onClose();
                    onSaveSuccess();
                    openRestaurantSavedNotification('Restaurant created');
                  })
                  .catch(handleApiError)
                  .finally(() => setLoading(false));
  };

  const updateRestaurant = (restaurantName: string, formData: FormData) => {
    setLoading(true);
    restaurantEntryApi.updateRestaurant(restaurantName, formToRestaurantData(formData))
                      .then(() => {
                        onClose();
                        onSaveSuccess();
                        openRestaurantSavedNotification('Restaurant updated');
                      })
                      .catch(handleApiError)
                      .finally(() => setLoading(false));
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

  // TODO restaurant deletion

  return (
    <Modal open
           title={restaurantData.name || 'New restaurant'}
           okText={restaurantData.name ? 'Update' : 'Create'}
           okButtonProps={{
             loading,
             disabled: !name || !categories?.length,
           }}
           onOk={() => modalForm.submit()}
           onCancel={() => onClose()}>

      <Form form={modalForm}
            onFinish={submitForm}
            onValuesChange={onValuesChange}
            autoComplete="off">

        <Form.Item name="name" initialValue={restaurantData.name}>
          <Input placeholder="Name"/>
        </Form.Item>

        <Form.Item name="categories" initialValue={restaurantData.categories}>
          <Select mode="multiple" allowClear
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