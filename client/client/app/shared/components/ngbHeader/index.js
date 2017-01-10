// Import Style
import './ngbHeader.scss';

import angular from 'angular';

// Import internal modules
import componentProject from './ngbHeaderProject.component.js';

// Import dependencies
import ngbShareLink from './ngbShareLink';
import ngbShareLinkMenu from './ngbShareLinkMenu';
import ngbToolWindows from './ngbToolWindows';
import ngbVersion from './ngbVersion';


export default angular.module('ngbHeader', [ngbVersion, ngbShareLink, ngbShareLinkMenu, ngbToolWindows])
    .component('ngbHeaderProject', componentProject)
    .name;
