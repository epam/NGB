import {stringParseInt} from '../utils/Int';
import {Node} from '../../components/ngbDataSets/internal';

export default class ngbApiService {

    static instance($state, projectContext, ngbDataSetsService, $mdDialog, localDataService, dispatcher) {
        return new ngbApiService($state, projectContext, ngbDataSetsService, $mdDialog, localDataService, dispatcher);
    }

    projectContext;
    ngbDataSetsService;
    $mdDialog;
    localDataService;
    dispatcher;
    datasets;

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
                    isSuccessful: false
                };}
                return this._select(item, true, datasets);
            })
        } else {
            const [item] = datasets.filter(m => m.id === id);
            if(!item){ return {
                message: `No dataset with id = ${id}`,
                isSuccessful: false
            };}
            return this._select(item, true, datasets);
        }
    }

    navigateToCoordinate(coordinates) {
        if (!this.projectContext.tracks.length) {
            return {
                message: 'No tracks selected.',
                isSuccessful: false
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
                    isSuccessful: true
                };
            }
            return {
                message: 'No coordinates provided',
                isSuccessful: false
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
                isSuccessful: false
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
            isSuccessful: true
        };

    }

    setGlobalSettings(params){
        let settings = this.localDataService.getSettingsCopy();
        settings = this._mergeDeep(settings, params);
        this.localDataService.updateSettings(settings);
        this.dispatcher.emitGlobalEvent('settings:change', settings);
        return {
            message: 'Ok',
            isSuccessful: true
        };
    }

    loadTrack(track, mode) {

        //check track request mode
        switch (mode) {
            case 'id':
                const trackId = stringParseInt(track);
                if (trackId) {
                    return this._selectTrackById(trackId);
                } else {
                    return {
                        message: 'No track id specified.',
                        isSuccessful: false
                    };
                }
                break;

            case 'url':
            case 'ngbServer':

                if (!track.entities || !track.referenceId) {
                    return {
                        message: 'Not enough params specified',
                        isSuccessful: false,
                    }
                }

                let errors = [],
                    references = this.projectContext.references,
                    [chosenReference] = references.filter(r => r.id === track.referenceId),
                    selectedItems = [];

                if(!chosenReference) {
                    return {
                        message: 'Reference not found',
                        isSuccessful: false,
                    }
                }

                selectedItems = (track.entities || []).map(t => {
                        let shouldReturn = t.index ? true : !this._trackNeedsIndexFile(t);

                        if (shouldReturn) {
                            return {
                                index: t.index ? t.index : null,
                                path: t.path,
                                reference: chosenReference,
                                format: this._trackFormat(t),
                                name: this._fileName(t),
                            };
                        } else {
                            errors.push(`Index file required for ${t.path}.`);
                        }
                    });

                this._loadTracks(selectedItems);

                return {
                  message: errors.length ? errors.join(' | ') : 'Ok',
                  isSuccessful: !errors.length
                };

                break;
        }

        return {
            message: 'Something went wrong.',
            isSuccessful: false
        };

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
                case 'bam': return 'BAM';
                case 'vcf': return 'VCF';
                case 'bed': return 'BED';
                case 'gff': return 'GENE';
                case 'gff3': return 'GENE';
                case 'gtf': return 'GENE';
            }
        }
        return null;
    }

    _loadTracks(selectedFiles) {
        const mapFn = (selectedFile) => {
            // console.log(selectedFile);//todo remove log
            return {
                openByUrl: true,
                isLocal: true,
                format: selectedFile.format,
                id: selectedFile.path,
                bioDataItemId: selectedFile.path,
                referenceId: selectedFile.reference.id,
                reference: selectedFile.reference,
                name: selectedFile.path,
                indexPath: selectedFile.index,
                projectId: selectedFile.reference.name
            };
        };
        const mapTrackStateFn = (t) => {
            return {
                bioDataItemId: t.name,
                name: t.name,
                index: t.indexPath,
                isLocal: true,
                format: t.format,
                projectId: t.projectId
            };
        };
        let tracks = selectedFiles.map(mapFn).filter(
            track => this.projectContext.tracks.filter(t => t.isLocal && t.name.toLowerCase() === track.name.toLowerCase()).length === 0);

        if (tracks.length === 0) {
            return;
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
    }

    _getAllDataSets() {
        this.datasets = this.projectContext.datasets;
        if (!this.datasets || this.datasets.length === 0) {
            this.projectContext.refreshDatasets().then(async () => {
                this.datasets = await this.ngbDataSetsService.getDatasets();
                if(!this.datasets.length) {
                    return null;
                }
            });
        } else {
            if(!this.datasets) {
                return null;
            }
        }

        return this.datasets;
    }

    _selectTrackById(id) {

        this._getAllDataSets();

        const tracks = [];
        const findTrackFn = function (item: Node) {
            if(item.nestedProjects && item.nestedProjects.length > 0) {
                for(let i = 0; i < item.nestedProjects.length; i++) {
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
        for (let i = 0; i < this.datasets.length; i++) {
            findTrackFn(this.datasets[i]);
        }

        const [selectedTrack] = tracks;
        // console.log(this.datasets);//todo remove log

        if (selectedTrack) {

            const trackParents = [];
            const getTrackParents = function (track: Node) {
                if(track.project) {
                    trackParents.push(track.project);
                    getTrackParents(track.project);
                }
            };
            getTrackParents(selectedTrack);

            trackParents.reverse().map(parent => {
                if (parent) {
                    parent.__expanded = !parent.__expanded;
                    this.ngbDataSetsService.toggle(parent);
                }
            });

            // console.log(selectedTrack);//todo remove log

            return this._toggleSelected(selectedTrack);

        } else {
            return {
                message: 'No tracks found with id specified.',
                isSuccessful: false
            };
        }
    }

    _toggleSelected(node) {
        node.__selected = !node.__selected;
        return this._toggle(node);
    }

    _toggle(node) {
        // console.log(node);//todo remove log
        if (node.__selected) {
            node.__expanded = true;
        }
        if (node.isProject && !node.__selected) {
            this.ngbDataSetsService.deselectItem(node);
        }

        return this._select(node, node.__selected, this.datasets);
    }

    _select(item, isSelected, tree) {
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
            isSuccessful: true
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
