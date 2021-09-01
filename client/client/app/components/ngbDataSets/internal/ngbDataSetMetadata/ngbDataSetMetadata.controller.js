import BaseController from '../../../../shared/baseController';

function metadataIsEqual(o1, o2) {
    if ((!o1 && o2) || (o1 && !o2)) {
        return false;
    }
    if (o1 && o2) {
        if (Object.keys(o1).length !== Object.keys(o2).length) {
            return false;
        }
        for(const p in o1) {
            if(o1.hasOwnProperty(p) && o2.hasOwnProperty(p)){
                if(o1[p] !== o2[p]) {
                    return false;
                }
            }
        }
    }
    return true;
}
export default class ngbDataSetMetadataController extends BaseController {
    node;
    formData;
    initial_metadata;
    metadata;
    saving;

    static get UID() {
        return 'ngbDataSetMetadataController';
    }

    constructor($mdDialog, $scope, node, ngbDataSetsService) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            node,
            service: ngbDataSetsService.projectDataService
        });
        this.formData = Object.entries(this.node.metadata || {});
        this.initial_metadata = this.node.metadata || {};
    }
    get metadataIsEmpty() {
        return Object.keys(this.formData).length === 0;
    }
    get metadataIsChanged() {
        return !metadataIsEqual(this.metadata, this.initial_metadata);
    }
    get metadata() {
        return Object.fromEntries(this.formData);
    }
    get existedKeys() { 
        return this.formData.map(pair => pair[0]).reduce((r, key) => {
            if (key) {
                r[key] = (r[key] || 0) + 1;
            }
            return r;
        }, {});
    }
    get formHasDuplicates() {
        return Object.values(this.existedKeys).filter(v => v > 1).length;
    }
    async saveMetadata() {
        const requestBody = {
            id: this.node.id,
            aclClass: this.service.getNodeAclClass(this.node),
            metadata: this.metadata
        };
        this.saving = true;
        await this.service.saveMetadata(requestBody);
        this.saving = false;
        this.$mdDialog.hide();
    }
    closeDialog() { 
        this.$mdDialog.cancel();
    }
    cancelChanges() {
        this.$mdDialog.cancel();
    }
    addFormItem() {
        this.formData.push(['', '']);
    }
    removeAttribute(index) {
        this.formData = this.formData.filter((_el, elIndex) => index !== elIndex);
    }
    isDuplicate(newKey) {
        return !!Object.entries(this.existedKeys)
            .filter(([key, value]) => (
                newKey &&
                newKey.toLowerCase() === key.toLowerCase() &&
                value > 1
            ))[0];
    }
}
