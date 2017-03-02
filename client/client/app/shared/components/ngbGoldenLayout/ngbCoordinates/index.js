// Import Style
import './ngbCoordinates.scss';


import angular from 'angular';

// Import internal modules
import searchItemComponent from './ngbSearchItem/ngbSearchItem.component'

import controller from './ngbCoordinates.controller';
import component from './ngbCoordinates.component';

export default angular.module('ngbCoordinates' , [])
    .component('ngbSearchItem', searchItemComponent)
    .controller(controller.UID, controller)
    .component('ngbCoordinates', component)
    .name;
