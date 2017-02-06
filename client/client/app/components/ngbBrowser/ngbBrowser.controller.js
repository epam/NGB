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
        'reference:change': ::this.onStateChange
    };

    $onInit() {
        this.onStateChange();
    }

    onStateChange() {
        const reference = this.projectContext.reference;
        const chromosome = this.projectContext.currentChromosome;
        this.isChromosome = chromosome !== null;
        this.isProject = reference !== null && reference !== undefined;

        this.$timeout(::this.$scope.$apply);
    }
}