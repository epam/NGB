import {EventGeneInfo, EventVariationInfo, PairReadInfo} from '../../../shared/utils/events';
import Clipboard from 'clipboard';

export default class ngbTrackEvents {

    _genomeDataService = null;
    _projectDataService = null;
    dispatcher;
    projectContext;

    constructor(dispatcher, projectContext, $scope, $compile, projectDataService, genomeDataService) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.$scope = $scope;
        this.$compile = $compile;
        this._projectDataService = projectDataService;
        this._genomeDataService = genomeDataService;
    }

    async _getGeneTracks(track) {
        if (!this.projectContext.project || track.instance.config.projectId !== this.projectContext.projectId) {
            const mapTrackFn = function(item) {
                return {
                    chromosomeId: track.instance.config.chromosomeId,
                    id: item.id,
                    name: item.name,
                    referenceId: item.referenceId
                };
            };
            return ((await this._projectDataService.getProject(track.instance.config.projectId)).items || [])
                .filter(item => item.format.toLowerCase() === 'gene').map(mapTrackFn);
        }
        return this.projectContext.geneTracks;
    }

    featureClick(trackInstance, data, track, event) {
        const isGeneTrack = track.format.toLowerCase() === 'gene';
        if (data.feature) {
            (async() => {
                let geneTracks = [];
                if (!isGeneTrack) {
                    geneTracks = await this._getGeneTracks(track);
                }
                else {
                    geneTracks.push({
                        chromosomeId: track.instance.config.chromosomeId,
                        id: track.id,
                        name: track.name,
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
                            referenceId: track.instance.config.referenceId,
                            startIndex: data.feature.startIndex
                        },
                        name: 'feature:info:select'
                    }],
                    title: 'Show Info'
                });
                if (geneTracks.length > 0) {
                    if (data.feature.attributes && data.feature.attributes.gene_id) {
                        menuData.push({
                            events: [{
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
            })();
        }
    }

    variationRequest(trackInstance, data: EventVariationInfo, track, event) {
        (async()=> {
            if (data) {
                if (data.endPoint && data.endPoint.chromosome) {
                    data.endPoint.chromosome = this.projectContext.getChromosome({name: data.endPoint.chromosome});
                }
                const menuData = [];
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

        const showInfo = {
            events: [
                {
                    data: {
                        chromosomeId: track.instance.config.chromosomeId,
                        endIndex: data.read.endIndex,
                        properties: data.info,
                        read: data.read,
                        referenceId: track.instance.config.referenceId,
                        startIndex: data.read.startIndex
                    },
                    name: 'feature:info:select'
                }
            ],
            title: 'Show info'
        };

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


        const copyToClipboard = {
            clipboard: data.info.map(line => line.join(' = ')).join('\r\n'),
            title: 'Copy info to clipboard'
        };

        const menuData = [];
        menuData.push(showInfo);
        if (hasPairRead && !chromosomeNotFoundError) {
            menuData.push(goToMateMenuItem);
            menuData.push(openMateMenuItem);
        }
        menuData.push(copyToClipboard);
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
    }

    static configureCopyToClipboardElements() {
        new Clipboard('.copy-to-clipboard-button');
    }

}
