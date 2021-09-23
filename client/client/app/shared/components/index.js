import angular from 'angular';

import ngbGoldenLayout from './ngbGoldenLayout';
import ngbMainToolbar from './ngbMainToolbar';
import ngbMainSettings from './ngbMainSettings';
import ngbPanelErrorList from './ngbPanelErrorList';
import ngbSearch from './ngbSearch';
import ngbUiParts from './ngbUiParts';
import ngbVariantDetails from './ngbVariantDetails';
import ngbDatasetItemDownloadUrlController from './ngbDatasetItemDownloadUrl';
import ngbMarkdown from './ngbMarkdown';

import collapsiblePanel from './widgets/collapsiblePanel';
import textBox from './widgets/TextBox';
import ngbColorPicker from './widgets/ngbColorPicker';

export default angular.module('SharedComponents', [
    ngbGoldenLayout,
    ngbMainToolbar,
    ngbPanelErrorList,
    ngbSearch,
    ngbMainSettings,
    ngbUiParts,
    ngbVariantDetails,
    collapsiblePanel,
    textBox,
    ngbDatasetItemDownloadUrlController,
    ngbMarkdown,
    ngbColorPicker
]).name;
