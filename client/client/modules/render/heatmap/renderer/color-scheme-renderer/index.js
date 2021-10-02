import * as PIXI from 'pixi.js-legacy';
import InteractiveZone from '../../interactions/interactive-zone';
import {colorSchemes} from '../../color-scheme';
import config from './config';
import events from '../../utilities/events';
import getDisplayValue from '../../../utilities/getDisplayValue';
import makeInitializable from '../../utilities/make-initializable';

function getBase (...values) {
    if (values.length < 2) {
        return undefined;
    }
    const sorted = values
        .slice()
        .sort((a, b) => a - b);
    let minDiff = Infinity;
    for (let v = 1; v < sorted.length; v += 1) {
        const diff = sorted[v] - sorted[v - 1];
        if (diff < minDiff) {
            minDiff = diff;
        }
    }
    if (minDiff === 0) {
        return undefined;
    }
    return Math.floor(Math.log10(Math.abs(minDiff)));
}

/**
 * @typedef {Object} ColorSchemeRenderOptions
 * @property {number} height
 */

class ColorSchemeRenderer extends InteractiveZone {
    /**
     *
     * @param {ColorScheme} colorScheme
     * @param {LabelsManager} labelsManager
     */
    constructor(colorScheme, labelsManager) {
        super({priority: InteractiveZone.Priorities.colorScheme});
        makeInitializable(
            this,
            {
                isInitialized: () => this.colorScheme && this.colorScheme.initialized
            }
        );
        this.container = new PIXI.Container();
        /**
         * Heatmap color scheme
         * @type {ColorScheme}
         */
        this.colorScheme = colorScheme;
        this.onColorSchemeChangedCallback = this.initialize.bind(this);
        this.colorScheme.onChanged(this.onColorSchemeChangedCallback);
        this.colorScheme.onInitialized(this.onColorSchemeChangedCallback);
        /**
         * Labels manager
         * @type {LabelsManager}
         */
        this.labelsManager = labelsManager;
        this.labels = [];
        this.discreteLabels = new Map();
        this.initialized = false;
        this.initialize();
    }

    destroy() {
        if (this.container) {
            this.container.destroy(true);
        }
        if (this.colorScheme) {
            this.colorScheme.removeEventListeners(this.onColorSchemeChangedCallback);
        }
        this.labelsManager = undefined;
        this.colorScheme = undefined;
        this.labels = [];
        this.discreteLabels = new Map();
        super.destroy();
    }

    get width() {
        if (!this.initialized) {
            return 0;
        }
        if (this.colorScheme.type === colorSchemes.continuous) {
            return config.margin * 2.0
                + config.continuous.width
                + Math.max(
                    ...this.labels.map(label => label.width + 2.0 * config.label.margin),
                    0
                );
        }
        return config.margin * 2.0 + this._discreteTotalWidth;
    }

    initialize() {
        this.container.removeChildren();
        this.container.interactive = true;
        this.container.buttonMode = true;
        this._discreteTotalWidth = 0;
        this.labels = [];
        this.discreteLabels = new Map();
        this.continuousContainer = new PIXI.Container();
        this.discreteContainer = new PIXI.Container();
        this.container.addChild(this.continuousContainer);
        this.container.addChild(this.discreteContainer);
        this.discreteLabelsContainer = new PIXI.Container();
        this.discreteGraphics = new PIXI.Graphics();
        this.discreteContainer.addChild(this.discreteGraphics);
        this.discreteContainer.addChild(this.discreteLabelsContainer);
        if (
            this.colorScheme.initialized &&
            this.colorScheme.type === colorSchemes.discrete &&
            this.labelsManager
        ) {
            for (const gradient of this.colorScheme.gradientCollection.values()) {
                const description = gradient.description;
                const label = this.labelsManager.getLabel(description, config.label.font);
                if (label) {
                    this.discreteLabelsContainer.addChild(label);
                    label.visible = false;
                    this.discreteLabels.set(gradient.key, label);
                }
            }
        }
        this.clearSession();
        this.initialized = true;
        this.emit(events.render.request);
    }

    test(event) {
        if (event) {
            const {
                x: x1,
                y: y1,
                width,
                height
            } = this.container.getBounds(true);
            const x2 = x1 + width;
            const y2 = y1 + height;
            return event.globalX >= x1 &&
                event.globalX <= x2 &&
                event.globalY >= y1 &&
                event.globalY <= y2;
        }
        return false;
    }

    onClick(event) {
        super.onClick(event);
        if (this.colorScheme) {
            this.colorScheme.configureRequest();
        }
    }

    clearSession() {
        this.session = {};
    }

