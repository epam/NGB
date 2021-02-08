import PIXI from 'pixi.js';
import {PixiTextSize} from '../../utilities';
import {drawingConfiguration} from '../../core';
import getRulerHeight from './rulerHeightManager';

const Math = window.Math;

export default class RulerBrush {
    viewport;
    drag = new Map();

    brush = null;
    brushArea = null;
    pickLocationArea = null;
    startCursor = null;
    endCursor = null;
    selectableArea = null;
    selectionRegion = null;
    selectionRegionWidthLabel = null;

    _globalRulerOffsetY = 0;
    _localRulerOffsetY = 0;
    _cursorYOffset = 0;

    _localRulerHeight = 0;
    _globalRulerHeight = 0;

    _eventListenersDestructors = [];

    constructor(callback, viewport, config) {
        this._config = config;
        this.viewport = viewport;
        this._globalRulerHeight = getRulerHeight(this._config.global);
        this._localRulerHeight = getRulerHeight(this._config.local);
        this._globalRulerOffsetY = this._config.brush.line.thickness;
        this._localRulerOffsetY = this._globalRulerOffsetY +
            this._globalRulerHeight +
            this._config.rulersVerticalMargin +
            this._config.brush.line.thickness;
        this._cursorYOffset = this._globalRulerOffsetY - this._config.brush.line.thickness;
        this.moveBrush = callback;
        this.container = new PIXI.Container();
        this.brush = new PIXI.Graphics();
        this.brushArea = this._createInteractiveElement();
        this.shortenedBrush = new PIXI.Graphics();
        this.pickLocationArea = this.createPickLocationArea();
        this.startCursor = this.createCursor();
        this.endCursor = this.createCursor();
        this.selectableArea = this.createSelectableArea();
        this.selectionRegion = new PIXI.Graphics();
        this.selectionRegionWidthLabel = new PIXI.Text('', this._config.brush.drag.label);
        this.selectionRegionWidthLabel.resolution = drawingConfiguration.resolution;

        this.drag.set(this.startCursor, null);
        this.drag.set(this.endCursor, null);
        this.drag.set(this.brushArea, null);
        this.drag.set(this.selectableArea, null);
        this.drag.set(this.pickLocationArea, null);

        this.container.addChild(this.pickLocationArea);
        this.container.addChild(this.brushArea);
        this.container.addChild(this.brush);
        this.container.addChild(this.shortenedBrush);
        this.container.addChild(this.startCursor);
        this.container.addChild(this.endCursor);
        this.container.addChild(this.selectableArea);
        this.container.addChild(this.selectionRegion);
        this.container.addChild(this.selectionRegionWidthLabel);
    }

    clearData() {
        for (let i = 0; i < this._eventListenersDestructors.length; i++) {
            this._eventListenersDestructors[i]();
        }
        this.drag = null;
        this._eventListenersDestructors = null;
    }

    render() {
        this.changeArea();
        this.changeBrush();
        this.changeShortenedBrush();
        this.changeCursor(this.startCursor, this.viewport.brush.start, this.viewport.brush.end);
        this.changeCursor(this.endCursor, this.viewport.brush.end, this.viewport.brush.start);
        this._drawSelection(this.viewport.brush, false, true, true);
    }

    changeBrush() {
        this.brush.clear();
        const thickness = this._config.brush.line.thickness;
        const halfThickness = thickness / 2;
        const normalize = (val) => Math.max(halfThickness, Math.min(this.viewport.canvasSize - halfThickness, val));
        this.brush
            .lineStyle(thickness, this.viewport.isShortenedIntronsMode ? this._config.brushColor.shortenedIntrons : this._config.brushColor.normal, 1)
            .moveTo(0, this._localRulerOffsetY)
            .lineTo(normalize(this.viewport.project.chromoBP2pixel(this.viewport.brush.start) + halfThickness),
                this._localRulerOffsetY)
            .moveTo(normalize(this.viewport.project.chromoBP2pixel(this.viewport.brush.start)), this._localRulerOffsetY)
            .lineTo(normalize(this.viewport.project.chromoBP2pixel(this.viewport.brush.start)),
                this._globalRulerOffsetY - thickness)
            .moveTo(normalize(this.viewport.project.chromoBP2pixel(this.viewport.brush.end)),
                this._globalRulerOffsetY - thickness)
            .lineTo(normalize(this.viewport.project.chromoBP2pixel(this.viewport.brush.end)), this._localRulerOffsetY)
            .moveTo(normalize(this.viewport.project.chromoBP2pixel(this.viewport.brush.end) - halfThickness),
                this._localRulerOffsetY)
            .lineTo(this.viewport.canvasSize, this._localRulerOffsetY);
    }

