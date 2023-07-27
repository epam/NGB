class NgbTagsController {
    static get UID () {
        return 'ngbTagsController';
    }

    constructor($scope) {
        this.$scope = $scope;
        this.addInputVisible = false;
        this.newValue = undefined;
        this.onRemoveTagCallback = this.onRemoveTag.bind(this);
    }

    get addTagLabel () {
        return this.addLabel || 'add';
    }

    get items () {
        if (this.tags && Array.isArray(this.tags)) {
            return this.tags;
        }
        return [];
    }

    get newTagValueIsDuplicate () {
        return this.addInputVisible &&
            !!this.newValue &&
            this.items.some((item) => item === this.newValue);
    }

    addItem (value) {
        if (value && this.items.includes(value)) {
            return false;
        }
        if (value) {
            if (!this.tags || !Array.isArray(this.tags)) {
                this.tags = [];
            }
            this.tags.push(value);
        }
        return true;
    }

    onAddClick () {
        this.addInputVisible = true;
        this.newValue = undefined;
    }

    onBlur () {
        if (this.addItem(this.newValue)) {
            this.addInputVisible = false;
            this.newValue = undefined;
        }
    }

    onCancelAdd () {
        this.newValue = undefined;
        this.addInputVisible = false;
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.onBlur();
                break;
            case 'escape':
            case 'esc':
                this.onCancelAdd();
                break;
            default:
                break;
        }
    }

    onRemoveTag (tag) {
        this.tags = this.items.filter((item) => item !== tag);
    }
}

export default NgbTagsController;
