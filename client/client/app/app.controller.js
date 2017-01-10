import  baseController from './shared/baseController';

export default class ngbAppController extends baseController {
    static get UID() {
        return 'ngbAppController';
    }

    dispatcher;
    projectContext;

    /* @ngInject */
    constructor(dispatcher,
                projectContext,
                eventHotkey,
                $stateParams,
                $rootScope,
                $scope,
                $state,
                projectDataService,
                genomeDataService,
                localDataService) {
        super();
        Object.assign(this, {
            $scope,
            $state,
            $stateParams,
            dispatcher,
            eventHotkey,
            genomeDataService,
            projectContext,
            projectDataService
        });

        this.dictionaryState = localDataService.getDictionary().State;

        this.initStateFromParams();

        this.initEvents();

        this.eventHotkey.init();

        $rootScope.$on('$stateChangeSuccess', (evt, toState, toParams) => {
            this._changeStateFromParams(toParams);
        });

    }

    events = {
        'projectId:change': ::this._goToState,
        'state:change': ::this._goToState,
        'viewport:position': ::this._goToState
    };

    _changeStateFromParams(params) {
        const {projectId, chromosome, end, rewrite, start, tracks} = params;
        const position = start
            ? {end, start}
            : null;
        this.projectContext.rewriteLayout = true;
        if (rewrite) {
            this.projectContext.rewriteLayout = this.dictionaryState.on.toLowerCase() === rewrite.toLowerCase();
        }
        this.projectContext.changeState({
            chromosome: {name: chromosome},
            position: (start && !end) ? start: null,
            project: {id: projectId},
            tracksState: tracks ? JSON.parse(tracks) : null,
            viewport: position
        });
    }

    initStateFromParams() {
        this._changeStateFromParams(this.$stateParams);

        const {toolbar, layout, bookmark, screenshot}=this.$stateParams;

        if (toolbar) {
            const toolbarVisibility = this.dictionaryState.on.toLowerCase() === toolbar.toLowerCase();
            this.projectContext.toolbarVisibility = toolbarVisibility;
        }

        if (bookmark) {
            const bookmarkVisibility = this.dictionaryState.on.toLowerCase() === bookmark.toLowerCase();
            this.projectContext.bookmarkVisibility = bookmarkVisibility;
        }

        if (layout) {
            this.projectContext.layout = JSON.parse(layout);
        }

        if (screenshot) {
            const screenShotVisibility = this.dictionaryState.on.toLowerCase() === screenshot.toLowerCase();
            this.projectContext.screenShotVisibility = screenShotVisibility;
        }
    }

    _goToState() {
        const projectId = this.projectContext.projectId;
        const chromosome = this.projectContext.currentChromosome ? this.projectContext.currentChromosome.name : null;
        const start = this.projectContext.viewport ? parseInt(this.projectContext.viewport.start) : null;
        const end = this.projectContext.viewport ? parseInt(this.projectContext.viewport.end) : null;
        this.$state.go(this.$state.current.name, {
            chromosome,
            end,
            projectId,
            start
        }, {notify: false, inherit: false});
    }
}