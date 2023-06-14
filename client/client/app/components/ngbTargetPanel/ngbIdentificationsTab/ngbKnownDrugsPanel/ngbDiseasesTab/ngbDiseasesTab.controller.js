const MENU_STATE = {
    TABLE: 'TABLE',
    BUBBLES: 'BUBBLES',
    GRAPH: 'GRAPH'
};

export default class ngbDiseasesTabController {

    get menuState() {
        return MENU_STATE;
    }

    itemSelected = this.menuState.TABLE;

    static get UID() {
        return 'ngbDiseasesTabController';
    }

    constructor($scope, $timeout) {
        Object.assign(this, {$scope, $timeout});
        this.itemSelected = this.menuState.TABLE;
    }

    changeState(state) {
        if (this.menuState.hasOwnProperty(state)) {
            this.itemSelected = this.menuState[state];
        }
        this.$timeout(::this.$scope.$apply);
    }
}
