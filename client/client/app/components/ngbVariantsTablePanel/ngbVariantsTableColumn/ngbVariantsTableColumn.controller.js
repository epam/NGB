import  baseController from '../../../shared/baseController';

export default class ngbVariantTableColumnController extends baseController {

    static get UID() {
        return 'ngbVariantTableColumnController';
    }

    projectContext;

    constructor(dispatcher, projectContext, $scope, $timeout, projectDataService) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            projectDataService
        });

        this.initEvents();
    }


    events = {
        'ngbColumns:change': ::this.onColumnChange,
        'projectId:change': ::this.loadColumns
    };

    $onInit() {
        this.loadColumns();
    }


    loadColumns() {
        this.onColumnChange([]);
        this.$timeout(::this.$scope.$apply);
    }


    addColumnToTable() {
        const infoFields = this.columnsList
            .filter(column => column.selection === true)
            .map(m => m.name);
        this.projectContext.changeVcfInfoFields(infoFields);
    }

    onColumnChange(infoFields) {
        if (this.projectContext.project) {
            this.columnsList = this.projectContext.vcfInfo;
        } else {
            this.columnsList = [];
        }
        infoFields = infoFields || [];
        this.columnsList
            .forEach(m => m.selection = infoFields.indexOf(m.name) >= 0);
    }
}
