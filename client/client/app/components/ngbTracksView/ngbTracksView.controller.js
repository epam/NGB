import {ngbTracksViewZoomManager, ngbTracksViewCameraManager} from './managers';
import Promise from 'bluebird';
import {Viewport} from '../../../modules/render/';
import angular from 'angular';
import baseController from '../../shared/baseController';
import {getFormattedDate} from '../../shared/utils/date';
import NotificationsContext from '../../shared/notificationsContext';

const RULER_TRACK_FORMAT = 'RULER';

export default class ngbTracksViewController extends baseController {
    zoomManager: ngbTracksViewZoomManager;
    bookmarkCamera: ngbTracksViewCameraManager;
    projectDataService;
    localDataService;
    chromosome;

    domElement: HTMLElement;
    tracksContainer = null;
    referenceId = null;

    rulerTrack = {
        format: RULER_TRACK_FORMAT
    };
    tracks = [];
    renderable = false;

    dispatcher;
    projectContext;
    selectionContext;
    trackNamingService;

    static get UID() {
        return 'ngbTracksViewController';
    }

    constructor(
        $scope,
        $element,
        localDataService,
        projectDataService,
        genomeDataService,
        vcfDataService,
        dispatcher,
        projectContext,
        selectionContext,
        trackNamingService
    ) {
        super();

        Object.assign(this, {
            $element,
            $scope,
            dispatcher,
            localDataService,
            genomeDataService,
            projectContext,
            projectDataService,
            vcfDataService,
            selectionContext,
            trackNamingService
        });

        this.domElement = this.$element[0];
        this.tracksContainer = $(this.domElement).find('.track-panel-container')[0];
        this.refreshTracksScope = () => $scope.$apply();

        (async() => {
            await  this.INIT();
            this.refreshTracksScope();
            if (!this.browserId) {
                Object.assign(this.events, {
                    'chromosome:change': () => {
                        this.position = null;
                        this.tracks = [];
                        this.renderable = false;
                        this.chromosomeName = null;
                        Promise.delay(0)
                            .then(async() => {
                                await this.INIT();
                            })
                            .then(this.refreshTracksScope);
                    },
                    'position:select': ::this.selectPosition,
                    'viewport:position': ::this.setViewport,
                    'blatRegion:change': ::this.setBlatRegion,
                    'blastRegion:change': ::this.setBlastRegion
                });
            }

            this.initEvents();

        })();
    }

    get selectedTracksMenuIsVisible() {
        return this.selectionContext.getSelected(this.browserId).length > 1;
    }

    events = {
        'tracks:state:change': () => {
            Promise.delay(0)
                .then(::this.manageTracks)
                .then(::this.refreshTracksScope);
        },
        'tracks:fit:height': ::this.fitTracksHeights,
        'hotkeyPressed': ::this.hotKeyListener
    };

    hotKeyListener(event) {
        if (event === 'bam>showAlignments') {
            const bamTracksWithAlignmentTrue = this.projectContext.tracksState.filter(t => t.format === 'BAM' && t.state.alignments === true);
            const bamTracksWithAlignmentFalse = this.projectContext.tracksState.filter(t => t.format === 'BAM' && t.state.alignments === false);

            this.dispatcher.emitGlobalEvent('bam:showAlignments', {event : event, disableShowAlignmentsForAllTracks: bamTracksWithAlignmentTrue.length > 0 && bamTracksWithAlignmentFalse.length > 0});
        }
    }

    $onDestroy() {
        if (this.zoomManager && this.zoomManager.destructor instanceof Function) this.zoomManager.destructor();
        if (this.viewport && this.viewport.onDestroy) {
            this.viewport.onDestroy();
        }
        if (this._removeHotKeyListener) {
            this._removeHotKeyListener();
        }
        this.viewport = null;
    }

