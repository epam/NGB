import angular from 'angular';
import baseController from '../../../shared/baseController';

import {PairReadInfo} from '../../utils/events';


export default class ngbGoldenLayoutController extends baseController {
    static get UID() {
        return 'ngbGoldenLayoutController';
    }


    panels = {
        ngbBrowser: 'ngbBrowser',
        ngbTracksView: 'ngbTracksView',
        ngbVariations: 'ngbVariantsTablePanel'
    };
    eventsNotForShareFromParent = ['layout:panels:displayed', 'layout:restore:default', 'layout:load',
        'layout:item:change', 'ngbFilter:setDefault'];
    eventsNotForShareFromSub = ['layout:item:change'];

    initHub = false;
    projectContext;

    constructor($scope, $compile, $window, $element, $timeout, dispatcher, ngbGoldenLayoutService, GoldenLayout, projectContext) {
        super(dispatcher);
        Object.assign(this, {
            $compile,
            $scope,
            $timeout,
            GoldenLayout,
            dispatcher,
            projectContext
        });
        this.$element = $element.find('[ngb-golden-layout-container]');
        this.service = ngbGoldenLayoutService;
        this.$window = angular.element($window);
    }

    events = {
        'chromosome:change': ::this.panelRemoveExtraWindows,
        'eventHub': (event) => {
            this.emitHubEvent && this.emitHubEvent(event);
        },
        'hotkeyPressed': ::this.handleKeyPressed,
        'layout:item:change': ::this.handlePanelChange,
        'layout:restore:default': ::this.restoreDefault,
        'layout:load': ::this.loadLayout,
        'ngbFilter:change': ::this.panelRemoveExtraWindows,
        'reference:change': ::this.panelRemoveExtraWindows,
        'read:show:mate': ::this.panelAddBrowserWithPairRead,
        'variant:show:pair': ::this.panelAddBrowserWithVariation
    };

    $onDestroy() {
        this.$timeout.cancel(this.resizeEvent);
        this.goldenLayout.destroy();
    }

    onInitLayout() {
        this.initLayout();
        this.initEvents();
    }

    onResize() {
        this.goldenLayout.updateSize();
    }

    initLayout() {

        let layout = this.projectContext.layout;

        const GoldenLayout = this.GoldenLayout.get();

        layout = layout && GoldenLayout.unminifyConfig(layout);

        const savedState = this.service.repairSavedLayout(layout);

        const config = savedState || {...this.service.goldenLayoutDefaultConfig()};

        this.goldenLayout = new GoldenLayout(config, this.$element);

        this.goldenLayout.registerComponent('angularModule', (container, state) => {
            this._initComponent(container, state);
        });


        this.goldenLayout.on('stackCreated', (stack) => {
            const childScope = this.$scope.$new();
            childScope.isVariantsActive = false;

            const html = this.$compile(
                '<li ng-if="isVariantsActive"><ngb-variants-table-column></ngb-variants-table-column></li>')(childScope);

            stack.header.controlsContainer.prepend(html);

            stack.on('activeContentItemChanged', (contentItem) => {
                childScope.isVariantsActive = contentItem.config.componentState.panel === this.panels.ngbVariations;
            });

        });


        this.goldenLayout.init();

        this.goldenLayout.on('stateChanged', (events) => {
            if (events) { //undefined events on destroy
                let layout = this.goldenLayout.toConfig();
                layout = GoldenLayout.minifyConfig(layout);
                this.projectContext.layout = layout;
            }
        });

        this.goldenLayout.on('windowOpened', (event) => {
            this._createCommunicationBetweenWindows(event);
            this.projectContext.openPanel(event._config[0].componentState.panel);
        });

        if (this.goldenLayout.isSubWindow) {
            this._createCommunicationBetweenWindows();
        }
    }

    handleKeyPressed(event) {
        const handledPattern = new RegExp(/^layout>([\w-]*)$/);
        const testResult = handledPattern.exec(event);
        if (testResult) {
            const [,panelName]=testResult;
            if (panelName && this.service.layout.Panels[panelName]) {
                const panelItem = this.service.layout.Panels[panelName];

                let [glItem] = this.goldenLayout.root.getItemsByFilter(obj => obj.config &&
                obj.config.componentState && obj.config.componentState.panel === panelItem.panel);

                if (glItem) {
                    const parent = glItem.parent;
                    if (parent.type === 'stack') {
                        parent.setActiveContentItem(glItem);
                    }
                } else {
                    glItem = this.service.createItem(panelItem);
                    this.addGLItemByPosition(glItem);
                }
            }
        }
    }

    handlePanelChange(event) {
        if (event.layoutChange.displayed === true) {
            const [panel] = this.goldenLayout.root.getItemsByFilter(obj => obj.config &&
            obj.config.componentState && obj.config.componentState.panel === event.layoutChange.panel);
            if (!panel) {
                this.panelAdd(event.layoutChange);
            } else {
                const parent = panel.parent;
                if (parent && parent.type === 'stack') {
                    parent.setActiveContentItem(panel);
                }
            }
        } else {
            this.panelRemove(event.layoutChange);
        }
    }

