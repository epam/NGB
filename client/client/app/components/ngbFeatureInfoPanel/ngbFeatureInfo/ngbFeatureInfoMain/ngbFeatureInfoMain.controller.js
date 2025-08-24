export default class ngbFeatureInfoMainController {

    static get UID() {
        return 'ngbFeatureInfoMainController';
    }

    sequence = null;
    sequenceProgress = 0;
    isSequenceLoading = true;
    error = null;
    _isGeneralInfoOpen = true;

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
        this.dispatcher.on('feature:info:changes:cancel', this.onClickCancelBtn.bind(this));
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

    get editMode () {
        return this.ngbFeatureInfoPanelService.editMode;
    }

    get disableSave () {
        return this.ngbFeatureInfoPanelService.disableSaveButton();
    }

    get saveError () {
        return this.ngbFeatureInfoPanelService.saveError;
    }

    get saveInProgress () {
        return this.ngbFeatureInfoPanelService.saveInProgress;
    }

    get isGeneralInfoOpen () {
        return this._isGeneralInfoOpen;
    }

    set isGeneralInfoOpen (value) {
        this._isGeneralInfoOpen = value;
    }

    onClickEditBtn (event) {
        if (event) {
            event.stopPropagation();
        }
        this.ngbFeatureInfoPanelService.editMode = true;
        this.ngbFeatureInfoPanelService.newAttributes = this.properties;
    }

    onClickSaveBtn (event) {
        if (event) {
            event.stopPropagation();
        }
        this.ngbFeatureInfoPanelService.saveInProgress = true;
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
                this.ngbFeatureInfoPanelService.saveInProgress = false;
                const data = { trackId: this.fileId };
                if (success) {
                    this.onClickCancelBtn();
                    this.dispatcher.emitSimpleEvent('feature:info:saved', data);
                }
                this.$scope.$apply();
            });
    }

    onClickCancelBtn (event) {
        if (event) {
            event.stopPropagation();
        }
        this.ngbFeatureInfoPanelService.editMode = false;
        this.ngbFeatureInfoPanelService.newAttributes = null;
        this.ngbFeatureInfoPanelService.saveInProgress = false;
        this.ngbFeatureInfoPanelService.saveError = null;
    }
}
