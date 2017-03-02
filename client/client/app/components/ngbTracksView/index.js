import './ngbTracksView.scss';

import angular from 'angular';

import ngbBrowserToolbarPanel from './ngbBrowserToolbarPanel';
import ngbRulerTrack from './ngbRulerTrack';
import ngbTrack from './ngbTrack';
import ngbTrackSettings from './ngbTrackSettings';
import ngbTrackActions from './ngbTrackActions';

import component from './ngbTracksView.component';
import controller from './ngbTracksView.controller';

export default angular.module('ngbTracksView', [
    ngbBrowserToolbarPanel,
    ngbTrack,
    ngbRulerTrack,
    ngbTrackSettings,
    ngbTrackActions
])
    .controller(controller.UID, controller)
    .component('ngbTracksView', component)
    .name;
