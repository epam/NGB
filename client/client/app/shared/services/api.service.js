import {Node} from '../../components/ngbDataSets/internal';
import {stringParseInt} from '../utils/Int';

export default class ngbApiService {

    static instance($state,
                    projectContext,
                    ngbDataSetsService,
                    $mdDialog,
                    localDataService,
                    dispatcher) {
        return new ngbApiService(
            $state,
            projectContext,
            ngbDataSetsService,
            $mdDialog,
            localDataService,
            dispatcher);
    }

    projectContext;
    ngbDataSetsService;
    $mdDialog;
    localDataService;
    dispatcher;
    datasets;
    references;

    constructor($state, projectContext, ngbDataSetsService, $mdDialog, localDataService, dispatcher) {
        Object.assign(this, {
            $mdDialog,
            $state,
            dispatcher,
            localDataService,
            ngbDataSetsService,
            projectContext,
        });
        (async () => {
            await this.projectContext.refreshReferences(true);
            this.references = this.projectContext.references;
        })();
    }

    /** returns a Promise */
    async loadDataSet(id, forceSwitchRef = false) {
        let datasets = this.projectContext.datasets;
        if (!datasets || datasets.length === 0) {
            await this.projectContext.refreshDatasets();
            datasets = await this.ngbDataSetsService.getDatasets();
            const [item] = datasets.filter(m => m.id === id);
            if (!item) {
                return Promise.resolve({
                    isSuccessful: false,
                    message: `No dataset with id = ${id}`
                });
            }
            return this._select(item, true, datasets, forceSwitchRef);
        } else {
            const [item] = datasets.filter(m => m.id === id);
            if (!item) {
                return Promise.resolve({
                    isSuccessful: false,
                    message: `No dataset with id = ${id}`
                });
            }
            return this._select(item, true, datasets, forceSwitchRef);
        }
    }

    navigateToCoordinate(coordinates) {
        if (!this.projectContext.tracks.length) {
            return {
                isSuccessful: false,
                message: 'No tracks selected.'
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
                    isSuccessful: true,
                    message: 'Ok'
                };
            }
            return {
                isSuccessful: false,
                message: 'No coordinates provided'
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
                isSuccessful: false,
                message: 'No chromosome found'
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
            isSuccessful: true,
            message: 'Success.'
        };

    }

    setGlobalSettings(params) {
        let settings = this.localDataService.getSettingsCopy();
        settings = this._mergeDeep(settings, params);
        this.localDataService.updateSettings(settings);
        this.dispatcher.emitGlobalEvent('settings:change', settings);
        return {
            isSuccessful: true,
            message: 'Ok'
        };
    }

    setTrackSettings(params) {
        const trackId = params ? params.id : null;
        const tracks = this.projectContext.tracks;

        if (trackId) {
            let isSelectedTrack = false;
            for (let i = 0; i < tracks.length; i++) {
                if (tracks[i].bioDataItemId === trackId) {
                    isSelectedTrack = true;
                }
            }

            if (isSelectedTrack) {
                this.dispatcher.emitGlobalEvent('trackSettings:change', params);
                return {
                    isSuccessful: true,
                    message: 'Ok'
                };
            } else {
                return {
                    isSuccessful: false,
                    message: 'No selected tracks found with id specified.'
                };
            }
        } else {
            return {
                isSuccessful: false,
                message: 'No track id'
            };
        }
    }

    /** returns a Promise */
    toggleSelectTrack(params) {
        const forceSwitchRef = params.forceSwitchRef ? params.forceSwitchRef : false;
        if (params.track) {
            return this._selectTrackById(params.track, forceSwitchRef);
        } else {
            return new Promise((resolve) => {
                resolve({
                    isSuccessful: false,
                    message: 'No tracks ids specified.'
                });
            });
        }
    }

