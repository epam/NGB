// Import Style


// Import internal modules
import ngbProjectReference from './ngbProjectReference.component';
import ngbProjectTitle from './ngbProjectTitle.component';
import ngbProjectTracks from './ngbProjectTracks.component';
import angular from 'angular';

export default angular.module('ngbUiParts', [])
    .component('ngbProjectReference', ngbProjectReference)
    .component('ngbProjectTitle', ngbProjectTitle)
    .component('ngbProjectTracks', ngbProjectTracks)
    .name;


// Import app modules
