import './ngbVariantsTableDownload.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbVariantsTableDownload.component';
import controller from './ngbVariantsTableDownload.controller';
import dlgController from './ngbVariantsTableDownloadDlg.controller';


// Import external modules
export default angular.module('ngbVariantsTableDownload', [])
    .controller(dlgController.UID, dlgController)
    .controller(controller.UID, controller)
    .component('ngbVariantsTableDownload', component)
    .name;
