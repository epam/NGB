'use strict';

import angular from 'angular';

import 'angular-ui-grid/ui-grid.min.js';
import 'angular-ui-grid/ui-grid.min.css';
import '../../modules/uiGridGroupColumns/ui-grid-group-columns';

const deps = [
    'ui.grid',
    'ui.grid.moveColumns',
    'ui.grid.resizeColumns',
    'ui.grid.cellNav',
    'ui.grid.autoResize',
    'ui.grid.selection',
    'ui.grid.grouping',
    'ui.grid.pinning',
    'ui.grid.saveState',
    'ui.grid.infiniteScroll',
    'uiGridGroupColumns'
];

export default angular.module('uiGrid', deps).name;
