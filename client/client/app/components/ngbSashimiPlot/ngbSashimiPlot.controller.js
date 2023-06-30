import Promise from 'bluebird';
import angular from 'angular';
import baseController from '../../shared/baseController';
import {getFormattedDate} from '../../shared/utils/date';
import {ngbTracksViewCameraManager, ngbTracksViewZoomManager} from '../ngbTracksView/managers';
import {Viewport} from '../../../modules/render/core/viewport';

export default class ngbSashimiPlotController extends baseController{

    static get UID() {
        return 'ngbSashimiPlotController';
    }

    zoomManager: ngbTracksViewZoomManager;
    bookmarkCamera: ngbTracksViewCameraManager;
    projectContext;
    dispatcher;

    rulerTrack = {
        format: 'RULER'
    };

    geneTracks = [];
    bamTracks = [];

    bamTrackState = {
        alignments: false,
        coverage: false,
        spliceJunctions: true,
        sashimi: true
    };

    constructor($scope, $element, projectContext, dispatcher, localDataService, trackNamingService) {
        super(dispatcher);
        this.$scope = $scope;
        this.domElement = $element[0];
        this.projectContext = projectContext;
        this.dispatcher = dispatcher;
        this.localDataService = localDataService;
        this.trackNamingService = trackNamingService;
        this.refreshTracksScope = () => $scope.$apply();
    }

    $onInit() {
        (async() => {
            await this.INIT();
            this.refreshTracksScope();
            this.initEvents();
        })();
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
        let chromosome;
        if (this.chromosomeName) {
            chromosome = this.projectContext.getChromosome({name: this.chromosomeName});
        } else {
            chromosome = this.projectContext.currentChromosome;
        }
        if (!chromosome)
            return;

        const browserInitialSetting = {
            browserId: this.browserId,
            position: this.position,
            silentInteractions: true
        };
        if (!this.referenceId) {
            this.referenceId = this.projectContext.referenceId;
        }
        this.chromosome = chromosome;

        if (!this.chromosome.size)
            return;

        const currentTracks = (this.projectContext.getActiveTracks() || []).filter((t) => {
            if (this.tracks) {
                return (this.tracks || [])
                    .filter(selectedTrack => `${t.bioDataItemId}` === `${selectedTrack.config.bioDataItemId}` &&
                        t.format === selectedTrack.config.format &&
                        `${t.duplicateId || ''}` === `${selectedTrack.config.duplicateId || ''}`
                    ).length > 0;
            }
            return false;
        });
        this.geneTracks = (this.projectContext.getActiveTracks() || []).filter((t) => /^gene$/i.test(t.format));
        const defaultOpts = {
            chromosome: this.chromosome,
            chromosomeId: this.chromosome.id,
            isFixed: false,
            referenceId: this.referenceId,
            sashimi: true,
            state: {
                alignments: false
            }
        };
        this.bamTracks = currentTracks.map(track => {
            const [cacheService] = (this.tracks || [])
                .filter(({config}) => config.bioDataItemId === track.bioDataItemId)
                .map(({cacheService}) => cacheService);
            return {
                track,
                opts: {
                    cacheService,
                    ...defaultOpts
                }
            };
        });

        this.geneTrackOpts = {
            chromosome: this.chromosome,
            chromosomeId: this.chromosome.id,
            isFixed: false,
            referenceId: this.referenceId,
            sashimi: true
        };


        this.silentInteractions = true;

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
                () => [this.rulerTrack, ...this.bamTracks.map(t => t.track), ...this.geneTracks],
                track => this.trackNamingService.getCustomName(track),
                () => this.localDataService.getSettings().showTrackOriginalName
            );

        this.renderable = true;
    }

    trackHash(track) {
        return `[${track.bioDataItemId}][${track.projectId}][${track.duplicateId || ''}]`;
    }
}
