export default class ngbInfoProductController {
    static get UID() {
        return 'ngbInfoProductController';
    }

    /* @ngInject */
    constructor($mdDialog) {
        this.showContentMenu = false;
        Object.assign(this, {
            $mdDialog
        });
    }


    openMenu($mdOpenMenu, ev) {
        this.showContentMenu = !this.showContentMenu;
        $mdOpenMenu(ev);
    }

}
