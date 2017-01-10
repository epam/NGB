// Import Style
import './collapsiblePanel.scss';

import angular from 'angular';

// Import internal modules
import collapsible from './collapsible.component';
import collapsiblePanel from './collapsiblePanel.component';

export default angular.module('dropDownMenu ', [])
    .component('collapsible', collapsible)
    .component('collapsiblePanel', collapsiblePanel)
    .name;
