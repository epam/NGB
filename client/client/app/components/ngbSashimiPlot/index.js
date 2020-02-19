import './ngbSashimiPlot.scss';

import angular from 'angular';

import ngbBrowserToolbarPanel from '../ngbTracksView/ngbBrowserToolbarPanel';
import controller from './ngbSashimiPlot.controller';
import component from './ngbSashimiPlot.component';
import run from './ngbSashimiPlot.run';

export default angular.module('ngbSashimiPlot', [
    ngbBrowserToolbarPanel
])
  .controller(controller.UID, controller)
  .component('ngbSashimiPlot', component)
  .run(run)
  .name;
