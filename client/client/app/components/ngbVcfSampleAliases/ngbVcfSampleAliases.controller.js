export default class ngbVcfSampleAliasesController {

    saveInProgress = false;
    errorMessage = null;
    samplesModel = {};
    currentSamples = {};

    static get UID() {
        return 'ngbVcfSampleAliasesController';
    }

    get vcfFileId () {
        return this.config.id;
    }

    get isDisable () {
        return this.nothingChanged() || this.someDuplicate();
    }

    nothingChanged () {
        return Object.entries(this.currentSamples)
            .every(item => {
                const [name, prettyName] = item;
                return this.samplesModel[name] === prettyName;
            });
    }

    someDuplicate () {
        const prettyNamesArray = Object.values(this.samplesModel).filter(name => name);
        const prettyNamesSet = new Set(prettyNamesArray);
        return prettyNamesArray.length !== prettyNamesSet.size;
    }

    isDuplicate (alias) {
        if (alias) {
            return Object.values(this.samplesModel)
                .filter(prettyName => prettyName === alias)
                .length > 1;
        }
        return false;
    }

    constructor($scope, $mdDialog, dispatcher, projectContext, config, samplesInfo, vcfDataService) {
        Object.assign(this, {$scope, $mdDialog, dispatcher, projectContext, config, vcfDataService});
        this.currentSamples = Object.assign({}, samplesInfo);
        this.samplesModel = Object.assign({}, samplesInfo);
    }

    onChangeInput () {
        this.errorMessage = null;
    }

    onClickCloseBtn () {
        this.$mdDialog.hide();
    }

    onClickSaveBtn () {
        this.saveInProgress = true;
        this.errorMessage = null;
        const request = {
            vcfFileId: this.vcfFileId,
            aliases: this.samplesModel
        };
        this.putSamplesAliases(request)
            .then(result => {
                if (result) {
                    this.dispatcher.emitSimpleEvent('vcf:refresh:track', true);
                    this.setVariantsTable();
                    this.$mdDialog.hide();
                } else {
                    return;
                }
            })
            .finally(() => {
                this.saveInProgress = false;
                this.$scope.$apply();
            });
    }

    putSamplesAliases (request) {
        return new Promise(resolve => {
            this.vcfDataService.putSamplesAliases(request)
            .then(() => {
                resolve(true);
            })
            .catch(error => {
                this.errorMessage = error.message;
                resolve(false);
            });
        });
    }

    async setVariantsTable() {
        const sampleNamesFilter = (this.projectContext.vcfFilter || {}).sampleNames || [];
        let prevSampleNames;
        if (sampleNamesFilter.length) {
            prevSampleNames = sampleNamesFilter.map(sampleName => (
                this.projectContext.vcfSampleAliases[sampleName]
            ));
        }
        await this.projectContext.setVcfInfo(false);
        if (prevSampleNames) {
            const newSampleAliases = prevSampleNames.map(name => (
                this.projectContext.vcfSampleInfo[this.config.id][name]
            ));
            this.projectContext.vcfFilter.sampleNames = newSampleAliases;
        }
        this.projectContext.filterVariants(true);
    }

    onClickCancelBtn () {
        this.$mdDialog.hide();
    }
}
