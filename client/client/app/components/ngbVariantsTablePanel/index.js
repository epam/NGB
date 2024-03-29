import angular from 'angular';

// Import Style
import './ngbVariantsTablePanel.scss';

// Import internal modules
import ngbVariantsTablePanel from './ngbVariantsTablePanel.component.js';
import controller from './ngbVariantsTablePanel.controller';

// Import components
import ngbVariantsLoadingIndicator from './ngbVariantsLoadingIndicator';
import ngbVariantsTable from './ngbVariantsTable';
import ngbVariantsTableDownload from './ngbVariantsTableDownload';
import ngbVariantsTableColumn from './ngbVariantsTableColumn';
import ngbVariantsTablePaginate from './ngbVariantsTablePaginate';


// Import external modules
export default angular.module('ngbVariantsTablePanel', [
    ngbVariantsTable,
    ngbVariantsTableDownload,
    ngbVariantsTableColumn,
    ngbVariantsTablePaginate,
    ngbVariantsLoadingIndicator])
    .component('ngbVariantsTablePanel', ngbVariantsTablePanel)
    .controller(controller.UID, controller)
    .name;
