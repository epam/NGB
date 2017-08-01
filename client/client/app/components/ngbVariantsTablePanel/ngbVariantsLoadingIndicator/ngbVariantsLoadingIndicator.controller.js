import  baseController from '../../../shared/baseController';

export default class ngbVariantsLoadingIndicatorController extends baseController {

    static get UID() {
        return 'ngbVariantsTablePaginateController';
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
        'variants:page:loading:finished': ::this.refresh,
        'variants:page:loading:started': ::this.refresh
    };

    refresh (updateScope = true) {
        this.isProgressShown = this.projectContext.variantsPageLoading;
        if (updateScope) {
            this._scope.$apply();
        }
    }
}