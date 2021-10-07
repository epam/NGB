import Clipboard from 'clipboard';
import {EventGeneInfo, PairReadInfo} from '../../../shared/utils/events';

import {EventGeneInfo, PairReadInfo} from '../../../shared/utils/events';

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
        const blastSettings = this.projectContext.getTrackDefaultSettings('blast_settings');
        const maxQueryLengthProperty = 'query_max_length';
        const maxSequenceLength = blastSettings &&
        blastSettings.hasOwnProperty(maxQueryLengthProperty) &&
        !Number.isNaN(Number(blastSettings[maxQueryLengthProperty]))
            ? Number(blastSettings[maxQueryLengthProperty])
            : Infinity;
        if (data.feature) {
            const featureSize = Math.abs((data.feature.startIndex || 0) - (data.feature.endIndex || 0));
            (async () => {
                let geneTracks = [];
                if (!isGeneTrack) {
                    geneTracks = this._getGeneTracks();
                } else {
                    geneTracks.push({
                        chromosomeId: trackInstance.config.chromosomeId,
                        id: track.id,
                        name: track.name,
                        projectId: track.projectIdNumber || undefined,
                        referenceId: track.referenceId
                    });
                }
                const menuData = [];
                const featureAttributes = data.feature.attributes;
                if (!data.feature.grouping) {
                    const getChrName = (id) => {
                        const chrIndex =  this.projectContext.chromosomes.findIndex(chr => chr.id === id);
                        return chrIndex > -1 &&
                            this.projectContext.chromosomes[chrIndex] &&
                            this.projectContext.chromosomes[chrIndex].name;
                    };
                    menuData.push({
                        events: [{
                            data: {
                                chromosomeId: trackInstance.config.chromosomeId,
                                seqName: data.feature.seqName || getChrName(trackInstance.config.chromosomeId),
                                endIndex: data.feature.endIndex,
                                name: data.feature.name,
                                properties: data.info,
                                projectId: track.projectIdNumber || undefined,
                                referenceId: track.referenceId,
                                startIndex: data.feature.startIndex,
                                geneId: (featureAttributes && featureAttributes.gene_id) || null,
                                title: 'FEATURE',
                                fileId: track.id,
                                uuid: data.feature.uid
                            },
                            name: 'feature:info:select'
                        }],
                        title: 'Show Info'
                    });
                }
                if (/^(gene|transcript|mrna|cds)$/i.test(data.feature.feature)) {
                    let featureType = data.feature.feature;
                    if (/^(transcript)$/i.test(featureType)) {
                        featureType = 'mRNA';
                    }
                    const blastSearchParams = {
                        geneId: (data.feature.attributes && data.feature.attributes.gene_id)
                            ? data.feature.attributes.gene_id
                            : null,
                        id: track.id,
                        chromosomeId: trackInstance.config.chromosomeId,
                        referenceId: track.referenceId,
                        index: track.openByUrl ? track.indexPath : null,
                        startIndex: data.feature.startIndex,
                        endIndex: data.feature.endIndex,
                        name: data.feature.name,
                        file: track.openByUrl ? track.id : null,
                        openByUrl: track.openByUrl,
                        feature: featureType
                    };
                    menuData.push({
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool: 'blastn',
                                source: 'gene'
                            },
                            name: 'read:show:blast'
                        }],
                        disabled: maxSequenceLength < featureSize,
                        warning: maxSequenceLength < featureSize
                            ? `Query maximum length (${maxSequenceLength}bp) exceeded`
                            : undefined,
                        title: 'BLASTn search',
                    });
                    menuData.push({
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool: 'blastp',
                                aminoAcid: true,
                                source: 'gene'
                            },
                            name: 'read:show:blast'
                        }],
                        disabled: !blastSearchParams.name,
                        warning: !blastSearchParams.name
                            ? 'Feature name is missing'
                            : undefined,
                        title: 'BLASTp search',
                    });
                } else if (
                    !data.feature.grouping &&
                    data.feature.startIndex &&
                    data.feature.endIndex
                ) {
                    const blastSearchParams = {
                        id: track.id,
                        chromosomeId: trackInstance.config.chromosomeId,
                        referenceId: track.referenceId,
                        index: track.openByUrl ? track.indexPath : null,
                        startIndex: data.feature.startIndex,
                        endIndex: data.feature.endIndex,
                        name: data.feature.name,
                        file: track.openByUrl ? track.id : null,
                        openByUrl: track.openByUrl,
                        feature: data.feature.feature
                    };
                    menuData.push({
                        title: 'BLASTn search',
                        disabled: maxSequenceLength < featureSize,
                        warning: maxSequenceLength < featureSize
                            ? `Query maximum length (${maxSequenceLength}bp) exceeded`
                            : undefined,
                        events: [{
                            data: {
                                ...blastSearchParams,
                                tool: 'blastn',
                                source: track.format
                            },
                            name: 'read:show:blast',
                        }]
                    });
                }
                if (geneTracks.length > 0) {
                    if (data.feature.attributes && data.feature.attributes.gene_id) {
                        const layoutChange = this.appLayout.Panels.molecularViewer;
                        layoutChange.displayed = true;
                        menuData.push({
                            events: [
                                {
                                    name: 'miew:clear:structure'
                                },
                                {
                                    data: {layoutChange},
                                    name: 'layout:item:change'
                                },
                                {
                                    data: new EventGeneInfo({
                                        endIndex: data.feature.endIndex,
                                        geneId: data.feature.attributes.gene_id,
                                        geneName: data.feature.attributes.gene_name,
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
                }
                if (data.feature.feature
                    && data.feature.feature.toLowerCase() === 'gene'
                    && data.feature.name) {
                    const layoutChange = this.appLayout.Panels.homologs;
                    layoutChange.displayed = true;
                    menuData.push({
                        events: [
                            {
                                data: {layoutChange},
                                name: 'layout:item:change'
                            },
                            {
                                data: {
                                    search: data.feature.name,
                                    featureId: data.feature.groupId
                                },
                                name: 'read:show:homologs'
                            }],
                        title: 'Show similar genes',
                        disabled: (data.feature.feature || '').toLowerCase() !== 'gene' || !data.feature.name,
                        warning: (data.feature.feature || '').toLowerCase() !== 'gene' || !data.feature.name
                            ? 'Feature type is not Gene or Name is missing'
                            : undefined,
                    });
                }
                if (menuData.length > 0) {
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
            })();
        }
    }

    variationRequest(trackInstance, data, track, event) {
        (async () => {
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
                const childScope = this.$scope.$new(false);
                childScope.menuData = menuData;
                const html = this.$compile('<ngb-track-menu menu-data="menuData"></ngb-track-menu>')(childScope);
                trackInstance.menuElement.show(
                    event.position, html
                );
                childScope.$apply();
                html.find('#hiddenMenuButton').triggerHandler('click');
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
            } else if (pairReadParameters) {
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
                const tags = read.tags.map(tag => `${tag.tag} = ${tag.value}`).join('\r\n');

                menuItem.clipboard = `${generalInfo}\r\n\r\n${read.sequence}\r\n\r\n${tags}`;
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
            geneId: null,
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
                data: {
                    ...readInfo,
                    tool: 'blastn',
                    source: 'bam'
                },
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
