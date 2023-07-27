class NgbTagController {
    static get UID () {
        return 'ngbTagController';
    }

    get isRemovable() {
        return this.removable === true;
    }

    onRemoveClicked () {
        if (typeof this.onRemove === 'function') {
            this.onRemove(this.tag);
        }
    }
}

export default NgbTagController;
