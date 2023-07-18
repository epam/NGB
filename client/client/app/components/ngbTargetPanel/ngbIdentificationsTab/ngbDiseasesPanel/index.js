import angular from 'angular';
import NgbDiseasesPanelComponent from './ngbDiseasesPanel.component';
import NgbDiseasesPanelService from './ngbDiseasesPanel.service';
import NgbDiseasesPanelController from './ngbDiseasesPanel.controller';

import NgbDiseasesChartService from './ngbDiseasesCharts/ngbDiseases.chart.service';
import ngbDiseasesBubbles from './ngbDiseasesBubbles';
import ngbDiseasesGraph from './ngbDiseasesGraph';
import ngbDiseasesTable from './ngbDiseasesTable';

import './ngbDiseasesCharts/ngbDiseases.chart.scss';
import './ngbDiseasesPanel.scss';

export default angular.module('ngbDiseasesPanel', [
    ngbDiseasesBubbles,
    ngbDiseasesGraph,
    ngbDiseasesTable
])
    .component('ngbDiseasesPanel', NgbDiseasesPanelComponent)
    .service('ngbDiseasesChartService', NgbDiseasesChartService.instance)
    .service('ngbDiseasesPanelService', NgbDiseasesPanelService.instance)
    .controller(NgbDiseasesPanelController.UID, NgbDiseasesPanelController)
    .name;
