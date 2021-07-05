
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import CarManager from "./components/CarManager"

import ReservationManager from "./components/ReservationManager"

import RentalManager from "./components/RentalManager"


import Mypage from "./components/mypage"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/Car',
                name: 'CarManager',
                component: CarManager
            },

            {
                path: '/Reservation',
                name: 'ReservationManager',
                component: ReservationManager
            },

            {
                path: '/Rental',
                name: 'RentalManager',
                component: RentalManager
            },


            {
                path: '/mypage',
                name: 'mypage',
                component: mypage
            },


    ]
})
