import Sorting from './sorting';

export default class ZonesManager {

    _zones = null;

    useCaches = false;

    constructor() {
        this._zones = new Map();
    }

    get zones(): Map {
        return this._zones;
    }

    configureZone(zone, boundaries) {
        const current = this.zones.get(zone) || {};
        const {areas: cachedAreas = []} = current;
        this.zones.set(zone, {
            areas: [],
            cachedAreas: cachedAreas
                .filter(area => !!area.key),
            boundaries: boundaries
        });
    }

    getZoneBoundaries(zoneName, localPosition = {y: 0}) {
        const {y} = localPosition;
        const zone = this.zones.get(zoneName);
        if (zone !== undefined && zone !== null) {
            return {
                y1: zone.boundaries.y1 - y,
                y2: zone.boundaries.y2 - y
            };
        }
        return null;
    }

    cacheExists(zoneName, area, translationVector) {
        if (!this.useCaches) {
            return false;
        }
        const {key, rect} = area;
        const zone = this.zones.get(zoneName);
        if (!key || !rect || !zone) {
            return false;
        }
        const {translateX, translateY} = translationVector;
        const {cachedAreas = []} = zone;
        if ((!translateY && !translateX) || cachedAreas.length === 0) {
            return false;
        }
        const desiredArea = {
            height: rect.y2 - rect.y1,
            width: rect.x2 - rect.x1
        };
        const sameKeys = cachedAreas.filter(a => !!key && a.key === key);
        const equalDimensions = (a, b) => Math.abs(a - b) < 1;
        const [cache] = sameKeys
            .filter(a => equalDimensions(a.width, desiredArea.width) &&
                equalDimensions(a.height, desiredArea.height)
            );
        return !!cache;
    }

    checkArea(zoneName, area, translationVector) {
        const {key, rect, global} = area || {};
        let {margin} = area;
        if (rect === null || rect === undefined)
            return null;
        const {translateX, translateY} = translationVector;
        margin = margin ? margin : {
            marginX: 0,
            marginY: 0
        };
        const zone = this.zones.get(zoneName);
        if (zone !== undefined && zone !== null) {
            const {cachedAreas = []} = zone;
            const desiredArea = {
                height: rect.y2 - rect.y1,
                width: rect.x2 - rect.x1,
                x1: rect.x1 + global.x,
                x2: rect.x2 + global.x,
                y1: rect.y1 + global.y,
                y2: rect.y2 + global.y
            };
            const [cache] = this.useCaches
                ? cachedAreas
                    .filter(a => !!key && a.key === key &&
                        a.width === desiredArea.width &&
                        a.height === desiredArea.height
                    )
                : [];
            let conflicts = true;
            if (cache && (translateY || translateX)) {
                conflicts = false;
                if (translateY) {
                    desiredArea.y1 = cache.y1;
                    desiredArea.y2 = cache.y2;
                }
                if (translateX) {
                    desiredArea.x1 = cache.x1;
                    desiredArea.x2 = cache.x2;
                }
                cachedAreas.splice(cachedAreas.indexOf(cache), 1);
            } else {
                const checkZone = {
                    height: translateY !== 0 ? Infinity : desiredArea.height,
                    y1: translateY !== 0 ? -Infinity : desiredArea.y1,
                    y2: translateY !== 0 ? Infinity : desiredArea.y2,
                    width: translateX !== 0 ? Infinity : desiredArea.width,
                    x1: translateX !== 0 ? -Infinity : desiredArea.x1,
                    x2: translateX !== 0 ? Infinity : desiredArea.x2
                };
                const areasToCheck = zone.areas.filter(area => ZonesManager._rectanglesConflict(area, checkZone, margin));
                const testXZones = ZonesManager._getEmptyZones(zone.boundaries, areasToCheck, 'x', desiredArea.width + 2 * margin.marginX);
                const testYZones = ZonesManager._getEmptyZones(zone.boundaries, areasToCheck, 'y', desiredArea.height + 2 * margin.marginY);
                const yCandidate = ZonesManager._getZonePlacement(
                    testYZones,
                    translateY,
                    desiredArea.y1,
                    desiredArea.y2,
                    margin.marginX
                );
                const xCandidate = ZonesManager._getZonePlacement(
                    testXZones,
                    translateX,
                    desiredArea.x1,
                    desiredArea.x2,
                    margin.marginX
                );
                if (!xCandidate || !yCandidate) {
                    conflicts = true;
                } else {
                    conflicts = false;
                    desiredArea.x1 = xCandidate.start;
                    desiredArea.x2 = xCandidate.end;
                    desiredArea.y1 = yCandidate.start;
                    desiredArea.y2 = yCandidate.end;
                }
            }
            conflicts = conflicts || !ZonesManager._rectangleFitBoundaries(desiredArea, zone.boundaries);
            if (conflicts) {
                desiredArea.x1 = rect.x1;
                desiredArea.x2 = rect.x2;
                desiredArea.y1 = rect.y1;
                desiredArea.y2 = rect.y2;
            }
            desiredArea.x1 -= global.x;
            desiredArea.x2 -= global.x;
            desiredArea.y1 -= global.y;
            desiredArea.y2 -= global.y;
            return {
                key,
                conflicts: conflicts,
                global: global,
                margin: margin,
                rect: desiredArea
            };
        }
        return null;
    }