    changeShortenedBrush() {
        this.shortenedBrush.clear();
        if (this.viewport.isShortenedIntronsMode) {
            const thickness = 1;
            const halfThickness = thickness / 2;
            for (let i = 0; i < this.viewport.shortenedIntronsViewport._coveredRange.ranges.length; i++) {
                const range = this.viewport.shortenedIntronsViewport._coveredRange.ranges[i];
                const x = this.viewport.project.brushBP2pixel(range.startIndex) - this.viewport.factor / 2;
                if (this.viewport.shortenedIntronsViewport.intronLength) {
                    this.shortenedBrush
                        .lineStyle(0, this._config.brushColor.shortenedIntrons, 0)
                        .beginFill(this._config.brushColor.shortenedIntrons, this._config.brushColor.shortenedIntronsAlpha)
                        .drawRect(
                            Math.floor(x - this.viewport.shortenedIntronsViewport.intronLength * this.viewport.factor),
                            this._localRulerOffsetY,
                            2 * this.viewport.shortenedIntronsViewport.intronLength * this.viewport.factor,
                            this._config.local.body.height
                        )
                        .endFill();
                }
                this.shortenedBrush
                    .lineStyle(thickness, this._config.brushColor.shortenedIntrons, 1)
                    .moveTo(Math.floor(x) - halfThickness, this._localRulerOffsetY)
                    .lineTo(Math.floor(x) - halfThickness, this._localRulerOffsetY + this._config.local.body.height);
            }
        }
    }

    createSelectableArea() {
        const area = this._createInteractiveElement(false);
        const white = 0xFFFFFF;
        area.beginFill(white, 0)
            .drawRect(0, this._localRulerOffsetY, this.viewport.canvasSize, this._localRulerHeight);
        return area;
    }

    createPickLocationArea() {
        const area = this._createInteractiveElement(false);
        const white = 0xFFFFFF;
        area.beginFill(white, 0)
            .drawRect(0, this._globalRulerOffsetY, this.viewport.canvasSize, this._globalRulerHeight);
        return area;
    }

    createCursor() {
        const cursor = this._createInteractiveElement();
        cursor.y = this._cursorYOffset;
        return cursor;
    }

    _createInteractiveElement(buttonMode = true) {
        const element = new PIXI.Graphics();
        element.interactive = true;
        element.interactiveChildren = false;
        element.buttonMode = buttonMode;
        const _onDragStart = this.onDragStart.bind(this);
        const _onDragEnd = this.onDragEnd.bind(this);
        const _onDragMove = this.onDragMove.bind(this);
        element
            .on('mousedown', _onDragStart)
            .on('touchstart', _onDragStart)
            .on('mouseup', _onDragEnd)
            .on('mouseupoutside', _onDragEnd)
            .on('touchend', _onDragEnd)
            .on('touchendoutside', _onDragEnd)
            .on('mousemove', _onDragMove)
            .on('touchmove', _onDragMove);
        const eventListenersDestructor = function() {
            element
                .off('mousedown', _onDragStart)
                .off('touchstart', _onDragStart)
                .off('mouseup', _onDragEnd)
                .off('mouseupoutside', _onDragEnd)
                .off('touchend', _onDragEnd)
                .off('touchendoutside', _onDragEnd)
                .off('mousemove', _onDragMove)
                .off('touchmove', _onDragMove);
        };
        this._eventListenersDestructors.push(eventListenersDestructor);
        return element;
    }

    changeArea() {
        this.brushArea.clear();
        const areaPosition = {
            x: this.viewport.project.chromoBP2pixel(this.viewport.brush.start),
            y: this._globalRulerOffsetY
        };
        const areaSize = {
            height: this._config.global.body.height,
            width: this.viewport.convert.chromoBP2pixel(this.viewport.brushSize)
        };
        this.brushArea
            .beginFill(this._config.brush.area.color, this._config.brush.area.alpha)
            .drawRect(areaPosition.x, areaPosition.y, areaSize.width, areaSize.height)
            .endFill();
        const thresholdFactor = 2;
        const notchWidthThreshold = thresholdFactor * this._config.brush.drag.notch.width;
        if (areaSize.width > notchWidthThreshold) {
            this.drawDragNotches(this.brushArea, areaPosition, areaSize);
        }
    }

