export const collapsedHeight = 10;

export default class VariantZonesManager {

    _config = null;
    _zones = null;

    get config() {
        return this._config;
    }

    get zones() {
        return this._zones;
    }

    constructor(config) {
        this._config = config;
        this._zones = [];
    }

    getTotalHeight() {
        let totalHeight = 0;
        for (let i = 0; i < this._zones.length; i++) {
            totalHeight += this._getZoneHeight(this._zones[i]);
        }
        return totalHeight;
    }

    parseZonesConfig(zoneNames) {
        if (zoneNames === null || zoneNames === undefined)
            return [];
        const zones = [];
        for (let i = 0; i < zoneNames.length; i++) {
            const subZones = this.parseZonesConfig(zoneNames[i].zones);
            let totalHeight = 0;
            for (let j = 0; j < subZones.length; j++) {
                totalHeight += (subZones[j].height + subZones[j].y);
            }
            totalHeight = totalHeight || this.getZoneDefaultHeight(zoneNames[i].name);
            zones.push({
                name: zoneNames[i].name.toUpperCase(),
                expanded: zoneNames[i].expanded !== undefined ? zoneNames[i].expanded : true,
                zones: subZones,
                y: this.config.zones.margin,
                height: totalHeight
            });
        }
        return zones;
    }

    getZoneDefaultHeight(zoneName) {
        let height = 0;
        switch (zoneName) {
            case 'aminoacid':
                height = this.config.aminoacids.height + 2 * this.config.aminoacids.margin;
                break;
            case 'sequence':
                height = this.config.nucleotide.size.height + 2 * this.config.nucleotide.margin.y;
                break;
            case 'strand':
                height = this.config.strand.height + 2 * this.config.strand.margin;
                break;
            default: {
                if (this.config.hasOwnProperty(zoneName) && this.config[zoneName].hasOwnProperty('height')) {
                    if (this.config[zoneName].hasOwnProperty('margin')) {
                        height = this.config[zoneName].height + 2 * this.config[zoneName].margin;
                    }
                    else {
                        height = this.config[zoneName].height;
                    }
                }
            }
        }
        return height;
    }

    configureZones(zonesConfig) {
        this._zones = this.parseZonesConfig(zonesConfig);
    }

    getStartPosition(...zones) {
        return this._getStartPosition(zones, this._zones);
    }

    getEndPosition(...zones) {
        return this._getEndPosition(zones, this._zones);
    }

    getHeight(...zones) {
        return this._getHeight(zones, this._zones);
    }

    getCenter(...zones) {
        return this.getStartPosition(...zones) + this.getHeight(...zones) / 2;
    }

    expand(...zones) {
        this._expandOrCollapse(zones, this._zones);
    }

    collapse(...zones) {
        this._expandOrCollapse(zones, this._zones, false);
    }

    _expandOrCollapse(zoneNames, zones, expanded = true) {
        if (zoneNames.length === 0 || zones === null || zones === undefined || zones.length === 0) {
            return;
        }
        for (let i = 0; i < zones.length; i++) {
            if (zones[i].name === zoneNames[0].toUpperCase()) {
                if (zoneNames.length === 1) {
                    zones[i].expanded = expanded;
                }
                else {
                    this._expandOrCollapse(zoneNames.slice(1), zones[i].zones, expanded);
                }
            }
        }
    }

    _getStartPosition(zoneNames, zones) {
        if (zoneNames.length === 0 || zones === null || zones === undefined || zones.length === 0) {
            return null;
        }
        let y = 0;
        for (let i = 0; i < zones.length; i++) {
            if (zones[i].name === zoneNames[0].toUpperCase())
                return y + (zones[i].expanded ? zones[i].y : 0) + (this._getStartPosition(zoneNames.slice(1), zones[i].zones) || 0);
            y += (zones[i].expanded ? zones[i].y : 0) + this._getZoneHeight(zones[i]);
        }
        return null;
    }

    _getEndPosition(zoneNames, zones) {
        if (zoneNames.length === 0 || zones === null || zones === undefined || zones.length === 0) {
            return null;
        }
        let y = 0;
        for (let i = 0; i < zones.length; i++) {
            if (zones[i].name === zoneNames[0].toUpperCase())
                return y + (zones[i].expanded ? zones[i].y : 0) + (this._getEndPosition(zoneNames.slice(1), zones[i].zones) || (this._getZoneHeight(zones[i])));
            y += (zones[i].expanded ? zones[i].y : 0) + this._getZoneHeight(zones[i]);
        }
        return null;
    }

    _getHeight(zoneNames, zones) {
        if (zoneNames.length === 0 || zones === null || zones === undefined || zones.length === 0) {
            return null;
        }
        for (let i = 0; i < zones.length; i++){
            if (zones[i].name === zoneNames[0].toUpperCase())
                return this._getHeight(zoneNames.slice(1), zones[i].zones) || this._getZoneHeight(zones[i]);
        }
        return null;
    }

    _getZoneHeight(zone) {
        if (zone && zone.expanded) {
            if (zone.zones.length > 0) {
                let height = 0;
                for (let i = 0; i < zone.zones.length; i++) {
                    height += zone.zones[i].expanded ? this._getZoneHeight(zone.zones[i]) : collapsedHeight;
                }
                return height;
            }
            else {
                return zone.height;
            }
        }
        return collapsedHeight;
    }

    isExpanded(...zones) {
        return this._isExpanded(zones, this._zones);
    }

    _isExpanded(zoneNames, zones) {
        if (zoneNames.length === 0 || zones === null || zones === undefined || zones.length === 0) {
            return false;
        }
        for (let i = 0; i < zones.length; i++){
            if (zones[i].name === zoneNames[0].toUpperCase()) {
                if (zoneNames.length === 1) {
                    return zones[i].expanded;
                }
                return this._isExpanded(zoneNames.slice(1), zones[i].zones);
            }
        }
        return false;
    }

}
