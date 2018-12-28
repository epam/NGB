// Import Style
import './ngbUser.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbUser.component.js';
import controller from './ngbUser.controller';
import dataServices from '../../../../../dataServices/angular-module';
import service from './ngbUser.service';

export default angular.module('ngbUser', [dataServices])
    .service('ngbUserService', service)
    .controller(controller.UID, controller)
    .component('ngbUser', component)
    .name;