import angular from 'angular';

// Import internal modules
import controller from './ngbInfoProduct.controller';
import component from './ngbInfoProduct.component';


export default angular.module('ngbInfoProduct', [])
    .controller(controller.UID, controller)
    .component('ngbInfoProduct',component)
    .name;
