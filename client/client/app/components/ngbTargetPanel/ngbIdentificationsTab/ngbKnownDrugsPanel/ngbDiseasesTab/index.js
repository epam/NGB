import angular from 'angular';

import './ngbDiseasesTab.scss';
import './ngbDiseasesCharts/ngbDiseases.chart.scss';

import component from './ngbDiseasesTab.component';
import controller from './ngbDiseasesTab.controller';
import chartsService from './ngbDiseasesCharts/ngbDiseases.chart.service';

import ngbDiseasesTable from './ngbDiseasesTable';
import ngbDiseasesBubbles from './ngbDiseasesBubbles';
import ngbDiseasesGraph from './ngbDiseasesGraph';

export default angular
    .module('ngbDiseasesTab', [ngbDiseasesTable, ngbDiseasesBubbles, ngbDiseasesGraph])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTab', component)
    .service('ngbDiseasesChartService', chartsService.instance)
    .name;
