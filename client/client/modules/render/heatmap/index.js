import {
    ColorFormats,
    colorSchemes as HeatmapColorSchemes,
} from './color-scheme';
import {HeatmapDataType} from './heatmap-data';
import HeatmapView from './heatmap-view';
import angular from 'angular';
import {getRenderer} from '../core';

export {
    HeatmapColorSchemes,
    ColorFormats,
    HeatmapDataType,
};

class Heatmap {
    /**
     *
     * @param {HTMLElement} container
     * @param {dispatcher} dispatcher
     * @param {string} [className]
     * @param {boolean} [checkContainerSize=false]
     */
    constructor(container, dispatcher, className, checkContainerSize = false) {
        this.useWebGL = true;
        this.container = container;
        this.onResize = this.resize.bind(this);
        this.domElement = document.createElement('div');
        this.dispatcher = dispatcher;
        this.handleContextLoss = this.onWebGLContextLoss.bind(this);
        if (className) {
            this.domElement.classList.add(className);
        }
        angular.element(window).on('resize', this.onResize);
        this.heatmapView = new HeatmapView({dispatcher});
        this.refreshRenderer(true);
        this.ensureContainerSize();
        if (checkContainerSize) {
            this.checkContainerSize();
        }
    }

    ensureContainerSize() {
        if (this.container && this.container.clientWidth > 0) {
            this.refreshRenderer();
        } else {
            this.cancelEnsureSizeFrame = requestAnimationFrame(this.ensureContainerSize.bind(this));
        }
    }

    checkContainerSize() {
        if (
            this.container &&
            this.container.clientWidth > 0 &&
            this.container.clientHeight > 0 &&
            (
                this._containerWidth !== this.container.clientWidth ||
                this._containerHeight !== this.container.clientHeight
            )
        ) {
            this.refreshRenderer();
        }
        this.cancelCheckContainerSize = requestAnimationFrame(this.checkContainerSize.bind(this));
    }

    /**
     *
     * @param {number|string} id
     * @param {number|string} [projectId]
     */
    setDataConfig(id, projectId) {
        this.heatmapView.initialize({
            dataConfig: {id, projectId},
            dispatcher: this.dispatcher
        });
    }

    refreshRenderer(force = false) {
        if (!this.container) {
            return;
        }
        const size = this.pixiRenderer || {width: 0, height: 0};
        const actualSize = this.container
            ? {width: this.container.clientWidth, height: this.container.clientHeight}
            : {width: 0, height: 0};
        if (!force && actualSize.width === size.width && actualSize.height === size.height) {
            return;
        }

        this._containerWidth = actualSize.width;
        this._containerHeight = actualSize.height;
        if (!this.pixiRenderer) {
            /**
             *
             * @private
             */
            this.pixiRenderer = getRenderer(actualSize, {backgroundColor: 0xffffff}, this.useWebGL);
            if (this.useWebGL) {
                this.pixiRenderer.view.addEventListener('webglcontextlost', this.handleContextLoss);
            }
            this.container.appendChild(this.pixiRenderer.view);
        }
        this.resizeRenderer(actualSize);
        this.heatmapView.initialize({
            displayOptions: {
                domElement: this.container,
                pixiRenderer: this.pixiRenderer
            },
            ...actualSize
        });
    }

    destroyPixiRenderer() {
        if (this.pixiRenderer && this.pixiRenderer.view) {
            if (this.useWebGL) {
                this.pixiRenderer.view.removeEventListener('webglcontextlost', this.handleContextLoss);
            }
            if (this.container) {
                this.container.removeChild(this.pixiRenderer.view);
            }
            this.pixiRenderer.destroy(true);
            this.pixiRenderer = null;
        }
    }

    destroy() {
        cancelAnimationFrame(this.cancelEnsureSizeFrame);
        cancelAnimationFrame(this.cancelCheckContainerSize);
        this.destroyPixiRenderer();
        if (this.container) {
            this.container = undefined;
        }
        if (this.heatmapView) {
            this.heatmapView.destroy();
        }
        angular.element(window).off('resize', this.onResize);
    }

    resize() {
        this.refreshRenderer();
    }

    resizeRenderer(size) {
        if (this.pixiRenderer) {
            this.pixiRenderer.resize(size.width, size.height);
            this.pixiRenderer.view.style.width = `${size.width}px`;
            this.pixiRenderer.view.style.height = `${size.height}px`;
        }
    }

    onWebGLContextLoss = (e) => {
        e.preventDefault();
        this.useWebGL = false;
        this.destroyPixiRenderer();
        this.refreshRenderer(true);
    };
}

export {HeatmapView};
export default Heatmap;
