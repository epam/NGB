import * as PIXI from 'pixi.js-legacy';
import CheckboxButton from '../checkbox-button';
import HeatmapEventDispatcher from '../../utilities/heatmap-event-dispatcher';

class HeatmapOptionsRenderer extends HeatmapEventDispatcher {
    /**
     *
     * @param {LabelsManager} labelsManager
     * @param {HeatmapInteractions} interactions
     * @param {HeatmapViewOptions} options
     */
    constructor(labelsManager, interactions, options) {
        super();
        this.container = new PIXI.Container();
        this.annotationsButton = new CheckboxButton('ANNOTATIONS', interactions, labelsManager);
        this.dendrogramButton = new CheckboxButton('DENDROGRAM', interactions, labelsManager);
        this.container.addChild(this.annotationsButton.container);
        this.container.addChild(this.dendrogramButton.container);
        this.buttons = [
            this.dendrogramButton,
            this.annotationsButton
        ];
        this.updateButtonPositionsCallback = this.updateButtonPositions.bind(this);
        this.annotationsButton.onInitialized(this.updateButtonPositionsCallback);
        this.dendrogramButton.onInitialized(this.updateButtonPositionsCallback);
        this.annotationsButton.onVisibilityChanged(this.updateButtonPositionsCallback);
        this.dendrogramButton.onVisibilityChanged(this.updateButtonPositionsCallback);
        this.annotationsButton.initialize();
        this.dendrogramButton.initialize();
        /**
         * Heatmap view options
         * @type {HeatmapViewOptions}
         */
        this.options = options;
        this._visible = true;
        if (this.options) {
            this.updateButtonStatesCallback = this.updateButtonStates.bind(this);
            this.options.onChange(this.updateButtonStatesCallback);
            this.dendrogramButton.onButtonClicked(() => {
                this.options.dendrogram = !this.options.dendrogram;
            });
            this.annotationsButton.onButtonClicked(() => {
                this.options.annotations = !this.options.annotations;
            });
            this.updateButtonStates();
            this.updateButtonPositions();
            this.render();
        }
    }

    get visible() {
        return this._visible;
    }

    set visible(visible) {
        if (visible !== this._visible) {
            this._visible = visible;
            this.container.visible = visible;
            this.updateButtonStates();
        }
    }

    destroy() {
        super.destroy();
        if (this.buttons) {
            this.buttons.forEach(button => button.destroy());
        }
        this.buttons = undefined;
        if (this.options) {
            this.options.removeEventListeners(
                this.updateButtonStatesCallback
            );
        }
        this.options = undefined;
    }

    updateButtonStates() {
        if (
            this.options &&
            this.annotationsButton
        ) {
            this.annotationsButton.enabled = this.options.annotations;
            this.annotationsButton.visible = this.options.annotationsAvailable && this.visible;
        }
        if (
            this.options &&
            this.dendrogramButton
        ) {
            this.dendrogramButton.enabled = this.options.dendrogram;
            this.dendrogramButton.visible = this.options.dendrogramAvailable && this.visible;
        }
    }

    updateButtonPositions() {
        let y = 0;
        for (const button of this.buttons) {
            button.container.x = 0;
            button.container.y = y;
            button.updateBounds();
            if (button.visible && button.valid) {
                y += button.height;
            }
            button.markAsChanged();
        }
    }

    /**
     * Sets buttons container position
     * @param {number} x
     * @param {number} y
     */
    setPosition(x, y) {
        const changed = x !== this.container.x || y !== this.container.y;
        this.container.x = x;
        this.container.y = y;
        if (changed) {
            this.updateButtonPositions();
        }
    }

    render () {
        return this.buttons.map(button => button.render()).filter(Boolean) > 0;
    }
}

export default HeatmapOptionsRenderer;
