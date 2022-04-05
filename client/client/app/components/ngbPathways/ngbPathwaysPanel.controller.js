import baseController from '../../shared/baseController';

export default class ngbPathwaysPanelController extends baseController {

    pathwaysStates;
    currentPathwaysState;
    tabSelected;
    searchRequest;

    events = {
        'read:show:pathways': data => {
            this.searchRequest = data ? data.search : null;
            this.changeState('INTERNAL_PATHWAYS');
        },
        'load:pathways': data => {
            if (data && data.state) {
                this.changeState(data.state);
            }
        }
    };

    constructor(dispatcher, $scope, $timeout, ngbPathwaysService) {
        super(dispatcher);
        Object.assign(this, {
            dispatcher,
            $scope,
            $timeout,
            ngbPathwaysService
        });
        this.pathwaysStates = this.ngbPathwaysService.pathwaysStates;
        this.initEvents();
        this.changeState(this.ngbPathwaysService.currentState);
        if(this.ngbPathwaysService.currentState === this.pathwaysStates.INTERNAL_PATHWAYS) {
            this.searchPathway();
        }
    }

    static get UID() {
        return 'ngbPathwaysPanelController';
    }

    changeState(state) {
        if (this.pathwaysStates.hasOwnProperty(state)) {
            this.currentPathwaysState = this.pathwaysStates[state];
            this.service = this.ngbPathwaysService.pathwaysServiceMap[this.currentPathwaysState];
            switch (state) {
                case this.pathwaysStates.INTERNAL_PATHWAYS: {
                    this.ngbPathwaysService.currentInternalPathway = undefined;
                    this.tabSelected = this.pathwaysStates.INTERNAL_PATHWAYS;
                    break;
                }
                case this.pathwaysStates.INTERNAL_PATHWAYS_RESULT: {
                    this.tabSelected = this.pathwaysStates.INTERNAL_PATHWAYS;
                    break;
                }
            }
            this.ngbPathwaysService.currentState = state;
        }
        this.$timeout(() => this.$scope.$apply());
    }

    searchPathway() {
        this.ngbPathwaysService.currentSearch = {
            search: this.searchRequest,
            speciesList: [],
            rewriteSpecies: true
        };
        this.dispatcher.emitSimpleEvent('pathways:internalPathways:search');
        this.changeState('INTERNAL_PATHWAYS');
    }
}
