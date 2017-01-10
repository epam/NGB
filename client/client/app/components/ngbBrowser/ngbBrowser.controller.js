import  baseController from '../../shared/baseController';

export default class ngbBrowserController extends baseController {

    static get UID() {
        return 'ngbBrowserController';
    }

    projectContext;

    constructor(dispatcher, projectContext, $scope, $timeout) {
        super(dispatcher);
        Object.assign(this, {
            dispatcher,
            projectContext,
            $scope,
            $timeout
        });
        this.initEvents();
    }

    events = {
        'chromosome:change': ::this.onStateChange,
        'projectId:change': ::this.onStateChange
    };

    $onInit() {
        this.onStateChange();
    }

    onStateChange() {
        const projectId = this.projectContext.projectId;
        const chromosome = this.projectContext.currentChromosome;
        this.isChromosome = chromosome !== null;
        this.isProject = projectId !== null && projectId !== undefined;

        this.$timeout(::this.$scope.$apply);
    }
}