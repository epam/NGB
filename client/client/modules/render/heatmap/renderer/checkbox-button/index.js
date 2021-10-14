import * as PIXI from 'pixi.js-legacy';
import InteractiveZone from '../../interactions/interactive-zone';
import config from './config';
import events from '../../utilities/events';
import makeInitializable from '../../utilities/make-initializable';

class CheckboxButton extends InteractiveZone {
    /**
     *
     * @param {string} text
     * @param {HeatmapInteractions} interactions
     * @param {LabelsManager} labelsManager
     */
    constructor(text, interactions, labelsManager) {
        super({
            priority: InteractiveZone.Priorities.button
        });
        makeInitializable(this);
        /**
         * Button text
         * @type {string}
         */
        this.text = text;
        this._enabled = false;
        this._visible = true;
        this._hovered = false;
        this._changed = true;
        /**
         * Interactions manager
         * @type {HeatmapInteractions}
         */
        this.interactions = interactions;
        if (interactions) {
            interactions.registerInteractiveZone(this);
        }
        /**
         * Labels manager
         * @type {LabelsManager}
         */
        this.labelsManager = labelsManager;
        /**
         *
         * @type {PIXI.Container}
         */
        this.container = new PIXI.Container();
        this.container.interactive = true;
        this.container.buttonMode = true;
    }

    destroy() {
        super.destroy();
        if (this.container) {
            this.container.removeChildren();
        }
        this.container = undefined;
        if (this.interactions) {
            this.interactions.unregisterInteractiveZone(this);
        }
        this.interactions = undefined;
        this.labelsManager = undefined;
    }

    initialize() {
        this.container.removeChildren();
        if (this.labelsManager) {
            this.disabledLabel = this.labelsManager.getLabel(this.text, config.disabled.font);
            this.hoveredLabel = this.labelsManager.getLabel(this.text, config.hovered.font);
            this.activeLabel = this.labelsManager.getLabel(this.text, config.active.font);
            if (this.disabledLabel && this.activeLabel && this.hoveredLabel) {
                this.labelWidth = Math.max(
                    this.disabledLabel.width,
                    this.hoveredLabel.width,
                    this.activeLabel.width,
                    config.minWidth
                ) + 2.0 * config.padding;
                this.labelHeight = Math.max(
                    this.disabledLabel.height,
                    this.hoveredLabel.height,
                    this.activeLabel.height,
                    0
                ) + 2.0 * config.padding;
                /**
                 * Button width
                 * @type {number}
                 */
                this.width = this.labelWidth + 2.0 * config.margin;
                /**
                 * Button height
                 * @type {number}
                 */
                this.height = this.labelHeight + 2.0 * config.margin;
                const center = 0.5;
                this.disabledLabel.anchor.set(center, center);
                this.hoveredLabel.anchor.set(center, center);
                this.activeLabel.anchor.set(center, center);
                const x = Math.floor(this.width / 2.0);
                const y = Math.floor(this.height / 2.0);
                this.disabledLabel.x = x;
                this.disabledLabel.y = y;
                this.hoveredLabel.x = x;
                this.hoveredLabel.y = y;
                this.activeLabel.x = x;
                this.activeLabel.y = y;
                this.graphics = new PIXI.Graphics();
                this.container.addChild(this.graphics);
                this.container.addChild(this.disabledLabel);
                this.container.addChild(this.activeLabel);
                this.container.addChild(this.hoveredLabel);
                this.initialized = true;
                this.draw();
                this.updateBounds();
                this._changed = true;
            }
        }
    }

    updateBounds() {
        this.globalPosition = this.container.getGlobalPosition();
    }

    markAsChanged() {
        this._changed = true;
        this.emit(events.render.request);
    }

    get visible() {
        return this._visible;
    }

    set visible(visible) {
        if (this._visible !== visible) {
            this._visible = visible;
            this._changed = true;
            this.emit(events.render.request);
            this.emit(events.visibility);
        }
    }

    get enabled() {
        return this._enabled;
    }

    set enabled(enabled) {
        if (this._enabled !== enabled) {
            this._enabled = enabled;
            this._changed = true;
            this.emit(events.render.request);
        }
    }

    get hovered() {
        return this._hovered;
    }

    set hovered(hovered) {
        if (this._hovered !== hovered) {
            this._hovered = hovered;
            this._changed = true;
            this.emit(events.render.request);
        }
    }

    get valid() {
        const numberIsValid = n => !Number.isNaN(Number(n)) &&
            Number.isFinite(Number(n)) &&
            Number(n) > 0;
        return numberIsValid(this.width) &&
            numberIsValid(this.height) &&
            numberIsValid(this.labelWidth) &&
            numberIsValid(this.labelHeight);
    }

    test(event) {
        const visible = this.visible && this.valid;
        if (this.globalPosition && visible) {
            const {globalX, globalY} = event;
            return globalX >= this.globalPosition.x && globalX <= this.globalPosition.x + this.width &&
                globalY >= this.globalPosition.y && globalY <= this.globalPosition.y + this.height;
        }
        return super.test(event);
    }

    onClick(event) {
        super.onClick(event);
        event.stopImmediatePropagation();
        this.emit(events.click);
    }

    onDragEnd(event) {
        super.onDragEnd(event);
        event.stopImmediatePropagation();
        this.emit(events.click);
    }

    onButtonClicked(callback) {
        this.addEventListener(events.click, callback);
    }

    onVisibilityChanged(callback) {
        this.addEventListener(events.visibility, callback);
    }

    onHover(event) {
        super.onHover(event);
        this.hovered = !!event && this.test(event);
    }

    draw() {
        const visible = this.visible && this.valid;
        if (this.hoveredLabel) {
            this.hoveredLabel.visible = this.hovered && visible;
        }
        if (this.disabledLabel) {
            this.disabledLabel.visible = !this.hovered && !this.enabled && visible;
        }
        if (this.activeLabel) {
            this.activeLabel.visible = !this.hovered && this.enabled && visible;
        }
        if (this.graphics) {
            this.graphics.clear();
            if (visible) {
                let buttonConfig = config.disabled;
                if (this.hovered) {
                    buttonConfig = config.hovered;
                } else if (this.enabled) {
                    buttonConfig = config.active;
                }
                this.graphics
                    .beginFill(buttonConfig.background, buttonConfig.alpha)
                    .lineStyle(1, buttonConfig.stroke, 1)
                    .drawRoundedRect(
                        config.margin,
                        config.margin,
                        this.labelWidth,
                        this.labelHeight,
                        config.borderRadius
                    )
                    .endFill();
            }
        }
    }

    render() {
        if (this._changed) {
            this.draw();
            this._changed = false;
            return true;
        }
        return false;
    }
}

export default CheckboxButton;
