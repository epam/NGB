import {stringToDash} from '../../utils/String';
import {parseObject} from '../../utils/Object';
import deepExtend from 'deep-extend';

import {EventVariationInfo} from '../../utils/events';

export default class ngbGoldenLayout {

    static instance(appLayout, ngbGoldenLayoutConstant) {
        return new ngbGoldenLayout(appLayout, ngbGoldenLayoutConstant);
    }

    constructor(appLayout, ngbGoldenLayoutConstant) {
        this.layout = appLayout;
        this.GlConfig = ngbGoldenLayoutConstant;
    }

    getPanels(position) {
        const modules = Object.keys(this.layout.Panels).map(key => Object.assign({}, this.layout.Panels[key], {key}));

        return modules
            .filter(m => m.position === position)
            .filter(m => m.displayed !== false)
            .map(m => this.createItem(m));
    }


    repairSavedLayout(root) {
        if (!root)return root;

        const defComponentsObjs = Object.values(this.layout.Panels);
        const defComponentsPnls = defComponentsObjs.map(m => m.panel);

        //1 - check module name is exist in appLayout
        //panel
        this._removeItemFromLayout(root, (comp) => {
            if (comp.type === 'component' && comp.componentState && comp.componentState.panel) {
                if (defComponentsPnls.indexOf(comp.componentState.panel) < 0) {
                    return true;
                }
            }
            return false;
        });

        //2 - icon, position, title, selfLayout
        const componentsItem = parseObject(root, (obj) => {
            if (obj.type === 'component') {
                return obj;
            }
            return false;
        });

        componentsItem.forEach((comp) => {
            const defValue = defComponentsObjs.filter(m => m.panel === comp.componentState.panel);
            if (defValue.length) {
                Object.assign(comp.componentState, defValue[0]);
                comp.title = defValue[0].title;
            }

        });

        //3 - remove empty stack
        this._removeItemFromLayout(root, (comp) => (comp.type === 'stack' && (!comp.content || comp.content.length === 0)));

        //4 fixed dimension
        Object.assign(root.dimensions, this.GlConfig.dimensions);


        return root;
    }

    createItem(appLayoutConstItem) {
        return {
            title: appLayoutConstItem.title,
            type: 'component',
            componentName: 'angularModule',
            componentState: {
                ...appLayoutConstItem,
                htmlModule: stringToDash(appLayoutConstItem.panel)
            }
        };
    }

    createBrowserItem(event: EventVariationInfo) {
        const configItem = Object.assign({}, this.layout.Panels.browser);
        configItem.panel = 'ngbTracksView';
        configItem.browserData = event.endPoint;
        return this.createItem(configItem);
    }

    createStackItem(appLayoutConstItem) {

        const column = this.layout.Columns[appLayoutConstItem.position]
            ? this.layout.Columns[appLayoutConstItem.position]
            : {width: appLayoutConstItem.position === 'center' ? 60 : 30};

        return {
            type: 'stack',
            width: column.width || 30,
            componentState: {
                position: appLayoutConstItem.position
            }
        };
    }

    goldenLayoutDefaultConfig() {
        const root = deepExtend({}, this.GlConfig);

        const rowArr = parseObject(root, (obj) => {
            if (obj.type === 'row') {
                return obj;
            }
            return false;
        });

        for (const key in this.layout.Columns) {
            if (this.layout.Columns.hasOwnProperty(key)) {
                rowArr[0].content.push({
                    type: 'stack',
                    componentState: {
                        position: key
                    }
                });
            }
        }

        rowArr[0].content.forEach((obj) => {
            const column = this.layout.Columns[obj.componentState.position];
            obj.width = column.width || 30;
            obj.content = obj.content || [];
            obj.content.push(...this.getPanels(obj.componentState.position));
        });

        return root;
    }

    _removeItemFromLayout(root, fnForObject) {

        const findItems = (obj) => {

            if (obj instanceof Array) {
                for (let i = 0; i < obj.length; i++) {
                    if (fnForObject(obj[i]) === true) {
                        obj.splice(i, 1);
                    } else findItems(obj[i]);
                }
            } else if (obj instanceof Object) {
                if (obj.content) {
                    findItems(obj.content);
                    if (obj.activeItemIndex !== undefined && obj.content instanceof Array) {
                        obj.activeItemIndex = Math.min(obj.content.length - 1, obj.activeItemIndex);
                    }
                }
            }
        };

        findItems(root);
    }
}