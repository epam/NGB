import Clipboard from 'clipboard';

const FIELD_DB = {
    transcript: 'NUCCORE',
    protein: 'PROTEIN'
};

export default class ngbSequenceTableContextMenuController {

    _sequence;

    get sequence() {
        return this._sequence;
    }
    set sequence(value) {
        this._sequence = value ? value : undefined;
    }

    get fieldDb() {
        return FIELD_DB;
    }

    static get UID() {
        return 'ngbSequenceTableContextMenuController';
    }

    constructor($scope, ngbSequenceTableContextMenu, ngbSequencesTableService) {
        this.$scope = $scope;
        this.entity = $scope.row.entity;
        this.field = $scope.col.field;
        this.ngbSequenceTableContextMenu = ngbSequenceTableContextMenu;
        this.ngbSequencesTableService = ngbSequencesTableService;
        this.clipboard = new Clipboard('.copy-to-clipboard-button');
        this.getSequence();
    }

    copyToClipboard(event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbSequenceTableContextMenu.visible()) {
            this.ngbSequenceTableContextMenu.close();
        }
    }

    async getSequence() {
        const db = this.fieldDb[this.field];
        const id = this.entity[this.field].id;
        this.sequence = await this.ngbSequencesTableService.getSequence(db, id);
    }
}