    changeCursor(cursor, position, oppositePosition) {
        cursor.clear();
        let canvasPosition = this.viewport.project.chromoBP2pixel(position);
        const canvasOppositePosition = this.viewport.project.chromoBP2pixel(oppositePosition);
        const distance = Math.abs(canvasOppositePosition - canvasPosition);
        if (distance < this._config.brush.cursor.width) {
            const center = (canvasPosition + canvasOppositePosition) / 2;
            if (oppositePosition >= position) {
                canvasPosition = center - this._config.brush.cursor.width / 2;
            }
            else {
                canvasPosition = center + this._config.brush.cursor.width / 2;
            }
        }
        cursor.x = Math.min(Math.max(canvasPosition - this._config.brush.cursor.width / 2, 0),
            this.viewport.canvasSize - this._config.brush.cursor.width);
        cursor
            .beginFill(this.viewport.isShortenedIntronsMode ? this._config.brushColor.shortenedIntrons : this._config.brushColor.normal, 1)
            .drawRect(0, 0, this._config.brush.cursor.width, this._config.brush.cursor.height)
            .endFill();
        this.drawNotches(cursor);
    }

    drawNotches(cursor) {
        const thickness = this._config.brush.cursor.notch.thickness;
        const notchMargin = Math.floor(
            (this._config.brush.cursor.width - this._config.brush.cursor.notch.count * thickness) /
            this._config.brush.cursor.notch.count
        );
        const y = (this._config.brush.cursor.height - this._config.brush.cursor.notch.height) / 2;
        const firstNotchPosition = this._config.brush.cursor.width / 2 -
            (this._config.brush.cursor.notch.count - 1) / 2 * (notchMargin + thickness) - thickness / 2;
        for (let notch = 0; notch < this._config.brush.cursor.notch.count; notch++) {
            cursor
                .lineStyle(thickness, this._config.brush.cursor.notch.color, 1)
                .moveTo(firstNotchPosition + notch * (notchMargin + thickness), y)
                .lineTo(firstNotchPosition + notch * (notchMargin + thickness),
                    y + this._config.brush.cursor.notch.height);
        }
    }

    drawDragNotches(area, areaPosition, areaSize) {
        const thickness = this._config.brush.drag.notch.thickness;
        const dY = (areaSize.height - this._config.brush.drag.height) / 2;
        const notchMargin = Math.floor(
            (this._config.brush.drag.height - this._config.brush.drag.notches * thickness) /
            this._config.brush.drag.notches
        );
        const x = areaPosition.x + (areaSize.width - this._config.brush.drag.notch.width) / 2;
        const firstNotchPosition = areaPosition.y + dY + this._config.brush.drag.height / 2 -
            (this._config.brush.drag.notches - 1) / 2 * (notchMargin + thickness) - thickness / 2;
        for (let notch = 0; notch < this._config.brush.drag.notches; notch++) {
            area
                .lineStyle(thickness, this._config.brush.drag.notch.color, 1)
                .moveTo(x, firstNotchPosition + notch * (notchMargin + thickness))
                .lineTo(x + this._config.brush.drag.notch.width,
                    firstNotchPosition + notch * (notchMargin + thickness));
        }
    }

    createNewBrush(target, dragData, dx) {
        if (target === this.startCursor) {
            return {start: Math.min(dragData.brush.start + dx, dragData.brush.end)};
        }
        else if (target === this.endCursor) {
            return {end: Math.max(dragData.brush.end + dx, dragData.brush.start)};
        }
        else if (target === this.brushArea || target === this.pickLocationArea) {
            return {delta: dx};
        }
    }

    onDragStart(event) {
        this.drag.set(event.target, {
            brush: this.viewport.brush,
            point: event.data.global.clone()
        });
        this._drawSelection(null);
        if (this.updateScene) {
            this.updateScene();
        }
    }

