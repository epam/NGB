import {EventGeneInfo, PairReadInfo} from '../../../shared/utils/events';
import Clipboard from 'clipboard';

export default class ngbTrackEvents {

    _genomeDataService = null;
    _projectDataService = null;
    dispatcher;
    projectContext;

    constructor(dispatcher, projectContext, $scope, $compile, projectDataService, genomeDataService, bamDataService, appLayout) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.$scope = $scope;
        this.$compile = $compile;
        this._projectDataService = projectDataService;
        this._genomeDataService = genomeDataService;
        this._bamDataService = bamDataService;
        this.appLayout = appLayout;
    }

    _getGeneTracks() {
        return (this.projectContext.reference && this.projectContext.reference.geneFile)
        ? [this.projectContext.reference.geneFile]
        : this.projectContext.geneTracks;
    }

    featureClick(trackInstance, data, track, event) {
        const isGeneTrack = track.format.toLowerCase() === 'gene';
        if (data.feature) {
            (async() => {
                let geneTracks = [];
                if (!isGeneTrack) {
                    geneTracks = this._getGeneTracks();
                }
                else {
                    geneTracks.push({
                        chromosomeId: track.instance.config.chromosomeId,
                        id: track.id,
                        name: track.name,
                        projectId: track.projectIdNumber || undefined,
                        referenceId: track.referenceId
                    });
                }
                const menuData = [];
                menuData.push({
                    events: [{
                        data: {
                            chromosomeId: track.instance.config.chromosomeId,
                            endIndex: data.feature.endIndex,
                            name: data.feature.name,
                            properties: data.info,
                            projectId: track.projectIdNumber || undefined,
                            referenceId: track.instance.config.referenceId,
                            startIndex: data.feature.startIndex,
                            geneId: (data.feature.attributes && data.feature.attributes.gene_id) ? data.feature.attributes.gene_id : null,
                            title: 'FEATURE'
                        },
                        name: 'feature:info:select'
                    }],
                    title: 'Show Info'
                });
                const blastSearchParams = {
                    geneId: (data.feature.attributes && data.feature.attributes.gene_id) ? data.feature.attributes.gene_id : null,
                    id: track.id,
                    chromosomeId: track.instance.config.chromosomeId,
                    referenceId: track.referenceId,
                    index: track.openByUrl ? track.indexPath : null,
                    startIndex: data.feature.startIndex,
                    endIndex: data.feature.endIndex,
                    name: data.feature.name,
                    file: track.openByUrl ? track.id : null,
                    openByUrl: track.openByUrl,
                };
                menuData.push({
                    title: 'BLASTn search',
                    submenu: [{
                        title:'Exon only',
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool:'blastn'
                            },
                            name: 'read:show:blast',
                        }]},
                    {
                        title: 'All transcript info',
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool:'blastn'
                            },
                            name: 'read:show:blast',
                        }]
                    }]
                });
                menuData.push({
                    submenu: [{
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool:'blastp'
                            },
                            name: 'read:show:blast',
                        }],
                        title:'Exon only',
                    },
                    {
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool:'blastp'
                            },
                            name: 'read:show:blast',
                        }],
                        title: 'All transcript info',
                    }],
                    title: 'BLASTp search',
                });
                if (geneTracks.length > 0) {
                    if (data.feature.attributes && data.feature.attributes.gene_id) {
                        const layoutChange = this.appLayout.Panels.molecularViewer;
                        layoutChange.displayed = true;
                        menuData.push({
                            events: [
                                {
                                    data: { layoutChange },
                                    name: 'layout:item:change'
                                },
                                {
                                    data: new EventGeneInfo({
                                        endIndex: data.feature.endIndex,
                                        geneId: data.feature.attributes.gene_id,
                                        geneTracks,
                                        highlight: false,
                                        startIndex: data.feature.startIndex,
                                        transcriptId: data.feature.attributes.transcript_id
                                    }),
                                    name: 'miew:show:structure'
                                }],
                            title: 'Show 3D structure'
                        });
                    }
                    // else {
                    //     menuData.push({
                    //         title: 'Highlight region on 3D structure',
                    //         events: [{
                    //             name: 'miew:highlight:region',
                    //             data: {
                    //                 geneTracks,
                    //                 startIndex: data.feature.startIndex,
                    //                 endIndex: data.feature.endIndex,
                    //                 highlight: true
                    //             }
                    //         }]
                    //     });
                    // }
                }
                const childScope = this.$scope.$new(false);
                childScope.menuData = menuData;
                const html = this.$compile('<ngb-track-menu menu-data="menuData"></ngb-track-menu>')(childScope);
                trackInstance.menuElement.show(
                    event.position, html
                );
                childScope.$apply();
                ngbTrackEvents.configureCopyToClipboardElements();
                html.find('#hiddenMenuButton').triggerHandler('click');
            })();
        }
    }

    variationRequest(trackInstance, data, track, event) {
        (async() => {
            if (data) {
                if (data.endPoint && data.endPoint.chromosome) {
                    data.endPoint.chromosome = this.projectContext.getChromosome({name: data.endPoint.chromosome});
                }
                const menuData = [];
                if (track.openByUrl) {
                    data.openByUrl = track.openByUrl;
                    data.fileUrl = track.id;
                    data.indexUrl = track.indexPath;
                }
                menuData.push({
                    events: [{
                        data: {variant: data},
                        name: 'variant:details:select'
                    }],
                    title: 'Show Info'
                });
                if (data.endPoint) {
                    menuData.push({
                        events: [{
                            data: {variant: data},
                            name: 'variant:show:pair'
                        }],
                        title: 'Show pair in split screen'
                    });
                }
                // let geneTracks = ((await this._projectDataService.getProject(track.instance.config.projectId)).items || [])
                // .filter(item => item.format.toLowerCase() === 'gene').map((item) => {
                //     return {
                //         id: item.id,
                //         name: item.name,
                //         referenceId: item.referenceId,
                //         chromosomeId: track.instance.config.chromosomeId
                //     };
                // });
                // if (geneTracks.length > 0) {
                //     menuData.push({
                //         title: 'Highlight region on 3D structure',
                //         events: [{
                //             name: 'miew:highlight:region',
                //             data: {
                //                 geneTracks,
                //                 startIndex: data.startIndex,
                //                 endIndex: data.endIndex
                //             }
                //         }]
                //     });
                // }
                const childScope = this.$scope.$new(false);
                childScope.menuData = menuData;
                const html = this.$compile('<ngb-track-menu menu-data="menuData"></ngb-track-menu>')(childScope);
                trackInstance.menuElement.show(
                    event.position, html
                );
                childScope.$apply();
            }
        })();
    }

    async readClick(trackInstance, data, track, event) {
        const hasPairRead = data.read.pnext !== undefined && data.read.pnext !== null && data.read.pnext !== 0;

        let pairReadParameters = {originalRead: data.read, position: data.read.pnext};

        let goToMateMenuItemEvent = null;
        let chromosomeNotFoundError = null;
        if (hasPairRead) {
            let chr = data.chromosome;
            let chromosomeChanged = false;
            if (data.read.rnext !== '=') {
                chr = this.projectContext.getChromosome({name: data.read.rnext});
                chromosomeChanged = true;
                if (!chr) {
                    chromosomeNotFoundError = `Chromosome ${data.read.rnext} not found`;
                }
            }
            if (!chromosomeNotFoundError) {
                pairReadParameters.endPoint = {
                    chromosome: chr.name,
                    position: data.read.pnext
                };
            } else {
                pairReadParameters = null;
            }

            if (!chromosomeChanged) {
                goToMateMenuItemEvent = {position: data.read.pnext};
            }
            else if (pairReadParameters) {
                goToMateMenuItemEvent = {
                    chromosome: {name: pairReadParameters.endPoint.chromosome},
                    position: data.read.pnext
                };
            }
        }

        const goToMateMenuItem = pairReadParameters ? {
            state: goToMateMenuItemEvent,
            title: goToMateMenuItemEvent ? 'Go to mate' : 'Chromosome not found'
        } : null;
        const openMateMenuItem = pairReadParameters ? {
            events: [{
                data: new PairReadInfo(pairReadParameters),
                name: 'read:show:mate'
            }],
            title: 'Open mate region in split view'
        } : null;
        const showInfo = {
            events: [
                {
                    data: {
                        chromosomeId: track.instance.config.chromosomeId,
                        endIndex: data.read.endIndex,
                        properties: data.info,
                        read: data.read,
                        referenceId: track.instance.config.referenceId,
                        startIndex: data.read.startIndex,
                        geneId: null,
                        title: 'ALIGNMENT',
                        infoForRead: {
                            id: track.id,
                            projectId: track.projectIdNumber || undefined,
                            chromosomeId: data.chromosome.id,
                            startIndex: data.read.startIndex,
                            endIndex: data.read.endIndex,
                            name: data.read.name,
                            openByUrl: track.openByUrl,
                            file: track.openByUrl ? track.id : null,
                            index: track.openByUrl ? track.indexPath : null
                        }
                    },
                    name: 'feature:info:select'
                }
            ],
            title: 'Show info'
        };

        const self = this;
        const copyToClipboard = {
            clipboard: 'Loading...',
            title: 'Copy info to clipboard',
            isLoading: true,
            fn: async function (menuItem) {
                const payload = {
                    id: track.id,
                    projectId: track.projectIdNumber || undefined,
                    chromosomeId: data.chromosome.id,
                    startIndex: data.read.startIndex,
                    endIndex: data.read.endIndex,
                    name: data.read.name,
                    openByUrl: track.openByUrl,
                    file: track.openByUrl ? track.id : null,
                    index: track.openByUrl ? track.indexPath : null
                };
                const read = await self._bamDataService.loadRead(payload);
                const generalInfo = data.info.map(line => line.join(' = ')).join('\r\n');
                const tags = read.tags.map(tag => `${tag.tag  } = ${  tag.value}`).join('\r\n');

                menuItem.clipboard = `${generalInfo  }\r\n\r\n${  read.sequence  }\r\n\r\n${  tags}`;
                menuItem.isLoading = false;
                self.$scope.$apply();
            }
        };

        const copySequenceToClipboard = {
            clipboard: 'Loading...',
            title: 'Copy sequence to clipboard',
            isLoading: true,
            fn: async function (menuItem) {
                const payload = {
                    id: track.id,
                    projectId: track.projectIdNumber || undefined,
                    chromosomeId: data.chromosome.id,
                    startIndex: data.read.startIndex,
                    endIndex: data.read.endIndex,
                    name: data.read.name,
                    openByUrl: track.openByUrl,
                    file: track.openByUrl ? track.id : null,
                    index: track.openByUrl ? track.indexPath : null
                };
                const read = await self._bamDataService.loadRead(payload);
                menuItem.clipboard = read.sequence;
                menuItem.isLoading = false;
                self.$scope.$apply();
            }
        };
        const readInfo = {
            geneId:null,
            id: track.id,
            referenceId: track.referenceId,
            chromosomeId: data.chromosome.id,
            startIndex: data.read.startIndex,
            endIndex: data.read.endIndex,
            name: data.read.name,
            openByUrl: track.openByUrl,
            file: track.openByUrl ? track.id : null,
            index: track.openByUrl ? track.indexPath : null
        };
        const openBlatSearchMenuItem = {
            title: 'BLAT Search',
            events: [{
                data: {...readInfo},
                name: 'read:show:blat',
            }],
        };
        const openBlastnSearchMenuItem = {
            events: [{
                data: {...readInfo, tool:'blastn'},
                name: 'read:show:blast',
            }],
            title: 'BLASTn Search',
        };

        const menuData = [];
        menuData.push(showInfo);
        if (hasPairRead && !chromosomeNotFoundError) {
            menuData.push(goToMateMenuItem);
            menuData.push(openMateMenuItem);
        }
        menuData.push(openBlatSearchMenuItem);
        menuData.push(openBlastnSearchMenuItem);
        menuData.push(copyToClipboard);
        menuData.push(copySequenceToClipboard);
        if (chromosomeNotFoundError) {
            menuData.push({
                title: chromosomeNotFoundError
            });
        }

        const childScope = this.$scope.$new(false);
        childScope.menuData = menuData;
        const html = this.$compile('<ngb-track-menu menu-data="menuData"></ngb-track-menu>')(childScope);
        trackInstance.menuElement.show(
            event.position, html
        );
        childScope.$apply();
        ngbTrackEvents.configureCopyToClipboardElements();
        html.find('#hiddenMenuButton').triggerHandler('click');
    }

    static configureCopyToClipboardElements() {
        new Clipboard('.copy-to-clipboard-button');
    }
}
