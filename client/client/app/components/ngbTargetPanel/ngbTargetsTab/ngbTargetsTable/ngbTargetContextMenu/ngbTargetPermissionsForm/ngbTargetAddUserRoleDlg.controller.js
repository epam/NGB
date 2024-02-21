const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbTargetAddUserRoleDlgController {

    searchTerm = '';
    isUser = true;
    availableItems = [];
    existedItems = [];
    selectedItem;
    adGroups = [];

    get filteredItems() {
        return [...this.adGroups, ...(this.availableItems || [])].filter(i => {
            if (!this.searchTerm) {
                return true;
            }
            const str = this.searchTerm.toLowerCase();
            const matchAttributes = () => {
                if (i.attributes) {
                    for (const key in i.attributes) {
                        if (i.attributes.hasOwnProperty(key) &&
                            i.attributes[key] &&
                            i.attributes[key].toLowerCase().indexOf(str) >= 0) {
                            return true;
                        }
                    }
                }
                return false;
            };
            return (this.getItemName(i) || '').toLowerCase().indexOf(str) >= 0 || matchAttributes();
        });
    }

    get itemType() {
        if (this.isUser) {
            return 'user';
        }
        return 'role or group';
    }

    get isValid() {
        return !!this.selectedItem;
    }

    constructor($scope, $mdDialog, isUser, availableItems, existedItems, ngbTargetPermissionsFormService, target) {
        Object.assign(this, {
            $scope,
            $mdDialog,
            isUser,
            availableItems,
            existedItems,
            ngbTargetPermissionsFormService,
            target
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

    search() {
        if (!this.isUser) {
            const searchTerm = this.searchTerm;
            if (!searchTerm) {
                this.adGroups = [];
            } else {
                this.ngbTargetPermissionsFormService.searchAdGroups(searchTerm)
                    .then(results => {
                        if (searchTerm === this.searchTerm) {
                            this.adGroups = (results || [])
                                .filter(g => this.existedItems.indexOf(g) === -1)
                                .map(g => ({
                                    name: g,
                                    predefined: true
                                }));
                            this.$scope.$apply();
                        }
                    });
            }
        }
    }

    clearSearchTerm() {
        this.searchTerm = '';
    }

    save() {
        this.ngbTargetPermissionsFormService.grantPermission(
            this.target, {
                name: this.getItemName(this.selectedItem),
                principal: this.isUser
            },
            0)
                .then(() => this.$mdDialog.hide());
    }

    cancel() {
        this.$mdDialog.cancel();
    }

    close() {
        this.$mdDialog.hide();
    }
}