    _initComponent(container, state) {
        const childScope = this.$scope.$new();
        let attr = '';
        const panel = state.panel;

        if (state.panel === this.panels.ngbBrowser) {
            const defTitle = container._config.title;
            container._config.title = this.$compile(`<ngb-coordinates title="'${defTitle}'"></ngb-coordinates>`)(childScope);
            container._config.title.replace = function () {
            };
        }

        if (state.panel === this.panels.ngbTracksView) {
            const {chromosome, position} = state.browserData;
            const chromosomeName = typeof chromosome === 'object' ? chromosome.name : chromosome;
            const defTitle = `${chromosomeName} : ${position}`;
            attr = `browser-id="'${chromosomeName}:${position}'" chromosome-name="'${chromosomeName}'" position="'${position}'" `;

            container._config.title = this.$compile(
                `<ngb-coordinates  ${attr} title="'${defTitle}'"></ngb-coordinates>`)(childScope);
            container._config.title.replace = function () {
            };
        }

        container.on('destroy', () => {
            childScope.$destroy();
            this.projectContext.closePanel(panel);
        });

        container.on('resize', () => {
            this.$timeout.cancel(this.resizeEvent);
            this.resizeEvent = this.$timeout(() => {
                this.$window.trigger('resize');
            }, 5);
        });

        const element = container.getElement();
        const html = `<${state.htmlModule} layout-fill ${attr}></${state.htmlModule}>`;
        element.html(this.$compile(html)(childScope));

        this.projectContext.openPanel(panel);
    }

    _createCommunicationBetweenWindows() {
        const eventLine = 'communication:line';

        if (this.initHub === false) {
            this.initHub = true;

            const eventFilter = this.goldenLayout.isSubWindow
                ? this.eventsNotForShareFromSub
                : this.eventsNotForShareFromParent;


            this.emitHubEvent = (event) => {
                if (eventFilter.indexOf(event.name) < 0) {
                    this.goldenLayout.eventHub.emit(eventLine, event);
                }
            };

            this.goldenLayout.eventHub.on(eventLine, (event) => {
                this.$timeout(() => {
                    this.dispatcher.emitEventHub(event);
                });
            });

        }

    }

    panelAdd(eventLayoutItem) {
        const newItem = this.service.createItem(eventLayoutItem);
        this.addGLItemByPosition(newItem);
    }

    panelAddBrowserWithVariation(data) {
        this.panelRemoveExtraWindows();

        const newItem = this.service.createBrowserItem(data.variant);
        const [browserItem] = this.goldenLayout.root
            .getItemsByFilter((obj) => obj.config && obj.config.componentState
            && obj.config.componentState.panel === this.panels.ngbBrowser);

        if (browserItem) {
            const stackItem = browserItem.parent;

            if (stackItem.type === 'stack') {
                const rowItem = stackItem.parent;
                const index = rowItem.contentItems.indexOf(stackItem);

                rowItem.addChild(newItem, index + 1);
            }
        }
        else {
            this.addGLItemByPosition(newItem);
        }
    }

    panelAddBrowserWithPairRead(event: PairReadInfo) {
        this.panelRemoveExtraWindows();

        const newItem = this.service.createBrowserItem(event);
        const [browserItem] = this.goldenLayout.root
            .getItemsByFilter(obj => obj.config && obj.config.componentState
            && obj.config.componentState.panel === this.panels.ngbBrowser);

        if (browserItem) {
            const stackItem = browserItem.parent;

            if (stackItem.type === 'stack') {
                const rowItem = stackItem.parent;
                const index = rowItem.contentItems.indexOf(stackItem);

                rowItem.addChild(newItem, index + 1);
            }
        } else {
            this.addGLItemByPosition(newItem);
        }
    }

    addGLItemByPosition(newItem) {
        let index = 0;

        const getItemStackByPosition = () => {
            const itemStacksArr = this.goldenLayout.root.getItemsByType('stack');

            if (itemStacksArr.length) {
                const itemStacksFilteredPositions = itemStacksArr.filter(stack => stack.config.componentState
                && stack.config.componentState.position === newItem.componentState.position);

                if (itemStacksFilteredPositions.length) {
                    return itemStacksFilteredPositions[0];
                }
            }
        };

        let itemForAdd = getItemStackByPosition();

        if (!itemForAdd) {

            const rowArr = this.goldenLayout.root.getItemsByType('row');
            const itemRow = rowArr.length ? rowArr[0] : this.goldenLayout.root.contentItems[0];

            const itemStacksArr = itemRow.contentItems;

            const stack = this.service.createStackItem(newItem.componentState);

            if (newItem.componentState.position === 'left') {
                index = 0;
            }
            else if (newItem.componentState.position === 'right') {
                index = itemStacksArr.length;
            }
            else if (newItem.componentState.position === 'center') {

                if (itemStacksArr.length > 0) {
                    index = itemStacksArr.filter(
                        stack => stack.config.componentState && stack.config.componentState.position === 'left').length;

                }
            }

            itemRow.addChild(stack, index);
            itemForAdd = getItemStackByPosition();
        }

        if (itemForAdd) {
            itemForAdd.addChild(newItem, index);
        }
    }

    panelRemove(event) {
        this.goldenLayout.root
            .getItemsByFilter(obj => obj.config && obj.config.componentState && obj.config.componentState.panel === event.panel)
            .forEach((obj) => {
                obj.remove();
            });
    }

    panelRemoveExtraWindows() {
        if (this.goldenLayout && this.goldenLayout.root) {
            //todo maybe save extrawindow in var and remove it
            this.goldenLayout.root
                .getItemsByFilter(obj => obj.config &&
                obj.config.componentState && obj.config.componentState.panel === this.panels.ngbTracksView)
                .forEach(obj => {
                    obj.remove();
                });
        }

    }

    restoreDefault() {
        this.goldenLayout.destroy();
        const layout = null;
        this.projectContext.layout = layout;
        this.initLayout();
    }

    loadLayout(layout) {
        this.goldenLayout.destroy();
        this.projectContext.layout = layout;
        this.initLayout();
    }

}
