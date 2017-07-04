import  baseController from '../../shared/baseController';

import $ from 'jquery';

import 'jquery-ui/themes/base/core.css';
import 'jquery-ui/themes/base/theme.css';
import 'jquery-ui/themes/base/selectable.css';
import 'jquery-ui/ui/core';
import 'jquery-ui/ui/widgets/sortable';

export default class ngbOrganizeTracksController extends baseController {
    static get UID() {
        return 'ngbOrganizeTracksController';
    }

    gridOptions = {
        enableSorting: false,
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 20,
        multiSelect: true,
        rowHeight: 20,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false,
        enableColumnMenus: false,
        modifierKeysToMultiSelect: true,
        columnDefs: [
            {
                field: 'name',
                minWidth: 50,
                width: '80%',
                name: 'Track name'
            },
            {
                field: 'format',
                minWidth: 16,
                width: '20%',
                name: 'Track type'
            }
        ]
    };

    constructor($scope, $element, $timeout, $mdDialog, dispatcher, projectContext) {
        super($scope);

        Object.assign(this, {
            $scope,
            projectContext,
            $mdDialog,
            $timeout,
            $element,
            dispatcher
        });

        this.disabledButton = true;
        this.disabledSortButton = false;

        Object.assign(this.gridOptions, {
            appScopeProvider: $scope,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
            }
        });

        this.gridOptions.data = this.organizeTracks;

        const self = this;
        this.$timeout(function () {
            self.elmTracks = $($element).find('.ui-grid-canvas');
            self.initSortable();
        }, 0);

    }

    rowClick() {
        const selectedRows = this.gridApi.selection.getSelectedRows();
        if (selectedRows && selectedRows.length > 0) {
            this.disabledButton = false;
        } else {
            this.disabledButton = true;
        }
    }

    toTop() {
        const selectedRows = this.gridApi.selection.getSelectedRows();
        selectedRows.forEach((row) => {
            const index = this.gridOptions.data.indexOf(row);
            this.gridOptions.data.splice(index, 1);
            this.gridOptions.data.splice(0, 0, row);
        });
    }

    toBottom() {
        const selectedRows = this.gridApi.selection.getSelectedRows();
        selectedRows.forEach((row) => {
            const index = this.gridOptions.data.indexOf(row);
            this.gridOptions.data.splice(index, 1);
            this.gridOptions.data.splice(this.gridOptions.data.length, 0, row);
        });
    }

    up() {
        const selectedRows = this.gridApi.selection.getSelectedRows();
        selectedRows.forEach((row) => {
            const index = this.gridOptions.data.indexOf(row);

            if (index !== 0) {
                this.gridOptions.data.splice(index, 1);
                this.gridOptions.data.splice(index - 1, 0, row);
            }
        });
    }

    down() {
        const selectedRows = this.gridApi.selection.getSelectedRows().reverse();
        selectedRows.forEach((row) => {
            const index = this.gridOptions.data.indexOf(row);
            if (index !== this.gridOptions.data.length) {
                this.gridOptions.data.splice(index, 1);
                this.gridOptions.data.splice(index + 1, 0, row);
            }
        });
    }

    sortByName() {
        this.gridOptions.data.sort((a, b) => a.name.toLowerCase() > b.name.toLowerCase() ? 1 : -1);
        this.organizeTracks = this.gridOptions.data;
    }

    sortByType() {
        this.gridOptions.data.sort((a, b) => a.format.toLowerCase() > b.format.toLowerCase() ? 1 : -1);
        this.organizeTracks = this.gridOptions.data;
    }

    close() {
        const selectedRows = this.gridApi.selection.getSelectedRows();
        const filterTracks = this.gridOptions.data.filter(t => selectedRows.indexOf(t) === -1);
        this.gridOptions.data = filterTracks;
        this.organizeTracks = filterTracks;
        this.disabledSortButton = !(this.gridOptions.data && this.gridOptions.data.length > 0);
        this.disabledButton = true;
    }

    initSortable() {
        const self = this;
        let unselect = false;

        this.elmTracks.sortable({
            update: function (e, ui) {
                const end = ui.item.index();
                const selectedRows = self.gridApi.selection.getSelectedRows();

                selectedRows.forEach((row) => {
                    const index = self.gridOptions.data.indexOf(row);
                    self.gridOptions.data.splice(index, 1);
                    self.gridOptions.data.splice(end, 0, row);
                });
                self.$timeout(::self.$scope.$apply);
                return false;
            },
            start: function (e, ui) {
                if (unselect) {
                    const start = ui.item.index();
                    unselect = false;

                    self.gridApi.selection.clearSelectedRows();
                    self.gridApi.selection.selectRow(self.gridOptions.data[start]);
                }
            },
            stop: function (e, ui) {
                $(self.$element).find('.ui-grid-row-selected').css('visibility', 'visible');
            },
            helper: function (e, item) {
                if (!item.hasClass('ui-grid-row-selected')) {
                    item.addClass('ui-grid-row-selected').siblings().removeClass('ui-grid-row-selected');
                    unselect = true;
                }
                const elements = item.parent().children('.ui-grid-row-selected').clone();

                item.siblings('.ui-grid-row-selected').css('visibility', 'hidden');
                const helper = $('<div/>');
                return helper.append(elements);

            }
        });
    }

}
