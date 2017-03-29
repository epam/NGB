import {ngbTracksViewZoomManager, ngbTracksViewCameraManager} from './managers';
import Promise from 'bluebird';
import {Viewport} from '../../../modules/render/';
import angular from 'angular';
import baseController from '../../shared/baseController';
import {getFormattedDate} from '../../shared/utils/date';

export default class ngbTracksViewController extends baseController {
    zoomManager: ngbTracksViewZoomManager;
    bookmarkCamera: ngbTracksViewCameraManager;
    projectDataService;
    chromosome;

    domElement: HTMLElement;
    referenceId = null;

    rulerTrack = {
        format: 'RULER'
    };
    tracks = [];
    renderable = false;

    dispatcher;
    projectContext;

    static get UID() {
        return 'ngbTracksViewController';
    }

    constructor($scope, $element, projectDataService, genomeDataService, vcfDataService, dispatcher, projectContext ) {
        super();

        Object.assign(this, {
            $element,
            $scope,
            dispatcher,
            genomeDataService,
            projectContext,
            projectDataService,
            vcfDataService
        });

        this.domElement = this.$element[0];
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
        }
    };

    $onDestroy() {
        if (this.zoomManager && this.zoomManager.destructor instanceof Function) this.zoomManager.destructor();
        if (this.viewport && this.viewport.onDestroy) {
            this.viewport.onDestroy();
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

