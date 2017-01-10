import {
    browserPopoutToConfig,
    getQueryStringParam,
    headerButton,
    propagateToChildren,
    setTitle,
    stripTags,
    toConfig
} from './internal/goldenLayout';
import GoldenLayout from '../../../compat/goldenLayout';

export default class GoldenLayoutClass {

    static instance($location, $rootScope, $compile) {
        return new GoldenLayoutClass($location, $rootScope, $compile);
    }

    constructor($location, $rootScope, $compile) {


        const iconsSet = {
            close: () => {
                const childScope = $rootScope.$new();
                return $compile(require('./internal/close.tpl.html'))(childScope);
            },
            maximise: () => {
                const childScope = $rootScope.$new();
                return $compile(require('./internal/maximise.tpl.html'))(childScope);
            },
            minimise: () => {
                const childScope = $rootScope.$new();
                return $compile(require('./internal/maximise.tpl.html'))(childScope);
            },
            popout: () => {
                const childScope = $rootScope.$new();
                return $compile(require('./internal/popout.tpl.html'))(childScope);
            }
        };

        GoldenLayout.__lm.controls.BrowserPopout.prototype.toConfig = browserPopoutToConfig;
        GoldenLayout.__lm.utils.getQueryStringParam = getQueryStringParam($location.$$url);
        GoldenLayout.__lm.utils.stripTags = stripTags;
        GoldenLayout.__lm.LayoutManager.prototype.toConfig = toConfig(GoldenLayout.__lm);
        GoldenLayout.__lm.utils.EventHub.prototype._propagateToChildren = propagateToChildren;
        GoldenLayout.__lm.controls.Tab.prototype.setTitle = setTitle;
        GoldenLayout.__lm.controls.HeaderButton = headerButton(iconsSet);


        this.GoldenLayoutInstance = GoldenLayout;
    }

    get() {
        return this.GoldenLayoutInstance;
    }

}

