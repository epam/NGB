import {EventGeneInfo} from '../../../../shared/utils/events';

export default class NgbGenesTableContextMenuController {

    entity = {};
    featureName;
    sequenceTooLong = false;
    maxSequenceLength = Infinity;

    constructor($scope, dispatcher, appLayout, projectContext, ngbGenesTableContextMenu, ngbGenesTableService) {
        Object.assign(this, {dispatcher, appLayout, projectContext, ngbGenesTableContextMenu, ngbGenesTableService});
        this.entity = $scope.row.entity;
        this.featureName = this.entity[`${this.ngbGenesTableService.defaultPrefix}featureName`];

        const blastSettings = this.projectContext.getTrackDefaultSettings('blast_settings');
        const maxQueryLengthProperty = 'query_max_length';
        this.maxSequenceLength = blastSettings &&
        blastSettings.hasOwnProperty(maxQueryLengthProperty) &&
        !Number.isNaN(Number(blastSettings[maxQueryLengthProperty]))
            ? Number(blastSettings[maxQueryLengthProperty])
            : Infinity;
        const featureSize = Math.abs(
            (this.entity[`${this.ngbGenesTableService.defaultPrefix}startIndex`] || 0)
            - (this.entity[`${this.ngbGenesTableService.defaultPrefix}endIndex`] || 0)
        );
        this.sequenceTooLong = this.maxSequenceLength < featureSize;

    }

    blastSearch(tool, event) {
        this.ngbGenesTableContextMenu.close();
        const layoutChange = this.appLayout.Panels.blast;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        const readInfo = {
            referenceId: this.entity.referenceId,
            chromosomeId: this.entity.chromosomeObj.id,
            startIndex: this.entity[`${this.ngbGenesTableService.defaultPrefix}startIndex`],
            endIndex: this.entity[`${this.ngbGenesTableService.defaultPrefix}endIndex`]
        };
        const data = {
            ...readInfo,
            tool: tool,
            source: 'gene'
        };
        if (tool === 'blastp') {
            data.aminoAcid = true;
            data.feature = this.entity[`${this.ngbGenesTableService.defaultPrefix}featureType`];
            data.name = this.entity[`${this.ngbGenesTableService.defaultPrefix}featureName`];
            data.id = this.entity[`${this.ngbGenesTableService.defaultPrefix}featureFileId`];
        }
        this.dispatcher.emitSimpleEvent('read:show:blast', data);
        event.stopImmediatePropagation();
    }

    openMolecularView(event) {
        this.ngbGenesTableContextMenu.close();
        const layoutChange = this.appLayout.Panels.molecularViewer;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        const data = new EventGeneInfo({
            startIndex: this.entity[`${this.ngbGenesTableService.defaultPrefix}startIndex`],
            endIndex: this.entity[`${this.ngbGenesTableService.defaultPrefix}endIndex`],
            geneId: this.entity[`${this.ngbGenesTableService.defaultPrefix}featureId`],
            geneTracks: [{
                id: this.entity[`${this.ngbGenesTableService.defaultPrefix}featureFileId`],
                chromosomeId: this.entity.chromosomeObj ? +this.entity.chromosomeObj.id : undefined
            }]
        });
        this.dispatcher.emitSimpleEvent('miew:show:structure', data);
        event.stopImmediatePropagation();
    }
}