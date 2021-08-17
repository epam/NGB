import baseController from '../../shared/baseController';

export default class ngbHomologsPanelController extends baseController {

    homologsStates;
    currentHomologsState;
    tabSelected;
    service;

    events = {
        'read:show:homologs': () => {
            this.changeState('HOMOLOGENE');
        }
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
}
