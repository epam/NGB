// Import Style
import './ngbMainToolbar.scss';

import angular from 'angular';
import dataServices from '../../../../dataServices/angular-module';

// Import internal modules
import componentProject from './ngbMainToolbar.component.js';

// Import dependencies
import ngbShareLink from './ngbShareLink';
import ngbShareLinkMenu from './ngbShareLinkMenu';
import ngbToolWindows from './ngbToolWindows';
import ngbInfoProduct from './ngbInfoProduct';
import ngbOpenFile from './ngbOpenFile';
import ngbVersion from './ngbVersion';
import ngbBookmark from './ngbBookmark';
import ngbUserManagement from '../ngbUserManagement';

let dependncies;
if (process.env.__DESKTOP__) { // in desktop mode no shareLink components are needed
    dependncies = [ngbVersion, ngbToolWindows, ngbInfoProduct, ngbBookmark, ngbOpenFile, ngbUserManagement, dataServices];
} else {
    dependncies = [ngbVersion, ngbShareLink, ngbShareLinkMenu, ngbToolWindows, ngbInfoProduct, ngbBookmark, ngbOpenFile, ngbUserManagement, dataServices];
}

export default angular.module('ngbMainToolbar', dependncies)
    .component('ngbMainToolbar', componentProject)
    .name;
