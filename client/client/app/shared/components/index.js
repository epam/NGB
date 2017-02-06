import angular from 'angular';

import ngbGoldenLayout from './ngbGoldenLayout';
import ngbMainToolbar from './ngbMainToolbar';
import ngbMainSettings from './ngbMainSettings';
import ngbPanelErrorList from './ngbPanelErrorList';
import ngbSearch from './ngbSearch';
import ngbUiParts from './ngbUiParts';
import ngbVariantDetails from './ngbVariantDetails';


import collapsiblePanel from './widgets/collapsiblePanel';
import textBox from './widgets/TextBox';

export default angular.module('SharedComponents', [
    ngbGoldenLayout,
    ngbMainToolbar,
    ngbPanelErrorList,
    ngbSearch,
    ngbMainSettings,
    ngbUiParts,
    ngbVariantDetails,
    collapsiblePanel,
    textBox
]).name;