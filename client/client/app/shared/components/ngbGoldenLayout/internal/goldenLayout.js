import $ from 'jquery';
import {isString} from '../../../utils/String';

export function browserPopoutToConfig() {
    if (!isString(this._config[0].title)) {
        this._config[0].title = this._config[0].componentState.title;
    }
    if (this.getGlInstance()) {
        return {
            content: this.getGlInstance().toConfig().content,
            dimensions: {
                height: this.getGlInstance().height,
                left: this._popoutWindow.screenX || this._popoutWindow.screenLeft,
                top: this._popoutWindow.screenY || this._popoutWindow.screenTop,
                width: this.getGlInstance().width
            },
            indexInParent: this._indexInParent,
            parentId: this._parentId
        };
    }
    return {
        content: this._config,
        dimensions: this._dimensions,
        indexInParent: this._indexInParent,
        parentId: this._parentId
    };
}


export function toConfig(lm) {
    return function (root) {


        if (this.isInitialised === false) {
            throw new Error('Can\'t create config, layout not yet initialised');
        }

        if (root && !( root instanceof lm.items.AbstractContentItem )) {
            throw new Error('Root must be a ContentItem');
        }

        /*
         * settings & labels
         */
        const config = {
            dimensions: lm.utils.copy({}, this.config.dimensions),
            labels: lm.utils.copy({}, this.config.labels),
            settings: lm.utils.copy({}, this.config.settings)
        };

        /*
         * Content
         */
        config.content = [];
        const next = function (configNode, item) {
            let key;

            for (key in item.config) {
                if (item.config.hasOwnProperty(key) && key !== 'content') {

                    if (key === 'title' && !isString(item.config[key])) {
                        configNode[key] = item.config.componentState.title;
                    } else {
                        configNode[key] = item.config[key];
                    }

                }
            }

            if (item.contentItems.length) {
                configNode.content = [];

                for (let i = 0; i < item.contentItems.length; i++) {
                    configNode.content[i] = {};
                    next(configNode.content[i], item.contentItems[i]);
                }
            }
        };

        if (root) {
            next(config, {contentItems: [root]});
        } else {
            next(config, this.root);
        }

        /*
         * Retrieve config for subwindows
         */
        this._$reconcilePopoutWindows();
        config.openPopouts = [];
        for (let i = 0; i < this.openPopouts.length; i++) {
            config.openPopouts.push(this.openPopouts[i].toConfig());
        }

        /*
         * Add maximised item
         */
        config.maximisedItemId = this._maximisedItem ? '__glMaximised' : null;
        return config;
    };
}


export function getQueryStringParam(hash) {

    return function (param) {

        if (hash.indexOf('gl-window') < 0) {
            return null;
        }

        const search = hash.substring(hash.indexOf('?'), hash.length);
        const keyValuePairs = search.substr(1).split('&');
        const params = {};
        let pair;


        for (let i = 0; i < keyValuePairs.length; i++) {
            pair = keyValuePairs[i].split('=');
            params[pair[0]] = pair[1];
        }

        return params[param] || null;
    };
}
export function propagateToChildren(args) {

    let childGl, i;

    for (i = 0; i < this._layoutManager.openPopouts.length; i++) {
        childGl = this._layoutManager.openPopouts[i].getGlInstance();

        if (childGl && childGl !== this._childEventSource) {
            childGl.eventHub._$onEventFromParent(args);
        }
    }
}

export function setTitle(title) {
    isString(title)
        ? this.element.attr('title', title)
        : this.element.attr('title', 'Browser');

    this.titleElement.html(title);
}

export function stripTags(input) {
    return $.trim(input.replace(/(<([^>]+)>)/ig, ''));
}

export function headerButton(iconsSets) {
    return function (header, label, cssClass, action) {
        this._header = header;
        this.element = $(`<li class="${cssClass}" title="${label}"></li>`);

        this._header.on('destroy', () => {
            this.element.off();
            this.element.remove();
        }, this);
        this._action = action;
        this.element.click(this._action);

        this._header.controlsContainer.append(this.element);
        this._header.controlsContainer.find(`.${cssClass}`).append(iconsSets[label]());

    };
}