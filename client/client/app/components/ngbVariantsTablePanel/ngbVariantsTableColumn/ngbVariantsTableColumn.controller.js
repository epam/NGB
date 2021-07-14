import  baseController from '../../../shared/baseController';

export default class ngbVariantTableColumnController extends baseController {

    static get UID() {
        return 'ngbVariantTableColumnController';
    }

    projectContext;
    displayVariantsFilter;

    constructor(dispatcher, projectContext, $scope, $timeout) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext
        });

        this.displayVariantsFilter = this.projectContext.displayVariantsFilter;

        this.initEvents();
    }

    events = {
        'ngbColumns:change': ::this.onColumnChange,
        'reference:change': ::this.loadColumns,
        'display:variants:filter' : ::this.updateDisplayVariantsFilterValue
    };

    $onInit() {
        this.loadColumns();
    }

    updateDisplayVariantsFilterValue() {
        this.displayVariantsFilter = this.projectContext.displayVariantsFilter;
    }

    onDisplayVariantsFilterChange() {
        this.projectContext.setDisplayVariantsFilter(this.displayVariantsFilter, false);
    }

    onVariantsRestoreViewClick() {
        console.log(this.columnsList);
    }

    loadColumns() {
        this.onColumnChange([]);
        this.$timeout(::this.$scope.$apply);
    }

    addColumnToTable() {
        const currentColumns = this.projectContext.vcfInfoColumns;
        const infoFields = this.columnsList
            .filter(column => column.selection === true)
            .map(m => m.name);
        const [added] = infoFields.filter(i => currentColumns.indexOf(i) === -1);
        const [removed] = currentColumns.filter(c => infoFields.indexOf(c) === -1);
        if (added) {
            const columns = this.projectContext.vcfColumns;
            if (columns[columns.length - 1] === 'info') {
                columns.splice(columns.length - 1, 0, added);
            } else {
                columns.push(added);
            }
            this.projectContext.vcfColumns = columns;
        }
        if (removed) {
            const columns = this.projectContext.vcfColumns;
            const index = columns.indexOf(removed);
            if (index >= 0) {
                columns.splice(index, 1);
                this.projectContext.vcfColumns = columns;
            }
        }
        this.projectContext.changeVcfInfoFields(infoFields);
    }

    onColumnChange() {
        if (this.projectContext.reference) {
            this.columnsList = this.projectContext.vcfInfo;
        } else {
            this.columnsList = [];
        }
        const infoFields = this.projectContext.vcfColumns;
        this.columnsList
            .forEach(m => m.selection = infoFields.indexOf(m.name) >= 0);
    }
}
