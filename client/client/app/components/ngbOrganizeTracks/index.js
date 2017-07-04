import angular from 'angular';

// Import Style
import './ngbOrganizeTracks.scss';

// Import internal modules
import component from './ngbOrganizeTracks.component';
import controller from './ngbOrganizeTracks.controller';
import run from './ngbOrganizeTracks.run';

export default angular.module('ngbOrganizeTracks', [])
    .component('ngbOrganizeTracks', component)
    .controller(controller.UID, controller)
    .run(run)
    .name;