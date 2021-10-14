import angular from 'angular';
import baseController from '../../../shared/baseController';

export default class ngbBlastSearchResult extends baseController {

    static get UID() {
        return 'ngbBlastSearchResult';
    }

    searchResult = {};
    defaultParams = {};
    paramsPartitions = [];
    isProgressShown = true;
    options = '';

    events = {
        'defaultSettings:change': this.setDefaultParams.bind(this)
    };


    constructor($mdDialog, ngbBlastSearchService, ngbBlastSearchFormConstants, projectContext) {
        super();
        Object.assign(this, {
            $mdDialog,
            ngbBlastSearchService,
            ngbBlastSearchFormConstants,
            projectContext
        });
        this.initialize();
    }

    async initialize() {
        this.searchResult = await this.ngbBlastSearchService.getCurrentSearchResult();
        this.searchResult.organisms = (this.searchResult.organisms || [])
            .map(o => `${o.scientificname} (taxid: ${o.taxid})`);
        this.searchResult.dbNames = (this.searchResult.db || []).map(db => db.name);
        this.setDefaultParams();
        this.isProgressShown = false;
    }

    setDefaultParams() {
        this.defaultParams = this.projectContext.getTrackDefaultSettings('blast_settings') || {};
        const defaultParamsKeyList = Object.keys((this.defaultParams || {}).options || {});
        const resultParamsKeyList = Object.keys(this.searchResult.parameters || {});
        const excludedOptions = {};
        const currentOptions = {};
        let key;
        this.paramsPartitions = [];
        resultParamsKeyList.forEach(key => {
            if (defaultParamsKeyList.includes(key)) {
                currentOptions[key] = {
                    value: this.searchResult.parameters[key]
                };
            } else {
                excludedOptions[key] = {
                    value: this.searchResult.parameters[key]
                };
            }
        });
        const currentOptionsKeyList = Object.keys(currentOptions);
        for (let index = 0; index < currentOptionsKeyList.length; index++) {
            key = currentOptionsKeyList[index];
            if (!defaultParamsKeyList.includes(key)) {
                excludedOptions[key] = {
                    value: this.searchResult.parameters[key]
                };
            } else {
                const partition = [{
                    key: key,
                    value: this.searchResult.parameters[key]
                }];
                if (currentOptionsKeyList[index + 1]) {
                    key = currentOptionsKeyList[index + 1];
                    partition.push({
                        key: key,
                        value: this.searchResult.parameters[key]
                    });
                    index++;
                }
                this.paramsPartitions.push(partition);
            }
        }
        this.options = this.ngbBlastSearchService.stringifySearchOptions(excludedOptions);
    }

    openQueryInfo() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ['sequence', '$mdDialog', function (sequence, $mdDialog) {
                this.sequence = sequence;
                this.close = $mdDialog.hide;
            }],
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: require('./ngbBlastSearchQueryDlg.tpl.html'),
            locals: {
                sequence: this.searchResult.sequence
            }
        });
    }
}
