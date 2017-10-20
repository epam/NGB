import {stringParseInt} from '../utils/Int';

export default class ngbApiService {

    static instance($state, projectContext, ngbDataSetsService, $mdDialog, localDataService, dispatcher) {
        return new ngbApiService($state, projectContext, ngbDataSetsService, $mdDialog, localDataService, dispatcher);
    }

    projectContext;
    ngbDataSetsService;
    $mdDialog;
    localDataService;
    dispatcher;

    constructor($state, projectContext, ngbDataSetsService, $mdDialog, localDataService, dispatcher) {
        Object.assign(this, {
            $state,
            projectContext,
            ngbDataSetsService,
            $mdDialog,
            localDataService,
            dispatcher
        });
    }

    loadDataSet(id) {
        let datasets = this.projectContext.datasets;
        if (!datasets || datasets.length === 0) {
            this.projectContext.refreshDatasets().then(async () => {
                datasets = await this.ngbDataSetsService.getDatasets();
                const [item] = datasets.filter(m => m.id === id);
                if(!item){ return {
                    message: `No dataset with id = ${id}`,
                    completedSuccessfully: false
                };}
                return this._selectDataset(item, true, datasets);
            })
        } else {
            const [item] = datasets.filter(m => m.id === id);
            if(!item){ return {
                message: `No dataset with id = ${id}`,
                completedSuccessfully: false
            };}
            return this._selectDataset(item, true, datasets);
        }
    }

    navigateToCoordinate(coordinates) {
        if (!this.projectContext.tracks.length) {
            return {
                message: 'No tracks selected.',
                completedSuccessfully: false
            };
        }

        const coordinatesText = coordinates;

        let chrName = null;
        let start = null;
        let end = null;

        //1. '' - should open specified chromosome and range
        if (!coordinatesText) {
            if (this.projectContext.currentChromosome) {
                [start, end] = [1, this.projectContext.currentChromosome.size];
                this.projectContext.changeState({viewport: {end, start}});
                return {
                    message: 'Ok',
                    completedSuccessfully: true
                };
            }
            return {
                message: 'No coordinates provided',
                completedSuccessfully: false
            };
        }

        // 'x : start-stop' - default
        //'5 : 217 - 7726'.match(/(([0-9a-zA-Z]*)\D*\:)\D*(\d*)\D*\-\D*(\d*)/)
        //['5 : 217 - 7726', '5 :', '5', '217', '7726']
        const regexp_1 = /(([0-9a-zA-Z\D]*)\D*\:)\D*([0-9,. ]*)\D*\-\D*([0-9,. ]*)/;

        //2. 'start-stop' - should open specified range on current chromosome
        const regexp_2 = /^([0-9,. ]*)\D*\-\D*([0-9,. ]*)$/;

        //3. 'chr:start' - should open specified chromosome and range from start-50bp to start+50bp
        const regexp_3 = /^([0-9a-zA-Z\D]*)\D*\:\D*([0-9,. ]*)$/;

        //4. 'start' - should open range from start-50bp to start+50bp on current chromosome
        const regexp_4 = /^[0-9,. ]+$/;

        //5. 'chr:' - should open specified chromosome and range from 1 to end of chromosome
        const regexp_5 = /^([0-9a-zA-Z\D]*)\D*:$/;

        if (regexp_1.test(coordinatesText)) {
            [, , chrName, start, end] = coordinatesText.match(regexp_1);
        } else if (regexp_2.test(coordinatesText)) {
            [, start, end] = coordinatesText.match(regexp_2);
        } else if (regexp_5.test(coordinatesText)) {
            [, chrName] = coordinatesText.match(regexp_5);
        } else if (regexp_3.test(coordinatesText)) {
            [, chrName, start] = coordinatesText.match(regexp_3);
        } else if (regexp_4.test(coordinatesText)) {
            [start] = coordinatesText.match(regexp_4);
        }

        if (chrName) {
            chrName = chrName.trim();
        }

        const chr = (!chrName || (this.projectContext.currentChromosome && this.projectContext.currentChromosome.name === chrName))
            ? this.projectContext.currentChromosome
            : this.projectContext.getChromosome({name: chrName});

        if (!chr) {
            return {
                message: 'No chromosome specified',
                completedSuccessfully: false
            };
        }

        if (start) {
            start = stringParseInt(start);
            if (start < 1 || (end !== null && start > end) || start > chr.size) {
                start = 1;
            }

            if (end) {
                end = stringParseInt(end);
                if (end > chr.size) {
                    end = chr.size;
                }
            }
        } else {
            start = 1;
            end = chr.size;
        }

        const viewport = (start && end) ? {end, start} : null;
        const position = (start && !end) ? start : null;

        this.projectContext.changeState({chromosome: {name: chr.name}, position, viewport});

        return {
            message: 'Success.',
            completedSuccessfully: true
        };

    }

    setGlobalSettings(params){
        let settings = this.localDataService.getSettingsCopy();
        settings = this._mergeDeep(settings, params);
        this.localDataService.updateSettings(settings);
        this.dispatcher.emitGlobalEvent('settings:change', settings);
        return {
            message: 'Ok',
            completedSuccessfully: true
        };
    }

    setTrackSettings(params){
        this.dispatcher.emitGlobalEvent('trackSettings:change', params);
        return {
            message: 'Ok',
            completedSuccessfully: true
        };
    }

    _selectDataset(item, isSelected, tree) {
        const self = this;
        if (!this.ngbDataSetsService.checkSelectionAvailable(item, isSelected)) {
            const reference = this.ngbDataSetsService.getItemReference(item);
            this.ngbDataSetsService.deselectItem(item);
            const confirm = this.$mdDialog.confirm()
                .title(`Switch reference ${this.projectContext.reference ? this.projectContext.reference.name : ''}${reference ? ` to ${reference.name}` : ''}?`)
                .textContent('All opened tracks will be closed.')
                .ariaLabel('Change reference')
                .ok('OK')
                .cancel('Cancel');
            this.$mdDialog.show(confirm).then(function () {
                self.ngbDataSetsService.selectItem(item, isSelected, tree);
            });
        } else {
            this.ngbDataSetsService.selectItem(item, isSelected, tree);
        }
        return {
            message: 'Ok',
            completedSuccessfully: true
        };
    }

    _isObject(item) {
        return (item && typeof item === 'object' && !Array.isArray(item));
    }

    _mergeDeep(target, ...sources) {
        if (!sources.length) return target;
        const source = sources.shift();

        if (this._isObject(target) && this._isObject(source)) {
            for (const key in source) {
                if (this._isObject(source[key])) {
                    if (!target[key]) Object.assign(target, { [key]: {} });
                    this._mergeDeep(target[key], source[key]);
                } else {
                    Object.assign(target, { [key]: source[key] });
                }
            }
        }

        return this._mergeDeep(target, ...sources);
    }
}
