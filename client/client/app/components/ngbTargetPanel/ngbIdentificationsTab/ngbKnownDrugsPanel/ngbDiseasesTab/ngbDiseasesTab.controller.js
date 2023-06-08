const MENU_STATE = {
    TABLE: 'TABLE',
    BUBBLES: 'BUBBLES',
    GRAPH: 'GRAPH'
};

export default class ngbDiseasesTabController {

    currentMenuState = this.menuState.TABLE;
    itemSelected = this.menuState.TABLE;

    get menuState() {
        return MENU_STATE;
    }

    static get UID() {
        return 'ngbDiseasesTabController';
    }

    constructor($scope, $timeout) {
        Object.assign(this, {$scope, $timeout});
        this.currentMenuState = this.menuState.TABLE;
        this.itemSelected = this.menuState.TABLE;
    }

    changeState(state, isRepeat) {
        if (this.menuState.hasOwnProperty(state)) {
            this.currentMenuState = this.menuState[state];
            this.tabSelected = state;
        }
        this.$timeout(::this.$scope.$apply);
    }
}
