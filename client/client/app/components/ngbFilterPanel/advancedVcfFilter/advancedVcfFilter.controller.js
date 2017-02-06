import baseFilterController from '../baseFilterController';

export default class advancedVcfFilterController extends baseFilterController {
    static get UID() {
        return 'advancedVcfFilterController';
    }

    /** @ngInject */
    constructor(dispatcher, projectContext, projectDataService, $scope) {
        super(dispatcher, projectContext, $scope);
        this._dataProjectService = projectDataService;

        this.INIT();
        this.setDefault();

        const __init = ::this.INIT;

        this._dispatcher.on('variants:initialized', __init);
        this._dispatcher.on('tracks:state:change', __init);

        $scope.$on('$destroy', () => {
            this._dispatcher.removeListener('variants:initialized', __init);
            this._dispatcher.removeListener('tracks:state:change', __init);
        });

    }

    setDefault() {
        super.setDefault();
        this.selectedAdvancedVcfFilters = [];
    }

    INIT() {
        this.advancedVcfFiltersList = this.projectContext.vcfInfo;
    }

    toggleAdvancedVcfFilters(item) {
        const idx = this.selectedAdvancedVcfFilters.indexOf(item);
        if (idx > -1) {
            if (item.type === 'Flag') {
                this.selectedAdvancedVcfFilters.splice(idx, 1);
            } else if (item.type === 'Integer' || item.type === 'Float') {
                if((item.from === null || item.from === undefined) && (item.to === null || item.to === undefined)){
                    this.selectedAdvancedVcfFilters.splice(idx, 1);
                } else {
                    item.value = [item.from, item.to];
                }
            }
        } else {
            if (item.type === 'Flag') {
                item.value = true;
            } else if (item.type === 'Integer' || item.type === 'Float') {
                if((item.from === null || item.from === undefined) && (item.to === null || item.to === undefined)){
                    this.selectedAdvancedVcfFilters.splice(idx, 1);
                } else {
                    item.value = [item.from, item.to];
                }
            }
            this.selectedAdvancedVcfFilters.push(item);
        }
        this.emitEvent();
    }

    toggleAdvancedVcfFiltersStrings() {
        this.emitEvent();
    }

    existsAdvancedFlag(item) {
        return this.selectedAdvancedVcfFilters.indexOf(item) > -1;
    }

    emitEvent() {
        this.projectContext.vcfFilter.additionalFilters = this._prepareAdvancedFilterObject(this.selectedAdvancedVcfFilters);
        super.emitEvent();
    }

    _prepareAdvancedFilterObject(filters) {
        const advancedFilterObject = {};
        if (filters.length === 0) {
            return advancedFilterObject;
        }
        filters.forEach(function(filter) {
            advancedFilterObject[filter.name] = filter.value;
        });
        return advancedFilterObject;
    }

}