import {ngbTracksViewZoomManager, ngbTracksViewCameraManager} from './managers';
import Promise from 'bluebird';
import {Viewport} from '../../../modules/render/';
import angular from 'angular';
import baseController from '../../shared/baseController';
import {getFormattedDate} from '../../shared/utils/date';

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

    static get UID() {
        return 'ngbTracksViewController';
    }

    constructor($scope, $element, localDataService, projectDataService, genomeDataService, vcfDataService, dispatcher, projectContext) {
        super();

        Object.assign(this, {
            $element,
            $scope,
            dispatcher,
            localDataService,
            genomeDataService,
            projectContext,
            projectDataService,
            vcfDataService
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
                    'viewport:position': ::this.setViewport
                });
            }

            this.initEvents();

        })();
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

        const viewportPxMargin = 6;
        this.viewport = new Viewport(scrollPanel,
            {brush, chromosomeSize: this.chromosome.size},
            this.dispatcher,
            this.projectContext,
            viewportPxMargin,
            browserInitialSetting,
            this.vcfDataService);
        this.tracks = tracks;

        if (this.brushStart && this.brushEnd) {
            this.viewport.transform({end: this.brushEnd, start: this.brushStart});
        }

        if (this.position) {
            this.viewport.selectPosition(this.position);
        }

        this.zoomManager = new ngbTracksViewZoomManager(this.viewport);

        const reference = this.projectContext.reference;

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
                () => [this.rulerTrack, ...this.tracks]);

        this.renderable = true;
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

    async manageTracks() {
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

    trackHash(track) {
        return `[${track.bioDataItemId}][${track.projectId}]`;
    }
}