    /** returns a Promise */
    loadTracks(params) {
        const forceSwitchRef = params.forceSwitchRef ? params.forceSwitchRef : false;

        if (!params.tracks || !params.referenceId) {
            return new Promise((resolve) => {
                resolve({
                    isSuccessful: false,
                    message: 'Not enough params specified',
                });
            });
        }

        const [chosenReference] = this.references.filter(r => r.id === params.referenceId);

        if (!chosenReference) {
            return new Promise((resolve) => {
                resolve({
                    isSuccessful: false,
                    message: 'Reference not found',
                });
            });
        }

        const switchingReference = this.projectContext.reference && this.projectContext.reference.id !== +chosenReference.id;

        if (!forceSwitchRef && switchingReference) {
            const confirm = this.$mdDialog.confirm()
                .title(`Switch reference ${this.projectContext.reference ? this.projectContext.reference.name : ''}${chosenReference ? ` to ${chosenReference.name}` : ''}?`)
                .textContent('All opened tracks will be closed.')
                .ariaLabel('Change reference')
                .ok('OK')
                .cancel('Cancel');

            return this.$mdDialog.show(confirm)
                .then(() => this._processTracks(params.tracks, chosenReference))
                .catch(() => ({
                    isSuccessful: false,
                    message: 'Aborted by user'
                }));
        } else {
            return new Promise((resolve) => {
                resolve(this._processTracks(params.tracks, chosenReference));
            });
        }
    }

    setToken(token){
        localStorage.setItem('token', token);
        return {
            isSuccessful: true,
            message: 'Ok'
        };
    }

    _processTracks(tracks, chosenReference) {
        const errors = [];

        const selectedItems = (tracks || []).map(t => {
            const shouldReturn = t.index ? true : !this._trackNeedsIndexFile(t);

            if (shouldReturn) {
                return {
                    format: this._trackFormat(t),
                    index: t.index ? t.index : null,
                    name: this._fileName(t),
                    path: t.path,
                    reference: chosenReference,
                };
            } else {
                errors.push(`Index file required for ${t.path}.`);
            }
            return false;
        }).filter(i => !!i);

        if (errors.length) {
            return {
                isSuccessful: !errors.length,
                message: errors.length ? errors.join(' | ') : 'Ok'
            };
        }

        return this._loadTracks(selectedItems);
    }

    _trackNeedsIndexFile(track) {
        const extension = this._fileExtension(track);
        return extension === 'bam' ||
            extension === 'vcf' ||
            extension === 'bed' ||
            extension === 'gff' ||
            extension === 'gff3' ||
            extension === 'gtf';

    }

    _fileExtension(track) {
        if (!track.path || !track.path.length) {
            return null;
        }
        const listForCheckingFileType = track.path.split('.');
        if (listForCheckingFileType[listForCheckingFileType.length - 1].toLowerCase() === 'gz') {
            listForCheckingFileType.splice(listForCheckingFileType.length - 1, 1);
        }
        return listForCheckingFileType[listForCheckingFileType.length - 1].toLowerCase();
    }

    _fileName(track) {
        if (!track.path || !track.path.length) {
            return null;
        }
        let list = track.path.split('/');
        list = list[list.length - 1].split('\\');
        return list[list.length - 1];
    }

    _trackFormat(track) {
        const extension = this._fileExtension(track);
        if (extension) {
            switch (extension.toLowerCase()) {
                case 'bam':
                    return 'BAM';
                case 'vcf':
                    return 'VCF';
                case 'bed':
                    return 'BED';
                case 'gff':
                    return 'GENE';
                case 'gff3':
                    return 'GENE';
                case 'gtf':
                    return 'GENE';
            }
        }
        return null;
    }

    _loadTracks(selectedFiles) {
        const mapFn = (selectedFile) => ({
            bioDataItemId: selectedFile.path,
            format: selectedFile.format,
            id: selectedFile.path,
            indexPath: selectedFile.index,
            isLocal: true,
            name: selectedFile.path,
            openByUrl: true,
            projectId: selectedFile.reference.name,
            reference: selectedFile.reference,
            referenceId: selectedFile.reference.id,
        });
        const mapTrackStateFn = (t) => ({
            bioDataItemId: t.name,
            format: t.format,
            index: t.indexPath,
            isLocal: true,
            name: t.name,
            projectId: t.projectId
        });
        const tracks = selectedFiles.map(mapFn).filter(
            track => this.projectContext.tracks.filter(t => t.isLocal && t.name.toLowerCase() === track.name.toLowerCase()).length === 0);

        if (tracks.length === 0) {
            return {
                isSuccessful: true,
                message: 'Ok',
            };
        }

        for (let i = 0; i < tracks.length; i++) {
            this.projectContext.addLastLocalTrack(tracks[i]);
        }

        const [reference] = tracks.map(t => t.reference);
        reference.projectId = reference.name;
        reference.isLocal = true;

        if (!this.projectContext.reference || this.projectContext.reference.bioDataItemId !== reference.bioDataItemId) {
            const _tracks = [reference, ...tracks];
            const tracksState = _tracks.map(mapTrackStateFn);
            this.projectContext.changeState({reference, tracks: _tracks, tracksState, shouldAddAnnotationTracks: true});
        } else {
            const _tracks = [...this.projectContext.tracks, ...tracks];
            const tracksState = [...(this.projectContext.tracksState || []), ...tracks.map(mapTrackStateFn)];
            this.projectContext.changeState({reference: this.projectContext.reference, tracks: _tracks, tracksState});
        }

        return {
            isSuccessful: true,
            message: 'Ok',
        };
    }

