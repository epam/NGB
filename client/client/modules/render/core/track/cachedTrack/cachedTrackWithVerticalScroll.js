import CachedTrack from './cachedTrack';
import CachedTrackRendererWithVerticalScroll from './cachedTrackRendererWIthVerticalScroll';

export default class CachedTrackWithVerticalScroll extends CachedTrack {
    get verticalScrollRenderer (): CachedTrackRendererWithVerticalScroll {
        return undefined;
    }

    hoverVerticalScroll() {
        return !!this.verticalScrollRenderer &&
            this.verticalScrollRenderer.hoverVerticalScroll(this.viewport);
    }

    unhoverVerticalScroll() {
        return !!this.verticalScrollRenderer &&
            this.verticalScrollRenderer.unhoverVerticalScroll(this.viewport);
    }

    canScroll(delta) {
        return !!this.verticalScrollRenderer &&
            this.verticalScrollRenderer.canScroll(delta);
    }

    isScrollable() {
        return !!this.verticalScrollRenderer &&
            this.verticalScrollRenderer.isScrollable();
    }

    scrollIndicatorBoundaries() {
        return this.verticalScrollRenderer
            ? this.verticalScrollRenderer.scrollIndicatorBoundaries(this.viewport)
            : undefined;
    }

    setScrollPosition(value) {
        if (this.verticalScrollRenderer) {
            this.verticalScrollRenderer.setScrollPosition(this.viewport, value);
        }
    }

    onScroll({delta}) {
        if (this.tooltip) {
            this.tooltip.hide();
        }
        if (this.verticalScrollRenderer) {
            this.verticalScrollRenderer.scroll(this.viewport, delta);
        }
        this.updateScene();
    }
}
