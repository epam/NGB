import './ngbTrackSettings.scss';

import angular from 'angular';

import component from './ngbTrackSettings.component';
import controller from './ngbTrackSettings.controller';
import ngbTrackFontSize from './ngbTrackFontSize';
import trackCoverageSettings from './ngbTrackCoverageSettings';
import trackResizePreference from './ngbTrackResizePreference';
import wigColorPreference from './ngbWigColorPreference';
import collapsibleMenu from './ngbTrackSettings.collapsible.menu';

export default angular
    .module('ngbTrackSettings', [
        collapsibleMenu,
        trackCoverageSettings,
        trackResizePreference,
        wigColorPreference,
        ngbTrackFontSize,
    ])
    .controller(controller.UID, controller)
    .component('ngbTrackSettings', component).name;
