import angular from 'angular';

import './ngbDatasetItemDownloadUrl.scss';

// Import internal modules
import component from './ngbDatasetItemDownloadUrl.component';
import controller from './ngbDatasetItemDownloadUrl.controller';
import filesizeFilter from './ngbDatasetItemDownloadUrl.filesize.filter';

export default angular.module('ngbDatasetItemDownloadUrl', [])
    .component('ngbDatasetItemDownloadUrl', component)
    .controller(controller.UID, controller)
    .filter('filesize', filesizeFilter)
    .name;






