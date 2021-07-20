import './ngbGenesTableDownload.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbGenesTableDownload.component';
import controller from './ngbGenesTableDownload.controller';
import dlgController from './ngbGenesTableDownloadDlg.controller';


// Import external modules
export default angular.module('ngbGenesTableDownloadComponent', [])
    .controller(dlgController.UID, dlgController)
    .controller(controller.UID, controller)
    .component('ngbGenesTableDownload', component)
    .name;