    async INIT() {
        this.renderable = false;
        this.tracks = [];

        let chromosome = this.projectContext.currentChromosome;
        if (this.chromosomeName) {
            chromosome = this.projectContext.getChromosome({name: this.chromosomeName});
        }

        if (!chromosome)
            return;

        const browserInitialSetting = {
            browserId: this.browserId,
            position: this.position
        };

        this.referenceId = this.projectContext.referenceId;

        const tracks = this.projectContext.getActiveTracks();

        this.chromosome = chromosome;

        if (!this.chromosome.size)
            return;

        this.trackOpts = {
            chromosome: this.chromosome,
            chromosomeId: this.chromosome.id,
            isFixed: false,
            referenceId: this.referenceId
        };

        await Promise.delay(0);
        const scrollPanel = angular.element(this.domElement).find('.tracks-panel')[0];

        let brush = undefined;
        if (this.projectContext.viewport) {
            brush = this.projectContext.viewport;
        } else if (this.projectContext.position) {
            brush = {
                end: this.projectContext.position,
                start: this.projectContext.position
            };
        }
        let blatRegion = undefined;
        if (this.projectContext.blatRegion) {
            blatRegion = this.projectContext.blatRegion;
        } else {
            blatRegion = null;
        }

        let blastRegion = undefined;
        if (this.projectContext.blastRegion) {
            blastRegion = this.projectContext.blastRegion;
        } else {
            blastRegion = null;
        }

        const viewportPxMargin = 6;
        if (this.viewport) {
            this.viewport.reInitialize(scrollPanel,
                {brush, chromosomeSize: this.chromosome.size, blatRegion, blastRegion},
                this.dispatcher,
                this.projectContext,
                viewportPxMargin,
                browserInitialSetting,
                this.vcfDataService);
        } else {
            this.viewport = new Viewport(scrollPanel,
                {brush, chromosomeSize: this.chromosome.size, blatRegion, blastRegion},
                this.dispatcher,
                this.projectContext,
                viewportPxMargin,
                browserInitialSetting,
                this.vcfDataService);
        }

        if (this.brushStart && this.brushEnd) {
            this.viewport.transform({end: this.brushEnd, start: this.brushStart});
        }

        if (this.position) {
            this.viewport.selectPosition(this.position);
        }

        this.zoomManager = new ngbTracksViewZoomManager(this.viewport);

        const reference = this.projectContext.reference;

        this.tracks = tracks;

        this.bookmarkCamera =
            new ngbTracksViewCameraManager(
                () => [
                    'NGB',
                    reference.name,
                    this.chromosome.name,
                    Math.floor(this.viewport.brush.start),
                    Math.floor(this.viewport.brush.end),
                    getFormattedDate()
                ].join('_'),
                () => [this.rulerTrack, ...this.tracks],
                track => this.trackNamingService.getCustomName(track),
                () => this.localDataService.getSettings().showTrackOriginalName
            );

        this.renderable = true;
    }

    isFirstTrackOfTypes (track, ...types) {
        const typesSet = new Set(types);
        const [firstTrack] = (this.tracks || []).filter(t => typesSet.has(t.format));
        return firstTrack === track;
    }

    isFirstGeneOrBedTrack (track) {
        return this.isFirstTrackOfTypes(track, 'GENE', 'BED', 'FEATURE_COUNTS');
    }

    getTrackNotification (track) {
        if (this.isFirstGeneOrBedTrack(track)) {
            return NotificationsContext.NotificationType.GeneHistogramNotification;
        }
        return undefined;
    }

    fitTracksHeights() {
        let availableHeight = $(this.tracksContainer).height();
        let totalTrackWeights = 0;
        const tracks = [this.rulerTrack, ...this.tracks];
        let trackHeaderHeight = 20;
        let trackMargin = 8;
        const totalMargin = 2;
        if ((this.projectContext.collapsedTrackHeaders !== undefined && this.projectContext.collapsedTrackHeaders) || !this.localDataService.getSettings().showTracksHeaders) {
            trackHeaderHeight = 0;
            trackMargin = 4;
        }
        for (let i = 0; i < tracks.length; i++) {
            const instance = tracks[i].instance;
            if (instance) {
                if (instance.config.format === RULER_TRACK_FORMAT) {
                    availableHeight -= (instance.height + trackMargin);
                    continue;
                } else if (!instance.trackIsResizable) {
                    availableHeight -= instance.height;
                } else if (typeof instance.trackConfig.fitHeightFactor === 'function') {
                    totalTrackWeights += instance.trackConfig.fitHeightFactor(instance.state);
                } else {
                    totalTrackWeights += instance.trackConfig.fitHeightFactor;
                }
                availableHeight -= (trackHeaderHeight + trackMargin);
            }
        }
        availableHeight -= totalMargin;
        const oneWeight = availableHeight / totalTrackWeights;
        for (let i = 0; i < tracks.length; i++) {
            const instance = tracks[i].instance;
            if (instance) {
                let trackWeight = 1;
                if (instance.config.format === RULER_TRACK_FORMAT || !instance.trackIsResizable) {
                    continue;
                } else if (typeof instance.trackConfig.fitHeightFactor === 'function') {
                    trackWeight = instance.trackConfig.fitHeightFactor(instance.state);
                } else {
                    trackWeight = instance.trackConfig.fitHeightFactor;
                }
                instance.height = oneWeight * trackWeight;
                instance.reportTrackState(true);
            }
        }
        this.projectContext.submitTracksStates();
    }

    manageTracks() {
        const oldTracks = this.tracks && this.tracks.length ? this.tracks.reduce((acc, track) => ({
            ...acc,
            [this.trackHash(track)]: track
        })) : [];
        this.tracks = (this.projectContext.getActiveTracks())
        //adding old props to new data
            .map(track => ({
                ...oldTracks[this.trackHash(track)],
                ...track
            }));
    }

    selectPosition() {
        if (this.renderable) {
            this.viewport.selectPosition(this.projectContext.position);
        }
    }

    setViewport() {
        if (this.renderable) {
            this.viewport.selectInterval(this.projectContext.viewport);
        }
    }

    setBlatRegion() {
        if (this.renderable) {
            this.viewport.blatRegion = this.projectContext.blatRegion;
        }
    }

    setBlastRegion() {
        if (this.renderable) {
            this.viewport.blastRegion = this.projectContext.blastRegion;
        }
    }

    trackHash(track) {
        return `[${track.bioDataItemId}][${track.projectId}][${track.duplicateId || ''}]`;
    }
}

