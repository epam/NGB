// Import Style
import './ngbLog.scss';


import angular from 'angular';

// Import internal modules
import * as component from './ngbLog.component';
import {ngbLogService} from './ngbLog.service';

export default angular.module('ngbLog', [])
    .component('ngbLog', component)
    .service('ngbLogService', ngbLogService.instance)
    .name;