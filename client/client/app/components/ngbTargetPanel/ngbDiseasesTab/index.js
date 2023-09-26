import angular from 'angular';

import './ngbDiseasesTab.scss';

import component from './ngbDiseasesTab.component';
import controller from './ngbDiseasesTab.controller';
import service from './ngbDiseasesTab.service';

import ngbDiseasesTargetsPanel from './ngbDiseasesTargetsPanel';
import ngbDiseasesDrugsPanel from './ngbDiseasesDrugsPanel';

export default angular
    .module('ngbDiseasesTab', [ngbDiseasesTargetsPanel, ngbDiseasesDrugsPanel])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTab', component)
    .service('ngbDiseasesTabService', service.instance)
    .name;
