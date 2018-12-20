import BaseController from '../../../../shared/baseController';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbAddUserRoleDlgController extends BaseController {

    isUser = true;
    availableItems = [];
    selectedItem;
    ngbPermissionsFormService;
    node;

    searchTerm = '';

    get itemType() {
        if (this.isUser) {
            return 'user';
        }
        return 'role or group';
    }

    get isValid() {
        return !!this.selectedItem;
    }

    /* @ngInject */
    constructor($mdDialog, $scope, isUser, availableItems, node, ngbPermissionsFormService) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            availableItems,
            isUser,
            ngbPermissionsFormService,
            node
        });
    }

    getItemName(item) {
        if (!item) {
            return null;
        }
        if (this.isUser) {
            return item.userName;
        }
        return item.name;
    }

    getItemDisplayName(item) {
        if (!item) {
            return null;
        }
        const name = this.getItemName(item);
        if (!this.isUser && !item.predefined && (name || '').toLowerCase().startsWith(ROLE_NAME_FIRST_PART.toLowerCase())) {
            return name.substring(ROLE_NAME_FIRST_PART.length);
        }
        return name;
    }

    clearSearchTerm() {
        this.searchTerm = '';
    }

    close() {
        this.$mdDialog.hide();
    }

    cancel() {
        this.$mdDialog.cancel();
    }

    save() {
        this.ngbPermissionsFormService
            .grantPermission(
                this.node, {
                    name: this.getItemName(this.selectedItem),
                    principal: this.isUser
                },
                0)
            .then(() => this.$mdDialog.hide());
    }

}
