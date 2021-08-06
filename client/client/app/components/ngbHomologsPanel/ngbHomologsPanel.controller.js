import baseController from '../../shared/baseController';

export default class ngbHomologsPanelController extends baseController {

    homologsStates;
    currentHomologsState;
    tabSelected;
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
        this.homologsStates = ngbHomologsService.homologsStates;
        this.currentHomologsState = this.homologsStates.HOMOLOGENE;
        this.tabSelected = this.homologsStates.HOMOLOGENE;
        this.initEvents();
    }

    static get UID() {
        return 'ngbHomologsPanelController';
    }

    changeState(state) {
        if (this.homologsStates.hasOwnProperty(state)) {
            this.currentHomologsState = this.homologsStates[state];
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
        this.$timeout(this.$scope.$apply.bind(this));
    }

    onExternalChange() {
        if (this.currentHomologsState !== this.homologsStates.HOMOLOGENE) {
            this.ngbHomologsService.currentSearchId = null;
            this.changeState('HOMOLOGENE');
        }
    }
}
