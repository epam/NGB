import './ngbTracksView.scss';

import angular from 'angular';

import ngbBrowserToolbarPanel from './ngbBrowserToolbarPanel';
import ngbRulerTrack from './ngbRulerTrack';
import ngbTrack from './ngbTrack';
import ngbTrackSettings from './ngbTrackSettings';
import ngbTrackActions from './ngbTrackActions';
import ngbTracksSelectionMenu from './ngbTracksSelectionMenu';
import ngbTrackNotification from './ngbTrackNotification';

import component from './ngbTracksView.component';
import controller from './ngbTracksView.controller';

export default angular.module('ngbTracksView', [
    ngbBrowserToolbarPanel,
    ngbTrackNotification,
    ngbTrack,
    ngbRulerTrack,
    ngbTrackSettings,
    ngbTrackActions,
    ngbTracksSelectionMenu
])
    .controller(controller.UID, controller)
    .component('ngbTracksView', component)
    .name;
