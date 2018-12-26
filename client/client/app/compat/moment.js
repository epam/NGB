'use strict';

import angular from 'angular';
import moment from 'moment';

export default angular
    .module('moment', [])
    .factory('moment', function ($window) {
        if (!$window.moment) {
            $window.moment = moment;
        }
        return $window.moment;
    }).name;
