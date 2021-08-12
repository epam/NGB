import './ngbTrackSettings.scss';

import angular from 'angular';

import component from './ngbTrackSettings.component';
import controller from './ngbTrackSettings.controller';
import ngbTrackFontSize from './ngbTrackFontSize';
import trackCoverageSettings from './ngbTrackCoverageSettings';
import trackResizePreference from './ngbTrackResizePreference';
import wigColorPreference from './ngbWigColorPreference';
import bedColorPreference from './ngbBedColorPreference';
import motifsColorPreference from './ngbMotifsColorPreference';
import collapsibleMenu from './ngbTrackSettings.collapsible.menu';
import ngbPreventAutoClose from './ngbPreventAutoClose';

export default angular
    .module('ngbTrackSettings', [
        collapsibleMenu,
        trackCoverageSettings,
        trackResizePreference,
        wigColorPreference,
        bedColorPreference,
        motifsColorPreference,
        ngbTrackFontSize,
    ])
    .directive('ngbPreventAutoClose', ngbPreventAutoClose)
    .controller(controller.UID, controller)
    .component('ngbTrackSettings', component).name;