    renderContinuous(height = 0) {
        const newHeight = Math.min(config.continuous.maxHeight, height);
        if (this.session.continuousHeight !== newHeight) {
            this.continuousContainer.removeChildren();
            const labelsContainer = new PIXI.Container();
            const addLabel = (value, base) => {
                if (!this.labelsManager) {
                    return;
                }
                const text = getDisplayValue(value, base);
                const label = this.labelsManager.getLabel(text, config.label.font);
                if (label) {
                    label.x = Math.round(
                        config.margin + config.label.margin
                    );
                    label.y = Math.round(
                        (this.colorScheme.maximum - value)
                        / (this.colorScheme.maximum - this.colorScheme.minimum)
                        * (newHeight - label.height)
                    );
                    labelsContainer.addChild(label);
                    this.labels.push(label);
                }
            };
            const labelsConfig = [
                this.colorScheme.minimum,
                this.colorScheme.maximum
            ];
            const base = getBase(...labelsConfig.map(label => label.value));
            labelsConfig.forEach(config => addLabel(config, base));
            const offset = config.margin +
                Math.max(...this.labels.map(label => 2.0 * config.label.margin + label.width), 0);
            const graphics = new PIXI.Graphics();
            this.labels.forEach(label => {
                label.x = Math.round(offset - config.label.margin - label.width);
                graphics
                    .beginFill(config.label.background.fill, config.label.background.alpha)
                    .drawRect(
                        label.x - config.label.background.offset,
                        label.y - config.label.background.offset,
                        label.width + 2 * config.label.background.offset,
                        label.height + 2 * config.label.background.offset
                    )
                    .endFill();
            });
            const sections = Math.floor(newHeight / Math.max(1, config.continuous.sectionSize));
            const sectionSize = newHeight / sections;
            for (let section = 0; section < sections; section += 1) {
                const ratio = (section + 0.5) / sections;
                const value = this.colorScheme.maximum -
                    (this.colorScheme.maximum - this.colorScheme.minimum) * ratio;
                const y1 = section * sectionSize;
                const color = this.colorScheme.getColorForValue(value);
                graphics
                    .beginFill(color, 1)
                    .drawRect(
                        offset,
                        y1,
                        config.continuous.width,
                        sectionSize
                    )
                    .endFill();
            }
            graphics
                .lineStyle(1, config.continuous.stroke, 1)
                .drawRect(
                    offset,
                    0,
                    config.continuous.width,
                    newHeight
                )
                .lineStyle(0, 0x0, 0);
            this.continuousContainer.addChild(graphics);
            this.continuousContainer.addChild(labelsContainer);
        }
        this.continuousContainer.y = height / 2.0 - newHeight / 2.0;
        this.session.continuousHeight = newHeight;
        return true;
    }

    renderDiscrete(height = 0) {
        const gradients = this.colorScheme.gradientCollection;
        const heightChanged = this.session.discreteHeight !== height;
        if (this.session.discreteHeight !== height) {
            const drawingHeight = height - 2.0 * config.discrete.margin;
            const gradientStopTotalHeight = config.discrete.gradientStop.height +
                2.0 * config.discrete.gradientStop.margin;
            const gradientStopsTotalHeight = gradientStopTotalHeight * gradients.length;
            const gradientStopsPerColumn = Math.floor(drawingHeight / gradientStopTotalHeight);
            const columns = Math.ceil(gradientStopsTotalHeight / drawingHeight);
            const colorIndicatorTotalWidth = config.discrete.gradientStop.colorIndicator.width +
                2.0 * config.discrete.gradientStop.colorIndicator.margin;
            const columnWidths = (new Array(columns))
                .fill(colorIndicatorTotalWidth);
            for (let g = 0; g < gradients.length; g += 1) {
                const gradient = gradients.get(g);
                if (gradient && !this.discreteLabels.has(gradient.key)) {
                    continue;
                }
                const label = this.discreteLabels.get(gradient.key);
                const column = Math.floor(g / gradientStopsPerColumn);
                const labelWidth = label.width + 2.0 * config.discrete.gradientStop.margin;
                columnWidths[column] = Math.max(labelWidth + colorIndicatorTotalWidth, columnWidths[column]);
            }
            this._discreteTotalWidth = columnWidths.reduce((w, c) => w + c, 0);
            this.discreteGraphics.clear();
            for (let g = 0; g < gradients.length; g += 1) {
                const gradient = gradients.get(g);
                if (gradient && !this.discreteLabels.has(gradient.key)) {
                    continue;
                }
                const label = this.discreteLabels.get(gradient.key);
                const row = g % gradientStopsPerColumn;
                const column = Math.floor(g / gradientStopsPerColumn);
                const stopsPerColumn = Math.min(
                    gradientStopsPerColumn,
                    gradients.length - column * gradientStopsPerColumn
                );
                const columnWidth = columnWidths[column];
                const columnXOffset = columnWidths.slice(0, column).reduce((w, c) => w + c, 0);
                const columnYOffset = drawingHeight / 2.0 - stopsPerColumn / 2.0 * gradientStopTotalHeight;
                const yCenter = columnYOffset + gradientStopTotalHeight * (row + 0.5);
                label.y = yCenter - label.height / 2.0;
                label.x = columnXOffset +
                    columnWidth -
                    colorIndicatorTotalWidth -
                    label.width -
                    config.discrete.gradientStop.margin;
                label.visible = true;
                this.discreteGraphics
                    .beginFill(gradient.getAnyColor(), 1)
                    .lineStyle(1, config.discrete.gradientStop.colorIndicator.stroke, 1)
                    .drawRect(
                        columnXOffset + columnWidth - colorIndicatorTotalWidth,
                        yCenter - config.discrete.gradientStop.colorIndicator.height / 2.0,
                        colorIndicatorTotalWidth,
                        config.discrete.gradientStop.colorIndicator.height
                    )
                    .endFill();
            }
        }
        this.session.discreteHeight = height;
        return heightChanged;
    }

    /**
     * Renders color scheme
     * @param {ColorSchemeRenderOptions} options
     * @returns {boolean}
     */
    render(options = {}) {
        const {
            height = 0
        } = options;
        if (
            this.initialized &&
            (
                this.session.height !== height ||
                this.session.type !== this.colorScheme.type
            )
        ) {
            const isContinuous = this.colorScheme.type === colorSchemes.continuous;
            this.continuousContainer.visible = isContinuous;
            this.discreteContainer.visible = !isContinuous;
            let somethingChanged = this.session.type !== this.colorScheme.type;
            if (isContinuous) {
                somethingChanged = this.renderContinuous(height) || somethingChanged;
            } else {
                somethingChanged = this.renderDiscrete(height) || somethingChanged;
            }
            this.session.height = height;
            this.session.type = this.colorScheme.type;
            return somethingChanged;
        }
        return false;
    }
}

export default ColorSchemeRenderer;
