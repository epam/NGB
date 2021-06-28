import angular from 'angular';
import ngbBookmarksPanel from './ngbBookmarksPanel';
import ngbBrowser from './ngbBrowser';
import ngbDataSets from './ngbDataSets';
import ngbFeatureInfoPanel from './ngbFeatureInfoPanel';
import ngbLogModule from './ngbLog';
import ngbMolecularViewer from './ngbMolecularViewer';
import ngbProjectInfoPanel from './ngbProjectInfoPanel';
import ngbSashimiPlot from './ngbSashimiPlot';
import ngbTracksView from './ngbTracksView';
import ngbTracksSelection from './ngbTracksSelection';
import ngbVariantPanel from './ngbVariantPanel';
import ngbVariantsTablePanel from './ngbVariantsTablePanel';
import ngbOrganizeTracks from './ngbOrganizeTracks';
import ngbGenomeAnnotations from './ngbGenomeAnnotations';
import ngbBlatSearchPanel from './ngbBlatSearchPanel';
import ngbBlastSearchPanel from './ngbBlastSearchPanel';
import ngbGenesTablePanel from './ngbGenesTablePanel';

export default angular.module('NGB_Panels', [
    ngbBookmarksPanel,
    ngbBrowser,
    ngbDataSets,
    ngbLogModule,
    ngbMolecularViewer,
    ngbProjectInfoPanel,
    ngbSashimiPlot,
    ngbTracksView,
    ngbTracksSelection,
    ngbVariantPanel,
    ngbVariantsTablePanel,
    ngbFeatureInfoPanel,
    ngbOrganizeTracks,
    ngbGenomeAnnotations,
    ngbBlatSearchPanel,
    ngbBlastSearchPanel,
    ngbGenesTablePanel
]).name;
