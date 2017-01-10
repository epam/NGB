import angular from 'angular';
import './ngbBrowser.scss';
// Import internal modules
import controller from './ngbBrowser.controller';
import component from './ngbBrowser.component';


export default angular.module('ngbBrowser',[])
    .controller(controller.UID, controller)
    .component('ngbBrowser', component)
    .name;