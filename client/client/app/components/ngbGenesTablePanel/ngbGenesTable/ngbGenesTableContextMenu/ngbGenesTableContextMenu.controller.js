import {EventGeneInfo} from '../../../../shared/utils/events';

export default class NgbGenesTableContextMenuController {

    entity = {};

    constructor($scope, dispatcher, appLayout, projectContext, ngbGenesTableContextMenu) {
        Object.assign(this, {dispatcher, appLayout, projectContext, ngbGenesTableContextMenu});
        this.entity = $scope.row.entity;
    }

    blastSearch(tool, event) {
        this.ngbGenesTableContextMenu.close();
        const layoutChange = this.appLayout.Panels.blast;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        const readInfo = {
            referenceId: this.entity.referenceId,
            chromosomeId: this.entity.chromosome.id,
            startIndex: this.entity.start,
            endIndex: this.entity.end
        };
        const data = {
            ...readInfo,
            tool: tool,
            source: 'geneTable'
        };
        this.dispatcher.emitSimpleEvent('read:show:blast', data);
        event.stopImmediatePropagation();
    }

    openMolecularView(event) {
        this.ngbGenesTableContextMenu.close();
        const layoutChange = this.appLayout.Panels.molecularViewer;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        const data = new EventGeneInfo({
            startIndex: this.entity.startIndex,
            endIndex: this.entity.endIndex,
            geneId: this.entity.featureId,
            geneTracks: [{
                id: this.entity.featureFileId,
                chromosomeId: this.entity.chromosome ? +this.entity.chromosome.id : undefined
            }]
        });
        this.dispatcher.emitSimpleEvent('miew:show:structure', data);
        event.stopImmediatePropagation();
    }
}
