import {Button, Card, Col, Row} from 'antd';
import React, {useMemo, useState} from 'react';
import {Category, RestaurantData} from '../../api';
import {restaurantsApi} from '../../api/ApiFacade';
import {RestaurantModal} from './RestaurantModal';
import './RestaurantSearchPage.css';
import {RestaurantsSearchForm} from './RestaurantsSearchForm';
import {RestaurantsTable} from './RestaurantsTable';

type SearchQuery = {
  nameBeginsWith?: string,
  category?: Category,
  triedBefore: boolean,
  ratingAtLeast?: number
};

export const RestaurantSearchPage = () => {

  const [restaurants, setRestaurants] = useState<RestaurantData[]>();
  const [modalRestaurant, setModalRestaurant] = useState<RestaurantData>();
  const [formResetCounter, setFormResetCounter] = useState(0);

  const searchRestaurants = ({nameBeginsWith, category, ratingAtLeast, triedBefore}: SearchQuery) => {
    setRestaurants(undefined);
    restaurantsApi.queryRestaurantsByCriteria(nameBeginsWith, category, triedBefore, ratingAtLeast)
                  .then(({data}: { data: RestaurantData[] }) => setRestaurants(data));
  };

  const cardTitle = useMemo(() => (
    <Row>
      <Col span={18}>Restaurants</Col>
      <Col span={6} style={{textAlign: 'right'}}>
        <Button onClick={() => setModalRestaurant({})}>
          Add
        </Button>
      </Col>
    </Row>
  ), []);

  return (
    <>
      <Card title={cardTitle}>
        <RestaurantsSearchForm onSubmit={searchRestaurants}
                               resetTrigger={formResetCounter}/>
        <RestaurantsTable restaurants={restaurants}
                          openRestaurantDetails={setModalRestaurant}/>
      </Card>
      {modalRestaurant && <RestaurantModal restaurantData={modalRestaurant}
                                           onClose={() => setModalRestaurant(undefined)}
                                           onSaveSuccess={() => {
                                             setRestaurants(undefined);
                                             setFormResetCounter(current => ++current);
                                           }}/>}
    </>
  );
};