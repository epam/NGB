// Import Style
import './ngbTrack.scss';

import angular from 'angular';
import dndLists from '../../../shared/dnd';
// Import internal modules

import ngbAutofocus from '../../../shared/directives/autofocus.directive';
import controller from './ngbTrack.controller';
import component from './ngbTrack.component';
import ngbTrackMenu from './events/ngbTrackMenu.component';
import ngbResizePanelDirective from './ngbResizePanel.directive';
import dataServices from '../../../../dataServices/angular-module';
import menu from '../ngbTrackSettings';

export default angular.module('ngbTrack' , [dndLists, dataServices, menu, ngbAutofocus])
    .controller(controller.UID, controller)
    .component('ngbTrack', component)
    .component('ngbTrackMenu', ngbTrackMenu)
    .directive('ngbResizePanel', ngbResizePanelDirective)
    .name;
    