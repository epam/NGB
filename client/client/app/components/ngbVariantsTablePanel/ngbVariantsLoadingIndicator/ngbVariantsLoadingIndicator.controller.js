import  baseController from '../../../shared/baseController';

export default class ngbVariantsLoadingIndicatorController extends baseController {

    static get UID() {
        return 'ngbVariantsLoadingIndicatorController';
    }

    projectContext;
    isProgressShown = false;
    _scope;

    constructor(dispatcher, projectContext, $scope) {
        super(dispatcher);
        this._scope = $scope;
        Object.assign(this, {
            $scope,
            dispatcher,
            projectContext
        });

        this.initEvents();
        this.refresh(false);
    }

    events = {
        'variants:page:loading:finished': this.refresh.bind(this),
        'variants:page:loading:started': this.refresh.bind(this)
    };

    refresh () {
        this.isProgressShown = this.projectContext.variantsPageLoading;
    }
}