    static checkRectConflicts(zone, rect) {
        for (let i = 0; i < zone.areas.length; i++) {
            const testArea = zone.areas[i];
            if (ZonesManager._rectanglesConflict(testArea, rect)) {
                return true;
            }
        }
        return false;
    }

    submitArea(zoneName, area) {
        const {key, rect, global} = area;
        const zone = this.zones.get(zoneName);
        if (zone !== undefined && zone !== null) {
            const submittingArea = {
                key,
                height: rect.y2 - rect.y1,
                width: rect.x2 - rect.x1,
                x1: rect.x1 + global.x,
                x2: rect.x2 + global.x,
                y1: rect.y1 + global.y,
                y2: rect.y2 + global.y
            };
            zone.areas.push(submittingArea);
        }
    }

    getZoneActualRange(zoneName) {
        const zone = this.zones.get(zoneName);
        const range = {
            x1: null,
            x2: null,
            y1: null,
            y2: null
        };
        if (!zone) {
            return range;
        }
        for (let i = 0; i < zone.areas.length; i++) {
            const areaRect = zone.areas[i];
            if (!range.x1 || range.x1 > areaRect.x1)
                range.x1 = areaRect.x1;
            if (!range.x2 || range.x2 < areaRect.x2)
                range.x2 = areaRect.x2;
            if (!range.y1 || range.y1 > areaRect.y1)
                range.y1 = areaRect.y1;
            if (!range.y2 || range.y2 < areaRect.y2)
                range.y2 = areaRect.y2;
        }
        return range;
    }

    getZoneMask(zoneName, boundaries) {
        let xCoordinates = [];
        let yCoordinates = [];
        const zone = this.zones.get(zoneName);
        if (boundaries) {
            xCoordinates.push(boundaries.x1);
            xCoordinates.push(boundaries.x2);
            yCoordinates.push(boundaries.y1);
            yCoordinates.push(boundaries.y2);
        }
        if (zone !== undefined && zone !== null) {
            for (let i = 0; i < zone.areas.length; i++) {
                const areaRect = zone.areas[i];
                xCoordinates.push(areaRect.x1);
                xCoordinates.push(areaRect.x2);
                yCoordinates.push(areaRect.y1);
                yCoordinates.push(areaRect.y2);
            }
        }
        xCoordinates = Sorting.quickSort(xCoordinates);
        yCoordinates = Sorting.quickSort(yCoordinates);
        const xCoordinatesFiltered = [];
        const yCoordinatesFiltered = [];
        let prev = null;
        for (let i = 0; i < xCoordinates.length; i++) {
            if (prev === null || xCoordinates[i] !== prev) {
                xCoordinatesFiltered.push(xCoordinates[i]);
                prev = xCoordinates[i];
            }
        }
        prev = null;
        for (let i = 0; i < yCoordinates.length; i++) {
            if (prev === null || yCoordinates[i] !== prev) {
                yCoordinatesFiltered.push(yCoordinates[i]);
                prev = yCoordinates[i];
            }
        }
        const rects = [];
        for (let yIndex = 0; yIndex < yCoordinatesFiltered.length - 1; yIndex++) {

            for (let xIndex = 0; xIndex < xCoordinatesFiltered.length - 1; xIndex++) {
                const rect = {
                    x1: xCoordinatesFiltered[xIndex],
                    x2: xCoordinatesFiltered[xIndex + 1],
                    y1: yCoordinatesFiltered[yIndex],
                    y2: yCoordinatesFiltered[yIndex + 1]
                };
                if (!ZonesManager.checkRectConflicts(zone, rect)) {
                    rects.push(rect);
                }
            }
        }
        return rects;
    }

    static _getAreasPoints (areas, extend, ...properties) {
        const points = (areas || []).map(area =>
            properties
                .map(p => area.hasOwnProperty(p) ? area[p] : undefined)
                .filter(p => p !== undefined)
        )
            .reduce((r, c) => ([...r, ...c]), []);
        return [...(new Set(points.concat(extend)))]
            .sort((a, b) => a - b);
    }

