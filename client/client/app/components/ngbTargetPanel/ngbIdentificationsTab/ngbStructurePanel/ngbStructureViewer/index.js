import angular from 'angular';

import './ngbStructureViewer.scss';

import component from './ngbStructureViewer.component';
import controller from './ngbStructureViewer.controller';

import ngbMiew from '../../../../ngbMolecularViewer/ngbMiew';

export default angular
    .module('ngbStructureViewer', [ngbMiew])
    .controller(controller.UID, controller)
    .component('ngbStructureViewer', component)
    .name;