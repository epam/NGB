import * as PIXI from 'pixi.js-legacy';
import InteractiveZone from '../../interactions/interactive-zone';
import {colorSchemes} from '../../color-scheme';
import config from './config';
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
        /**
         * Labels manager
         * @type {LabelsManager}
         */
        this.labelsManager = labelsManager;
        this.labels = [];
        this.initialized = false;
        if (this.colorScheme) {
            this.colorScheme.onChanged(() => {
                this.clearSession();
                this.requestRender();
            });
        }
        this.initialize();
    }

    destroy() {
        if (this.container) {
            this.container.destroy(true);
        }
        this.labelsManager = undefined;
        this.colorScheme = undefined;
        super.destroy();
    }

    get width() {
        if (!this.initialized) {
            return 0;
        }
        const configuration = this.colorScheme.type === colorSchemes.discrete
            ? config.discrete
            : config.continuous;
        return config.margin * 2.0
            + configuration.width
            + Math.max(
                ...this.labels.map(label => label.width + 2.0 * config.label.margin),
                0
            );
    }

    initialize() {
        this.container.removeChildren();
        this.labels = [];
        this.continuousGraphics = new PIXI.Container();
        this.discreteGraphics = new PIXI.Container();
        this.container.addChild(this.continuousGraphics);
        this.container.addChild(this.discreteGraphics);
        this.clearSession();
        this.initialized = true;
    }

    clearSession() {
        this.session = {};
    }

    renderContinuous(height = 0) {
        const newHeight = Math.min(config.continuous.maxHeight, height);
        if (this.session.continuousHeight !== newHeight) {
            this.continuousGraphics.removeChildren();
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
            this.continuousGraphics.addChild(graphics);
            this.continuousGraphics.addChild(labelsContainer);
        }
        this.continuousGraphics.y = height / 2.0 - newHeight / 2.0;
        this.session.continuousHeight = newHeight;
        return true;
    }

    // eslint-disable-next-line no-unused-vars
    renderDiscrete(height = 0) {
        return false;
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
            this.continuousGraphics.visible = isContinuous;
            this.discreteGraphics.visible = !isContinuous;
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
