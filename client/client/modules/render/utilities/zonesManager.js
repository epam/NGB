import Sorting from './sorting';
import linearDimensionsConflict from './linearDimensionsConflicts';

export default class ZonesManager {

    _zones = null;

    constructor() {
        this._zones = new Map();
    }

    get zones(): Map {
        return this._zones;
    }

    configureZone(zone, boundaries) {
        this.zones.set(zone, {
            areas: [],
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

    checkArea(zoneName, area, translationVector, maxIterations) {
        const {rect, global} = area;
        let {margin} = area;
        if (rect === null || rect === undefined)
            return null;
        const {translateX, translateY} = translationVector;
        margin = margin ? margin : {
            marginX: 0,
            marginY: 0
        };
        const defaultMaxIterations = 10;
        maxIterations = maxIterations ? maxIterations : defaultMaxIterations;
        const zone = this.zones.get(zoneName);
        if (zone !== undefined && zone !== null) {
            const desiredArea = {
                height: rect.y2 - rect.y1,
                width: rect.x2 - rect.x1,
                x1: rect.x1 + global.x,
                x2: rect.x2 + global.x,
                y1: rect.y1 + global.y,
                y2: rect.y2 + global.y
            };
            let iteration = 0;
            let conflicts = true;
            while (ZonesManager._rectangleFitBoundaries(desiredArea, zone.boundaries) && conflicts && iteration < maxIterations) {
                conflicts = false;
                for (let i = 0; i < zone.areas.length; i++) {
                    const testArea = zone.areas[i];
                    if (ZonesManager._rectanglesConflict(testArea, desiredArea, margin)) {
                        let newX = desiredArea.x1;
                        let newY = desiredArea.y1;
                        if (translateX > 0) {
                            newX = testArea.x2 + margin.marginX;
                        }
                        else if (translateX < 0) {
                            newX = testArea.x1 - desiredArea.width - margin.marginX;
                        }
                        if (translateY > 0) {
                            newY = testArea.y2 + margin.marginY;
                        }
                        else if (translateY < 0) {
                            newY = testArea.y1 - desiredArea.height - margin.marginY;
                        }
                        desiredArea.x1 = newX;
                        desiredArea.x2 = newX + desiredArea.width;
                        desiredArea.y1 = newY;
                        desiredArea.y2 = newY + desiredArea.height;
                        conflicts = true;
                        break;
                    }
                }
                iteration++;
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
        const {rect, global} = area;
        const zone = this.zones.get(zoneName);
        if (zone !== undefined && zone !== null) {
            const submittingArea = {
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
        return linearDimensionsConflict(o1p1, o1p2, o2p1, o2p2, margin);
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
