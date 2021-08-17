import './ui-grid-group-columns.scss';
import angular from 'angular';

angular.module('uiGridGroupColumns', []).service('uiGridGroupColumnsService', [
    '$log',
    '$q',
    '$compile',
    'gridUtil',
    class {
        constructor(
            $log,
            $q,
            $compile,
            gridUtil,
        ) {
            Object.assign(this, {
                $log,
                $q,
                $compile,
                gridUtil,
            });
        }

        static get $$ngIsClass() {
            return true;
        }

        defaultGridOptions(grid) {
            // Check if different from defaults
            if (grid.options.headerTemplate !== null) {
                this.$log.debug('Warning: You are overriding the \'headerTemplate\' option while using the \'ui-grid-group-columns\' feature.');
            } else {
                grid.options.headerTemplate = require('./ui-grid-header-group-columns.html'); // eslint-disable-line no-param-reassign
            }

            if (grid.options.rowTemplate !== 'ui-grid/ui-grid-row') {
                this.$log.debug('Warning: You are overriding the \'rowTemplate\' option while using the \'ui-grid-group-columns\' feature.');
            } else {
                grid.options.rowTemplate = require('./ui-grid-row-group-columns.html'); // eslint-disable-line no-param-reassign
            }

            // The `ui-grid-row` directive uses the grid's `getRowTemplateFn` to generate its template.
            // That function is defined when the grid is instantiated and reads the `rowTemplate` value
            // from the grid options provided at initialization.
            // Because our directive runs after that, our custom options are not taken into consideration.
            // Our solution is to redefine the function (using the original code) after we updated the
            // grid options.
            // The alternative of using `registerRowBuilder` would have added unnecessary overhead because
            // it creates a new template function for each row.
            //
            // References:
            // https://github.com/angular-ui/ui-grid/blob/v4.2.4/src/js/core/directives/ui-grid-row.js#L28
            // https://github.com/angular-ui/ui-grid/blob/v4.2.4/src/js/core/services/gridClassFactory.js#L28
            if (grid.options.rowTemplate) {
                const rowTemplateFnPromise = this.$q.defer();
                grid.getRowTemplateFn = rowTemplateFnPromise.promise; // eslint-disable-line no-param-reassign

                this.gridUtil.getTemplate(grid.options.rowTemplate)
                    .then((template) => {
                        const rowTemplateFn = this.$compile(template);
                        rowTemplateFnPromise.resolve(rowTemplateFn);
                    })
                    .catch(() => {
                        this.$log.error(`Couldn't fetch/use row template '${grid.options.rowTemplate}'`);
                    });
            }

            if (grid.options.footerTemplate !== 'ui-grid/ui-grid-footer') {
                this.$log.debug('Warning: You are overriding the \'footerTemplate\' option while using the \'ui-grid-group-columns\' feature.');
            } else {
                grid.options.footerTemplate = require('./ui-grid-footer-group-columns.html'); // eslint-disable-line no-param-reassign
            }
        }

        groupColumnsProcessor(renderedColumnsToProcess) {
            // This function runs in the context of the grid
            const grid = this;
            const lastGroup = {};

            // Although it will be used for CSS class names, there is no problem if the generated slug
            // starts with a digit because it will be prepended by a prefix later on.
            const generateSlug = text => (
                // eslint-disable-next-line eqeqeq
                text != null
                    ? text
                        .toString()
                        .toLowerCase()
                        .replace(/\s+/g, '-')
                        .replace(/[^\w-]+/g, '')
                        .replace(/--+/g, '-')
                        .replace(/^-+/, '')
                        .replace(/-+$/, '')
                    : ''
            );

            // Reset `renderedColumnGroups` in all existing `renderContainers` (left, body, right)
            Object.values(grid.renderContainers).forEach((container) => {
                if (container.renderedColumnGroups === undefined) {
                    container.renderedColumnGroups = []; // eslint-disable-line no-param-reassign
                } else {
                    // Empty existing array, do not replace with a new one
                    container.renderedColumnGroups.length = 0; // eslint-disable-line no-param-reassign
                }
            });

            // We filter through all the columns and do not simply use the `renderedColumns` from
            // `renderContainers` because when a new render container is created that property is
            // populated after this function runs.
            renderedColumnsToProcess.forEach((item) => {
                // Clear first and last markers
                delete item.isFirstInGroup; // eslint-disable-line no-param-reassign
                delete item.isLastInGroup; // eslint-disable-line no-param-reassign

                if (item.visible === false) {
                    return;
                }

                const containerName = item.renderContainer || 'body';
                const groups = grid.renderContainers[containerName].renderedColumnGroups;
                const name = item.colDef.group;

                // Create a new group if empty,
                // then put consecutive columns that are related in the same group.
                if (groups.length > 0 && name === lastGroup[containerName]) {
                    groups[groups.length - 1].columns.push(item);
                } else {
                    groups.push({
                        name,
                        cssClass: name ? `ui-grid-header-group_${generateSlug(name)}` : undefined,
                        columns: [item],
                    });

                    lastGroup[containerName] = name;
                }
            });

            // Mark the first and last columns of each group inside the
            // existing `renderContainers` (left, body, right).
            Object.values(grid.renderContainers).forEach((container) => {
                container.renderedColumnGroups.forEach(({columns}) => {
                    columns[0].isFirstInGroup = true; // eslint-disable-line no-param-reassign
                    columns[columns.length - 1].isLastInGroup = true; // eslint-disable-line no-param-reassign
                });
            });

            return renderedColumnsToProcess;
        }
    },
]);

angular.module('uiGridGroupColumns').directive('uiGridGroupColumns', [
    'uiGridGroupColumnsService',
    uiGridGroupColumnsService => ({
        require: '^uiGrid',
        compile: () => ({
            pre($scope, $elm, $attrs, uiGridCtrl) {
                uiGridGroupColumnsService.defaultGridOptions(uiGridCtrl.grid);
                uiGridCtrl.grid.registerColumnsProcessor(
                    uiGridGroupColumnsService.groupColumnsProcessor,
                    1100,
                );
            },
        }),
    }),
]);
