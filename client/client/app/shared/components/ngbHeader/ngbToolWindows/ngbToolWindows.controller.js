export default class ngbToolWindowsController {
    static get UID() {
        return 'ngbToolWindowsController';
    }

    extraWindow = {
        ExtraBrowser: {
            icon: 'video_label',
            panel: 'ngbTracksView',
            position: 'center',
            selfLayout: true,
            title: 'Split View',
            visible: true
        }
    };

    /* @ngInject */
    constructor($mdDialog, appLayout, dispatcher, colorService, projectContext) {
        Object.assign(this, {
            $mdDialog,
            appLayout,
            colorService,
            dispatcher,
            projectContext
        });
    }

    $onInit() {
        this.colors = this.colorService.getAccentColors();
        this.restoreDefaultColor = this.colors[0];
    }

    openMenu($mdOpenMenu, ev) {
        const panelsInDispatcher = this.projectContext.layoutPanels || {};

        let index = 1;

        this.panels = Object.keys(this.appLayout.Panels).map((key) => {
            const panel = Object.assign({}, this.appLayout.Panels[key], {key});
            panel.displayed = panelsInDispatcher && panelsInDispatcher[panel.panel] === true;
            panel.iconColor = this.colors[index++];
            return panel;
        });

        this.extraPanels = Object.keys(this.extraWindow).map((key) => {
            const panel = Object.assign({}, this.extraWindow[key], {key});
            panel.displayed = panelsInDispatcher && panelsInDispatcher[panel.panel] === true;
            panel.iconColor = this.colors[index++];
            return panel;
        }).filter(m => m.displayed === true);


        $mdOpenMenu(ev);
    }

    toggle(item) {
        item.displayed = !item.displayed;
        this.dispatcher.emitGlobalEvent('layout:item:change', {layoutChange: item});
    }

    restoreDefault() {
        this.dispatcher.emitGlobalEvent('layout:restore:default', {});
    }
}
