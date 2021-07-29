import * as PIXI from 'pixi.js';

export default class DomainColorsManager {

    constructor(config) {
        this._config = config;
        this._domainsInfo = {};
        this._genes = [];
        this._totalDomains = 0;
    }

    set domainsInfo(info) {
        this._domainsInfo = info;
        this._updateTotalDomainsCount();
    }

    get domainsInfo() {
        return this._domainsInfo;
    }

    set genesInfo(info) {
        this._genes = info;
    }

    get genesInfo() {
        return this._genes;
    }

    _updateTotalDomainsCount() {
        this._totalDomains = 0;
        for (const key in this._domainsInfo) {
            if (this._domainsInfo.hasOwnProperty(key)) {
                this._totalDomains++;
            }
        }
    }

    getDomainColor(domainName) {
        if (domainName && this.domainsInfo.hasOwnProperty(domainName)) {
            let index = this.domainsInfo[domainName].index;// % this._config.domains.mainColors.length;
            if (index < this._config.domains.mainColors.length) {
                return {fill: this._config.domains.mainColors[index], alpha: 1};
            }
            else {
                index -= this._config.domains.mainColors.length;
                let alphaRatio = Math.floor(index / this._config.domains.additionalColors.length);
                const repeats = Math.floor((this._totalDomains - this._config.domains.mainColors.length) / this._config.domains.additionalColors.length) + 1;
                alphaRatio = 1 - alphaRatio / (repeats + 1);
                return {
                    fill: this._config.domains.additionalColors[index % this._config.domains.additionalColors.length],
                    alpha: alphaRatio
                };
            }
        }
        return {fill: 0xffffff, alpha: 1};
    }

    fillEmptyExon(geneName, graphics: PIXI.Graphics, rect, alpha = 1) {
        const space = this._config.domains.empty.space;
        const side = rect.height;
        const index = this.genesInfo.indexOf(geneName);
        if (index === 0) // transparent color
            return;
        const renderPart = (x) => {
            if (index === 1) {
                if (x < rect.x)
                    return;
                const xCorrected = Math.floor(x / space) * space;
                const start = {
                    x: Math.max(rect.x, Math.min(xCorrected - side, rect.x + rect.width)),
                    y: Math.min(Math.max(rect.y, rect.y + (xCorrected - rect.x)), rect.y + rect.height)
                };
                const end = {
                    x: Math.max(rect.x, Math.min(xCorrected, rect.x + rect.width)),
                    y: Math.min(Math.max(rect.y, rect.y + (xCorrected - (rect.x + rect.width))), rect.y + rect.height)
                };
                graphics.moveTo(start.x, start.y).lineTo(end.x, end.y);
            }
            else {
                if (x > rect.x + rect.width)
                    return;
                const xCorrected = Math.floor(x / space) * space;
                const start = {
                    x: Math.max(rect.x, Math.min(xCorrected, rect.x + rect.width)),
                    y: Math.min(Math.max(rect.y, rect.y + (rect.x - xCorrected)), rect.y + rect.height)
                };
                const end = {
                    x: Math.max(rect.x, Math.min(xCorrected + side, rect.x + rect.width)),
                    y: Math.min(Math.max(rect.y, rect.y + rect.height + (rect.x + rect.width - (xCorrected + side))), rect.y + rect.height)
                };
                graphics.moveTo(start.x, start.y).lineTo(end.x, end.y);
            }
        };
        graphics.lineStyle(1, this._config.domains.empty.color, alpha);
        for (let i = rect.x - side; i < rect.x + rect.width + side; i += space) {
            renderPart(i);
        }
    }

}
