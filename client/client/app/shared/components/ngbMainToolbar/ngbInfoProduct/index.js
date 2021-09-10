import './ngbInfoProduct.scss';
import angular from 'angular';

// Import internal modules
import component from './ngbInfoProduct.component';
import controller from './ngbInfoProduct.controller';

export default angular.module('ngbInfoProduct', [])
    .controller(controller.UID, controller)
    .component('ngbInfoProduct', component)
    .name;