    onDragEnd(event) {
        const dragData = this.drag.get(event.target);
        if (dragData) {
            const oldPoint = Math.round(this.viewport.project.pixel2chromoBP(this.container.toLocal(dragData.point).x));
            const newPoint = Math.round(this.viewport.project.pixel2chromoBP(this.container.toLocal(event.data.global).x));
            const wasDragging = oldPoint !== newPoint;
            if (event.target === this.selectableArea && wasDragging) {
                const region = this._getSelectionRegion(event);
                this._doSelection(region);
            }
            else if (event.target === this.pickLocationArea && !wasDragging) {
                if (oldPoint < this.viewport.brush.start || oldPoint > this.viewport.brush.end) {
                    const delta = Math.round(oldPoint - (this.viewport.brush.start + this.viewport.brush.end) / 2);
                    this.moveBrush(this.createNewBrush(event.target, dragData, delta));
                }
            }
            else if (event.target === this.startCursor || event.target === this.endCursor || event.target === this.brushArea) {
                const dx = newPoint - oldPoint;
                this.moveBrush(this.createNewBrush(event.target, dragData, dx));
            }
        }
        this.drag.set(event.target, null);
        this._drawSelection(this.viewport.brush, false, true, true);
        if (this.updateScene) {
            this.updateScene();
        }
    }

    onDragMove(event) {
        const dragData = this.drag.get(event.target);
        if (dragData !== null) {
            const target = event.target;
            if (target === this.selectableArea) {
                const region = this._getSelectionRegion(event);
                if (region) {
                    this._drawSelection(region, true, false);
                }
            }
            else if (target === this.startCursor || target === this.endCursor || target === this.brushArea) {
                const oldPoint = this.container.toLocal(dragData.point);
                const newPoint = this.container.toLocal(event.data.global);
                const dx = this.viewport.convert.pixel2chromoBP(newPoint.x - oldPoint.x);
                this.moveBrush(this.createNewBrush(target, dragData, dx));
                if (event.target === this.brushArea) {
                    this.drag.set(event.target, {
                        brush: this.viewport.brush,
                        point: event.data.global.clone()
                    });
                }
            }
            if (this.updateScene) {
                this.updateScene();
            }
        }
    }

    _getSelectionRegion(event) {
        const dragData = this.drag.get(event.target);
        if (dragData !== null) {
            const oldPoint = Math.max(0, Math.min(this.container.toLocal(dragData.point).x, this.viewport.canvasSize));
            const newPoint = Math.max(0, Math.min(this.container.toLocal(event.data.global).x, this.viewport.canvasSize));
            return {
                end: this.viewport.project.pixel2brushBP(Math.max(oldPoint, newPoint)),
                start: this.viewport.project.pixel2brushBP(Math.min(oldPoint, newPoint))
            };
        }
        return null;
    }

    _drawSelection(selection, drawRegion = false, displayFractionDigits = true, isTotalRegion = false) {
        this.selectionRegion.clear();
        if (selection) {
            const position = {
                x: isTotalRegion ? 0 : this.viewport.project.brushBP2pixel(selection.start),
                y: this._localRulerOffsetY - this._config.brush.line.thickness
            };
            const size = {
                height: this._localRulerHeight + this._config.brush.line.thickness,
                width: isTotalRegion ? this.viewport.canvasSize : this.viewport.project
                    .brushBP2pixel(selection.end) -
                this.viewport.project
                    .brushBP2pixel(selection.start)
            };
            if (drawRegion) {
                this.selectionRegion
                    .beginFill(
                        this.viewport.isShortenedIntronsMode ?
                            this._config.brushColor.shortenedIntrons :
                            this._config.brushColor.normal,
                        this._config.brush.area.selection.alpha
                    )
                    .drawRect(position.x, position.y, size.width, size.height);
            }
            if (!isTotalRegion) {
                const dashSize = 2;
                const dashes = size.height / (2 * dashSize - 1);
                const thickness = 1;
                this.selectionRegion
                    .lineStyle(
                        thickness,
                        this.viewport.isShortenedIntronsMode ?
                            this._config.brushColor.shortenedIntrons :
                            this._config.brushColor.normal,
                        1
                    );
                for (let i = 0; i < dashes; i++) {
                    this.selectionRegion
                        .moveTo(position.x + thickness / 2, position.y + 2 * i * dashSize)
                        .lineTo(position.x + thickness / 2, position.y + (2 * i + 1) * dashSize)
                        .moveTo(position.x + size.width - thickness / 2, position.y + 2 * i * dashSize)
                        .lineTo(position.x + size.width - thickness / 2, position.y + (2 * i + 1) * dashSize);
                }
            }
            const regionWidthText = this._getSelectionDescription(selection, isTotalRegion, displayFractionDigits);
            const labelSize = PixiTextSize.getTextSize(regionWidthText, this._config.brush.drag.label);
            const shouldDisplayLabel = labelSize.width + 2 * this._config.brush.drag.region.arrow.width < size.width;
            if (shouldDisplayLabel) {
                this.selectionRegionWidthLabel.text = regionWidthText;
                this.selectionRegionWidthLabel.x = Math.round(position.x + size.width / 2 - labelSize.width / 2);
                this.selectionRegionWidthLabel.y = Math.round(this._localRulerOffsetY + this._config.local.body.height / 2 - labelSize.height / 2);
            }
            else {
                this.selectionRegionWidthLabel.text = '';
            }
            this._drawSelectionRegionArrows(position, size, shouldDisplayLabel);
        }
        else {
            this.selectionRegionWidthLabel.text = '';
        }
    }

