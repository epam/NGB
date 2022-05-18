export default class ngbFeatureInfoMainController {

    static get UID() {
        return 'ngbFeatureInfoMainController';
    }

    sequence = null;
    sequenceProgress = 0;
    isSequenceLoading = true;
    error = null;

    constructor($scope, dispatcher, genomeDataService, bamDataService, $anchorScroll, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, dispatcher, genomeDataService, bamDataService, $anchorScroll, ngbFeatureInfoPanelService});

        if (!this.read) {
            (async() => {this.loadSequence();})();
        }
        else {
            this.isSequenceLoading = false;
        }

        if (this.infoForRead) {
            (async() => {this.loadRead();})();
        }
        else {
            this.isReadLoadingis = false;
        }
        this.dispatcher.on('feature:info:edit:click', ::this.onClickEditBtn);
        this.dispatcher.on('feature:info:cancel:click', ::this.onClickCancelBtn);
        this.dispatcher.on('feature:info:save:click', ::this.onClickSaveBtn);
        this.dispatcher.on('feature:info:add:click', ::this.scrollTo);
    }

    loadSequence() {
        this.isSequenceLoading = true;
        const referenceSize = this.endIndex - this.startIndex;
        const maxReferenceRequestSize = 50000;
        const parts = [];
        for (let i = 0; i < Math.floor(referenceSize / maxReferenceRequestSize) + 1; i++) {
            const start = Math.min(this.startIndex + i * maxReferenceRequestSize, this.endIndex);
            const end = Math.min(start + maxReferenceRequestSize - 1, this.endIndex);
            if (end - start > 0) {
                parts.push({
                    chromosomeId: this.chromosomeId,
                    endIndex: end,
                    id: this.referenceId,
                    scaleFactor: 1,
                    startIndex: start
                });
            }
        }
        const refresh = (result) => {
            this.sequence = result;
            if (!result || !result.length) {
                this.error = 'Error loading sequence for feature';
            } else {
                this.error = null;
            }
            this.isSequenceLoading = false;
            this.$scope.$apply();
        };
        if (parts.length > 0) {
            this.getReference(parts, '', 0, refresh);
        } else {
            this.isSequenceLoading = false;
            this.error = 'Error loading sequence for feature';
        }
    }

    loadRead() {
        this.isReadLoadingis = true;
        this.sequenceWithQualities = [];
        this.bamDataService.loadRead(this.infoForRead).then(read => {
            for (let i = 0; i < read.qualities.length; i++) {
                this.sequenceWithQualities.push({nucleotide : read.sequence[i], qualitie: read.qualities.charCodeAt(i) - 33});
            }
            this.tags = read.tags.map(tag => [tag.tag, tag.value]);
            this.isReadLoadingis = false;
        });
    }

    getReference(blocks, referenceBuffer, index, callback) {

        if (index >= blocks.length) {
            callback(referenceBuffer);
            return;
        }
        this.genomeDataService.loadReferenceTrack(blocks[index]).then(data => {
            const refBlocks = data.blocks || [];
            this.sequenceProgress = 100.0 * (index + 1) / blocks.length;
            this.$scope.$apply();
            if (!refBlocks) {
                referenceBuffer = null;
                callback(referenceBuffer);
            }
            else {
                referenceBuffer += refBlocks.reduce((a, b) => a + b.text, '');
                this.getReference(blocks, referenceBuffer, index + 1, callback);
            }
        });
    }

    scrollTo(id) {
        this.$anchorScroll(id);
    }

    get editMode() {
        return this.ngbFeatureInfoPanelService.editMode;
    }
    set editMode(value) {
        this.ngbFeatureInfoPanelService.editMode = value;
    }

    get saveError () {
        return this.ngbFeatureInfoPanelService.saveError;
    }
    set saveError (value) {
        this.ngbFeatureInfoPanelService.saveError = value;
    }

    get isGeneralInfoOpen () {
        return this.ngbFeatureInfoPanelService.isGeneralInfoOpen;
    }

    set isGeneralInfoOpen (value) {
        this.ngbFeatureInfoPanelService.isGeneralInfoOpen = value;
    }

    get saveInProgress() {
        return this.ngbFeatureInfoPanelService.saveInProgress;
    }
    set saveInProgress(value) {
        this.ngbFeatureInfoPanelService.saveInProgress = value;
    }

    onClickEditBtn () {
        this.editMode = true;
        this.ngbFeatureInfoPanelService.newAttributes = this.properties;
    }

    onClickCancelBtn () {
        this.editMode = false;
        this.ngbFeatureInfoPanelService.newAttributes = null;
        this.saveInProgress = false;
        this.saveError = null;
    }

    onClickSaveBtn () {
        this.saveInProgress = true;
        this.ngbFeatureInfoPanelService.saveNewAttributes();
        this.properties = [...this.ngbFeatureInfoPanelService.newAttributes
            .map(newAttribute => (
                [
                    newAttribute.name,
                    newAttribute.value,
                    newAttribute.attribute,
                    newAttribute.deleted || false
                ]
            ))];
        this.feature = this.ngbFeatureInfoPanelService.updateFeatureInfo(this.feature);
        this.ngbFeatureInfoPanelService.sendNewGeneInfo(this.fileId, this.uuid, this.feature)
            .then((success) => {
                this.saveInProgress = false;
                const data = { trackId: this.fileId };
                if (success) {
                    this.dispatcher.emitSimpleEvent('feature:info:changes:cancel');
                    this.dispatcher.emitSimpleEvent('feature:info:saved', data);
                }
                this.$scope.$apply();
            });
    }
}
