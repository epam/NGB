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
        ngbVariations: 'ngbVariantsTablePanel',
        ngbDataSets: 'ngbDataSets',
        ngbLog: 'ngbLog',
        ngbBlatSearchPanel: 'ngbBlatSearchPanel',
        ngbBlastSearchPanel: 'ngbBlastSearchPanel',
    };
    eventsNotForShareFromParent = ['layout:panels:displayed', 'layout:restore:default', 'layout:load',
        'layout:item:change', 'ngbFilter:setDefault'];
    eventsNotForShareFromSub = ['layout:item:change'];

    initHub = false;
    projectContext;
    ngbViewActions;

    constructor($scope, $compile, $window, $element, $timeout, dispatcher, ngbGoldenLayoutService, GoldenLayout, projectContext, ngbViewActionsConstant, appLayout) {
        super(dispatcher);
        Object.assign(this, {
            $compile,
            $scope,
            $timeout,
            GoldenLayout,
            dispatcher,
            projectContext,
            appLayout,
            ngbViewActions: ngbViewActionsConstant
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
        'read:show:blat': ::this.panelAddBlatSearchPanel,
        'read:show:blast': :: this.panelAddBlastSearchPanel,
        'tracks:state:change': ::this.panelRemoveBlatSearchPanel,
        'tracks:state:change:blat': ::this.panelRemoveBlatSearchPanel,
        'tracks:state:change:blast': ::this.panelRemoveBlastSearchPanel,
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
            Object.assign(childScope, this.ngbViewActions);
            childScope.tracksAreSelected = () => this.projectContext.tracks && this.projectContext.tracks.length > 0;
            childScope.projectContext = this.projectContext;

            const viewActionsTemplate = require('./ngbViewActions/ngbViewActions.tpl.html');

            const html = this.$compile(viewActionsTemplate)(childScope);

            stack.header.controlsContainer.prepend(html);

            stack.on('activeContentItemChanged', (contentItem) => {
                childScope.viewName = contentItem.config.componentState.panel;
                if (contentItem.config.componentState.panel === this.panels.ngbVariations) {
                    this.dispatcher.emit('activeVariants');
                }
                if (contentItem.config.componentState.panel === this.panels.ngbDataSets) {
                    this.dispatcher.emit('activeDataSets');
                }
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
            const [, panelName] = testResult;
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
            if (event.layoutChange.panel === 'ngbMotifsPanel') {
                const [dataSetsPanel] = this.goldenLayout.root.getItemsByFilter(obj => obj.config &&
                    obj.config.componentState && obj.config.componentState.panel === 'ngbDataSets');
                if (dataSetsPanel) {
                    const parent = dataSetsPanel.parent;
                    if (parent && parent.type === 'stack') {
                        parent.setActiveContentItem(dataSetsPanel);
                    }
                }
            }
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

    panelRemoveBlatSearchPanel() {
        const [blatSearchItem] = this.goldenLayout.root
            .getItemsByFilter((obj) => obj.config && obj.config.componentState
                && obj.config.componentState.panel === this.panels.ngbBlatSearchPanel);

        if (!blatSearchItem) {
            return;
        }

        const savedBlatRequest = JSON.parse(localStorage.getItem('blatSearchRequest')) || null;

        if(!savedBlatRequest) {
            return;
        }

        const [currentBlatSearchBamTrack] = this.projectContext.tracks.filter(t => t.format === 'BAM' && t.id === savedBlatRequest.id);

        if(!currentBlatSearchBamTrack) {
            this.panelRemove(this.appLayout.Panels.blat);
        }
    }

    panelRemoveBlastSearchPanel() {
        const [blastSearchItem] = this.goldenLayout.root
            .getItemsByFilter((obj) => obj.config && obj.config.componentState
                && obj.config.componentState.panel === this.panels.ngbBlastSearchPanel);

        if (!blastSearchItem) {
            return;
        }

        const savedBlastRequest = JSON.parse(localStorage.getItem('blastSearchRequest')) || null;

        if(!savedBlastRequest) {
            return;
        }

        const [currentBlastSearchBamTrack] = this.projectContext.tracks.filter(t => t.format === 'BAM' && t.id === savedBlastRequest.id);

        if(!currentBlastSearchBamTrack) {
            this.panelRemove(this.appLayout.Panels.blast);
        }
    }

    blatSearchPanelDestroyedHandler(item) {
        if (item.type === 'component') {
            if (item.config.componentState.panel === this.panels.ngbBlatSearchPanel) {
                this.blatSearchPanelRemoved();
                this.goldenLayout.off('itemDestroyed', this.blatSearchPanelDestroyedHandler, this);
            }
        }
    }
    blastSearchPanelDestroyedHandler(item) {
        if (item.type === 'component') {
            if (item.config.componentState.panel === this.panels.ngbBlastSearchPanel) {
                this.blastSearchPanelRemoved();
                this.goldenLayout.off('itemDestroyed', this.blastSearchPanelDestroyedHandler, this);
            }
        }
    }

    blatSearchPanelRemoved() {
        localStorage.removeItem('blatSearchRequest');
        localStorage.removeItem('blatColumns');

        this.projectContext.changeState({ blatRegion: { forceReset: true } });
    }

    blastSearchPanelRemoved() {
        localStorage.removeItem('blastSearchRequest');
        localStorage.removeItem('blastSearchResultColumns');
        localStorage.removeItem('blastHistoryColumns');

        this.projectContext.changeState({ blastRegion: { forceReset: true } });
    }

    panelAddBlatSearchPanel(event) {
        const layoutChange = this.appLayout.Panels.blat;
        layoutChange.displayed = true;

        const [blatSearchItem] = this.goldenLayout.root
            .getItemsByFilter((obj) => obj.config && obj.config.componentState
            && obj.config.componentState.panel === this.panels.ngbBlatSearchPanel);

        const payload = {
            id: event.id,
            referenceId: event.referenceId,
            chromosomeId: event.chromosomeId,
            startIndex: event.startIndex,
            endIndex: event.endIndex,
            name: event.name,
            openByUrl: event.openByUrl,
            file: event.file,
            index: event.index
        };
        localStorage.setItem('blatSearchRequest', JSON.stringify(payload || {}));

        this.goldenLayout.on('itemDestroyed', this.blatSearchPanelDestroyedHandler, this);

        if (!blatSearchItem) {
            this.panelAdd(layoutChange);
        } else {
            const parent = blatSearchItem.parent;
            if (parent && parent.type === 'stack') {
                parent.setActiveContentItem(blatSearchItem);
            }
        }
    }
    panelAddBlastSearchPanel(event) {
        const layoutChange = this.appLayout.Panels.blast;
        layoutChange.displayed = true;

        const [blastSearchItem] = this.goldenLayout.root
            .getItemsByFilter((obj) => obj.config && obj.config.componentState
                && obj.config.componentState.panel === this.panels.ngbBlastSearchPanel);

        const payload = {
            ...event,
            id: event.id,
            referenceId: event.referenceId,
            chromosomeId: event.chromosomeId,
            startIndex: event.startIndex,
            endIndex: event.endIndex,
            name: event.name,
            openByUrl: event.openByUrl,
            file: event.file,
            index: event.index,
            source: event.source
        };
        localStorage.setItem('blastSearchRequest', JSON.stringify(payload || {}));

        this.goldenLayout.on('itemDestroyed', this.blastSearchPanelDestroyedHandler, this);

        if (!blastSearchItem) {
            this.panelAdd(layoutChange);
        } else {
            const parent = blastSearchItem.parent;
            if (parent && parent.type === 'stack') {
                parent.setActiveContentItem(blastSearchItem);
            }
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
