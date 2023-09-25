import angular from 'angular';

import './ngbDiseasesTab.scss';

import component from './ngbDiseasesTab.component';
import controller from './ngbDiseasesTab.controller';
import service from './ngbDiseasesTab.service';

import ngbDiseasesTargetsPanel from './ngbDiseasesTargetsPanel';

export default angular
    .module('ngbDiseasesTab', [ngbDiseasesTargetsPanel])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTab', component)
    .service('ngbDiseasesTabService', service.instance)
    .name;
