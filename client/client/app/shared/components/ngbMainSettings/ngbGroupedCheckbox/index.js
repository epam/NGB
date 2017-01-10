import angular from 'angular';
import component from './ngbGroupedCheckbox.component';
import controller from './ngbGroupedCheckbox.controller';


export default angular.module('ngbGroupedCheckboxComponent', [])
    .component('ngbGroupedCheckbox', component)
    .controller(controller.UID, controller)
    .name;