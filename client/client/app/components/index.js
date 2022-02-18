import angular from 'angular';
import ngbBlastSearchPanel from './ngbBlastSearchPanel';
import ngbBlatSearchPanel from './ngbBlatSearchPanel';
import ngbBookmarksPanel from './ngbBookmarksPanel';
import ngbBrowser from './ngbBrowser';
import ngbDataSets from './ngbDataSets';
import ngbFeatureInfoPanel from './ngbFeatureInfoPanel';
import ngbGenesTablePanel from './ngbGenesTablePanel';
import ngbGenomeAnnotations from './ngbGenomeAnnotations';
import ngbHeatmapPanel from './ngbHeatmapPanel';
import ngbHomologsPanel from './ngbHomologsPanel';
import ngbLogModule from './ngbLog';
import ngbMolecularViewer from './ngbMolecularViewer';
import ngbOrganizeTracks from './ngbOrganizeTracks';
import ngbPathways from './ngbPathways';
import ngbProjectInfoPanel from './ngbProjectInfoPanel';
import ngbSashimiPlot from './ngbSashimiPlot';
import ngbStrainLineage from './ngbStrainLineage';
import ngbTracksSelection from './ngbTracksSelection';
import ngbTracksView from './ngbTracksView';
import ngbVariantPanel from './ngbVariantPanel';
import ngbVariantsTablePanel from './ngbVariantsTablePanel';
import ngbMotifsPanel from './ngbMotifsPanel';
import ngbVcfSampleAliases from './ngbVcfSampleAliases';
import ngbCoveragePanel from './ngbCoveragePanel';

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
    ngbGenesTablePanel,
    ngbHomologsPanel,
    ngbHeatmapPanel,
    ngbStrainLineage,
    ngbPathways,
    ngbMotifsPanel,
    ngbVcfSampleAliases,
    ngbCoveragePanel
]).name;
