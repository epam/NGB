import BaseController from '../../shared/baseController';

export default class ngbProjectInfoPanelController extends BaseController {
    /**
     * @returns {string}
     */
    static get UID() {
        return 'ngbProjectInfoPanelController';
    }

    projectContext;
    events = {
        'project:description:url': this.refreshProjectInfo.bind(this),
    };

    /**
     * @constructor
     */
    /** @ngInject */
    constructor(projectContext, $scope, $element, $timeout, dispatcher, ngbProjectInfoService) {
        super();
        Object.assign(this, {
            projectContext, $scope, $element, $timeout, dispatcher, ngbProjectInfoService
        });
        this.initEvents();
    }

    get isProgressShown () {
        return this.ngbProjectInfoService.descriptionIsLoading;
    }

    get showDescription () {
        return this.ngbProjectInfoService.showDescription;
    }

    refreshProjectInfo() {
        this.$scope.$apply();
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }
}
