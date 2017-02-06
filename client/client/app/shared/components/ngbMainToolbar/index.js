// Import Style
import './ngbMainToolbar.scss';

import angular from 'angular';

// Import internal modules
import componentProject from './ngbMainToolbar.component.js';

// Import dependencies
import ngbShareLink from './ngbShareLink';
import ngbShareLinkMenu from './ngbShareLinkMenu';
import ngbToolWindows from './ngbToolWindows';
import ngbInfoProduct from './ngbInfoProduct';
import ngbVersion from './ngbVersion';


export default angular.module('ngbMainToolbar', [ngbVersion, ngbShareLink, ngbShareLinkMenu, ngbToolWindows, ngbInfoProduct])
    .component('ngbMainToolbar', componentProject)
    .name;
