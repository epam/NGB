import angular from 'angular';
import './ngbBrowser.scss';
// Import internal modules
import controller from './ngbBrowser.controller';
import component from './ngbBrowser.component';
import service from './ngbBrowser.service';

export default angular.module('ngbBrowser',[])
    .controller(controller.UID, controller)
    .component('ngbBrowser', component)
    .service('ngbBrowserService', service)
    .name;