    static _getEmptyZones (boundaries, areas, axis = 'x', ofSize) {
        const isX = /^x$/i.test(axis);
        const {
            x1 = -Infinity,
            x2 = Infinity,
            y1 = -Infinity,
            y2 = Infinity
        } = boundaries || {};
        const coordinates = ZonesManager._getAreasPoints(
            areas,
            (isX ? [x1, x2] : [y1, y2]),
            ...(isX ? ['x1', 'x2'] : ['y1', 'y2'])
        );
        function intersects (test, o) {
            const {x1, x2, y1, y2} = test;
            const {start, end} = o;
            const tStart = isX ? Math.min(x1, x2) : Math.min(y1, y2);
            const tEnd = isX ? Math.max(x1, x2) : Math.max(y1, y2);
            if (tEnd < start) {
                return false;
            }
            if (end < tStart) {
                return false;
            }
            const o1size = Math.abs(tEnd - tStart);
            const o2size = Math.abs(end - start);
            const oSize = Math.max(tEnd, tStart, end, start) - Math.min(tEnd, tStart, end, start);
            return o1size + o2size > oSize;
        }
        const zones = [];
        for (let i = 1; i <= coordinates.length - 1; i++) {
            const c1 = coordinates[i - 1];
            const c2 = coordinates[i];
            if (c2 - c1 > ofSize) {
                let isEmpty = true;
                for (let a = 0; a < areas.length; a++) {
                    if (intersects(areas[a], {start: c1, end: c2})) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    zones.push({
                        start: c1,
                        end: c2
                    });
                }
            }
        }
        return zones;
    }

    static _getZonePlacement (zones, translate, start, end, margin) {
        const _start = Math.min(start, end);
        const _end = Math.max(start, end);
        const size = _end - _start;
        const _center = (_start + _end) / 2.0;
        function getZoneDistance (zone) {
            const {start: zStart, end: zEnd} = zone;
            if (zStart === -Infinity && zEnd === Infinity) {
                return 0;
            }
            if (zStart <= _start && zEnd >= _end) {
                return 0;
            }
            return (zStart + zEnd) / 2.0 - _center;
        }
        const [candidate] = zones
            .map(zone => ({...zone, distance: getZoneDistance(zone)}))
            .filter(zone => zone.distance === 0 ||
                Math.sign(zone.distance) === Math.sign(translate)
            )
            .sort((a, b) => Math.abs(a.distance) - Math.abs(b.distance));
        if (!candidate || translate === 0 || candidate.distance === 0) {
            return {
                start: _start,
                end: _end
            };
        }
        if (candidate.start === -Infinity || translate < 0) {
            return {
                start: candidate.end - margin - size,
                end: candidate.end - margin
            };
        }
        if (candidate.end === Infinity || translate > 0) {
            return {
                start: candidate.start + margin,
                end: candidate.start + margin + size
            };
        }
        return {
            start: _start,
            end: _end
        };
    }

    static _rectangleFitBoundaries(rect, boundaries) {
        return (boundaries.y1 === null || boundaries.y1 === undefined || rect.y1 >= boundaries.y1) &&
            (boundaries.y2 === null || boundaries.y2 === undefined || rect.y2 <= boundaries.y2);
    }

    static _linearDimensionsConflict(prop1, prop2, o1, o2, margin = 0) {
        if (
            !o1 ||
            !o2 ||
            !o1.hasOwnProperty(prop1) || !o1.hasOwnProperty(prop2) ||
            !o2.hasOwnProperty(prop1) || !o2.hasOwnProperty(prop2)
        ) {
            return false;
        }
        const o1p1 = o1[prop1];
        const o1p2 = o1[prop2];
        const o2p1 = o2[prop1];
        const o2p2 = o2[prop2];
        const o1size = Math.abs(o1p2 - o1p1);
        const o2size = Math.abs(o2p2 - o2p1);
        const oSize = Math.max(o1p1, o1p2, o2p1, o2p2) - Math.min(o1p1, o1p2, o2p1, o2p2);
        return o1size + o2size + margin >= oSize;
    }

    static _rectanglesConflict(rect1, rect2, margin = {marginX: 0, marginY: 0}) {
        const {marginX, marginY} = margin;
        const xConflicts = this._linearDimensionsConflict(
            'x1',
            'x2',
            rect1,
            rect2,
            marginX
        );
        const yConflicts = this._linearDimensionsConflict(
            'y1',
            'y2',
            rect1,
            rect2,
            marginY
        );
        return xConflicts && yConflicts;
    }
}
