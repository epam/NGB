import angular from 'angular';

import './ngbDiseasesTab.scss';

import component from './ngbDiseasesTab.component';
import controller from './ngbDiseasesTab.controller';
import service from './ngbDiseasesTab.service';

export default angular
    .module('ngbDiseasesTab', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTab', component)
    .service('ngbDiseasesTabService', service.instance)
    .name;