    _getAllDataSets() {
        let datasets = this.projectContext.datasets;
        if (!datasets || datasets.length === 0) {
            this.projectContext.refreshDatasets().then(async () => {
                datasets = await this.ngbDataSetsService.getDatasets();
                if (!datasets.length) {
                    return null;
                }
            });
        } else {
            if (!datasets) {
                return null;
            }
        }

        return datasets;
    }

    _selectTrackById(id, forceSwitchRef = false) {
        const datasets = this._getAllDataSets();

        const tracks = [];
        const findTrackFn = function (item: Node) {
            if (item.nestedProjects && item.nestedProjects.length > 0) {
                for (let i = 0; i < item.nestedProjects.length; i++) {
                    findTrackFn(item.nestedProjects[i]);
                }
            }
            if (item.tracks.length > 0) {
                for (let i = 0; i < item.tracks.length; i++) {
                    if (item.tracks[i] &&
                        item.tracks[i].format !== 'REFERENCE' &&
                        item.tracks[i].bioDataItemId === id) {
                        tracks.push(item.tracks[i]);
                    }
                }
            }
        };
        for (let i = 0; i < datasets.length; i++) {
            findTrackFn(datasets[i]);
        }

        const [selectedTrack] = tracks;

        if (selectedTrack) {
            const trackParents = [];
            const getTrackParents = function (track: Node) {
                if (track.project) {
                    trackParents.push(track.project);
                    getTrackParents(track.project);
                }
            };
            getTrackParents(selectedTrack);

            trackParents.reverse().forEach(parent => {
                if (parent) {
                    parent.__expanded = !parent.__expanded;
                    this.ngbDataSetsService.toggle(parent);
                }
            });

            return this._toggleSelected(datasets, selectedTrack, forceSwitchRef);
        } else {
            return new Promise((resolve) => {
                resolve({
                    isSuccessful: false,
                    message: 'No tracks found with id specified.'
                });
            });
        }
    }

    _toggleSelected(datasets, node, forceSwitchRef = false) {
        node.__selected = !node.__selected;

        return this._toggle(datasets, node, forceSwitchRef);
    }

    _toggle(datasets, node, forceSwitchRef = false) {
        if (node.__selected) {
            node.__expanded = true;
        }
        if (node.isProject && !node.__selected) {
            this.ngbDataSetsService.deselectItem(node);
        }

        return this._select(node, node.__selected, datasets, forceSwitchRef);
    }

    _select(item, isSelected, tree, forceSwitchRef = false) {
        if (!this.ngbDataSetsService.checkSelectionAvailable(item, isSelected) && !forceSwitchRef) {
            const reference = this.ngbDataSetsService.getItemReference(item);
            this.ngbDataSetsService.deselectItem(item);
            const confirm = this.$mdDialog.confirm()
                .title(`Switch reference ${this.projectContext.reference ? this.projectContext.reference.name : ''}${reference ? ` to ${reference.name}` : ''}?`)
                .textContent('All opened tracks will be closed.')
                .ariaLabel('Change reference')
                .ok('OK')
                .cancel('Cancel');

            return this.$mdDialog.show(confirm).then(() => {
                this.ngbDataSetsService.selectItem(item, isSelected, tree);

                return {
                    isSuccessful: true,
                    message: 'Ok'
                };
            }).catch(() => ({
                isSuccessful: false,
                message: 'Aborted by user'
            }));

        } else {
            return new Promise((resolve) => {
                this.ngbDataSetsService.selectItem(item, isSelected, tree);

                resolve({
                    isSuccessful: true,
                    message: 'Ok'
                });
            });
        }
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
                    if (!target[key]) Object.assign(target, {[key]: {}});
                    this._mergeDeep(target[key], source[key]);
                } else {
                    Object.assign(target, {[key]: source[key]});
                }
            }
        }

        return this._mergeDeep(target, ...sources);
    }
}
