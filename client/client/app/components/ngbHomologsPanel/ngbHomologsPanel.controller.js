import baseController from '../../shared/baseController';

export default class ngbHomologsPanelController extends baseController {

    homologsStates;
    currentHomologsState;
    tabSelected;
    service;

    events = {
    };

    constructor(dispatcher, $scope, $timeout, ngbHomologsService) {
        super(dispatcher);
        Object.assign(this, {
            dispatcher,
            $scope,
            $timeout,
            ngbHomologsService
        });
        this.homologsStates = this.ngbHomologsService.homologsStates;
        this.currentHomologsState = this.homologsStates.HOMOLOGENE;
        this.tabSelected = this.homologsStates.HOMOLOGENE;
        this.service = this.ngbHomologsService.homologsServiceMap[this.currentHomologsState];
        this.initEvents();
    }

    static get UID() {
        return 'ngbHomologsPanelController';
    }

    changeState(state) {
        if (this.homologsStates.hasOwnProperty(state)) {
            this.currentHomologsState = this.homologsStates[state];
            this.service = this.ngbHomologsService.homologsServiceMap[this.currentHomologsState];
            switch (state) {
                case this.homologsStates.HOMOLOGENE:
                case this.homologsStates.HOMOLOGENE_RESULT: {
                    this.tabSelected = this.homologsStates.HOMOLOGENE;
                    break;
                }
                case this.homologsStates.ORTHO_PARA:
                case this.homologsStates.ORTHO_PARA_RESULT: {
                    this.tabSelected = this.homologsStates.ORTHO_PARA;
                    break;
                }
            }
        }
        this.$timeout(() => this.$scope.$apply());
    }

    onExternalChange() {
        if (this.currentHomologsState !== this.homologsStates.HOMOLOGENE) {
            this.ngbHomologsService.currentSearchId = null;
            this.changeState('HOMOLOGENE');
        }
    }
}