    _getSelectionDescription(selection, isTotalRegion, displayFractionDigits) {
        let selectionDescription = '';
        if (this.viewport.shortenedIntronsViewport.shortenedIntronsMode) {
            const realWidth = `${this._config.brush.drag.formatter(
                Math.floor(selection.end - selection.start + (isTotalRegion ? 1 : 0)),
                displayFractionDigits
            )}bp`;
            const shortenedWidth = `${this._config.brush.drag.formatter(
                Math.floor(this.viewport.shortenedIntronsViewport
                        .getShortenedSize({end: selection.end, start: selection.start}) + (isTotalRegion ? 1 : 0)), 
                displayFractionDigits
            )}bp`;
            selectionDescription = `${shortenedWidth} (${realWidth})`;
        }
        else {
            selectionDescription = `${this._config.brush.drag.formatter(
                Math.floor(selection.end - selection.start + (isTotalRegion ? 1 : 0)), 
                displayFractionDigits
            )}bp`;
        }
        return selectionDescription;
    }

    _drawSelectionRegionArrows(position, size, labelIsVisible = false) {
        const dashSize = 4;
        const dashes = (size.width - 2 * dashSize) / (dashSize * 2);
        const line = this._localRulerOffsetY + this._config.local.body.height / 2;
        const arrowSize = this._config.brush.drag.region.arrow.width * Math.sqrt(2) / 2; // 45`
        this.selectionRegion
            .lineStyle(this._config.brush.drag.region.line.thickness, this._config.brush.drag.region.line.color, 1);
        for (let i = 0; i < dashes; i++) {
            const xStart = position.x + dashSize + 2 * i * dashSize;
            const xEnd = position.x + dashSize + (2 * i + 1) * dashSize;
            if (labelIsVisible) {
                if ((xStart > this.selectionRegionWidthLabel.x && xStart < this.selectionRegionWidthLabel.x + this.selectionRegionWidthLabel.width)
                    ||
                    (xEnd > this.selectionRegionWidthLabel.x && xEnd < this.selectionRegionWidthLabel.x + this.selectionRegionWidthLabel.width)) {
                    continue;
                }
            }
            this.selectionRegion
                .moveTo(xStart, line)
                .lineTo(xEnd, line);
        }
        const arrowMargin = this._config.brush.drag.region.arrow.margin;
        if (size.width > 2 * (arrowSize + arrowMargin)) {
            this.selectionRegion
                .moveTo(position.x + arrowMargin, line)
                .lineTo(position.x + arrowMargin + arrowSize, line - arrowSize)

                .moveTo(position.x + arrowMargin, line)
                .lineTo(position.x + arrowMargin + arrowSize, line + arrowSize)

                .moveTo(position.x + size.width - arrowMargin, line)
                .lineTo(position.x + size.width - arrowMargin - arrowSize, line - arrowSize)

                .moveTo(position.x + size.width - arrowMargin, line)
                .lineTo(position.x + size.width - arrowMargin - arrowSize, line + arrowSize);
        }
    }

    _doSelection(selection) {
        if (selection) {
            this.moveBrush(selection);
        }
    }
}