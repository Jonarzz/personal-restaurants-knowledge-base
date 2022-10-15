import {RestaurantEntryApi, RestaurantsApi} from './api';

// TODO based on build config
const basePath = 'http://localhost:8080';

export const restaurantsApi = new RestaurantsApi(undefined, basePath);
export const restaurantEntryApi = new RestaurantEntryApi(undefined, basePath);
