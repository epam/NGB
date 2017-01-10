export default class State {
    constructor() {
        this._toolbarVisibility = true;
    }

    _bookmarkVisibility = true;
    _chromosome;
    _projectId;
    _position;
    _rewriteLayout = true;
    _screenShotVisibility = true;
    _toolbarVisibility = true;
    _viewport = true;

    _layout = null;
    _tracksState = null;

    get projectId() {
        return this._projectId;
    }

    set projectId(value) {
        const valueCorrected = (value !== null && value !== undefined) ? parseInt(value) : null;
        if (this._projectId !== valueCorrected) {
            this._projectId = valueCorrected;
            this._chromosome = null;
            this._position = null;
        }
    }

    get chromosome() {
        return this._chromosome;
    }

    set chromosome(value) {
        this._chromosome = value;
    }


    get position() {
        return this._position;
    }

    set position(value) {
        this._position = value;
    }

    get rewriteLayout() {
        return this._rewriteLayout;
    }

    set rewriteLayout(value) {
        this._rewriteLayout = value;
    }

    get viewport() {
        return this._viewport;
    }

    set viewport(value) {
        this._viewport = value;
    }

    get layout() {
        if (this._layout) return JSON.parse(this._layout);
        return JSON.parse(localStorage.getItem('goldenLayout'));
    }

    set layout(value) {
        this._layout = JSON.stringify(value);
        if (this.rewriteLayout === true) {
            localStorage.setItem('goldenLayout', this._layout);
        }
    }


    get tracksState() {
        if (this._tracksState)return JSON.parse(this._tracksState);
        return JSON.parse(localStorage.getItem(`projectId_${this.projectId}`));
    }

    set tracksState(value) {
        this._tracksState = JSON.stringify(value);
        if (this.rewriteLayout === true) {
            localStorage.setItem(`projectId_${this.projectId}`, this._tracksState);
        }
    }

    get toolbarVisibility() {
        return this._toolbarVisibility;
    }

    set toolbarVisibility(value) {
        this._toolbarVisibility = value;
    }

    get bookmarkVisibility() {
        return this._bookmarkVisibility;
    }

    set bookmarkVisibility(value) {
        this._bookmarkVisibility = value;
    }

    get screenShotVisibility() {
        return this._screenShotVisibility;
    }

    set screenShotVisibility(value) {
        this._screenShotVisibility = value;
    }

}