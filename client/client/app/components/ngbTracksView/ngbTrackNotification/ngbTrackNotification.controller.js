import NotificationsContext from '../../../shared/notificationsContext';
import {displayModes} from '../../../../modules/render/tracks/featureCounts/modes';

export default class ngbTrackNotificationController {
    static get UID() {
        return 'ngbTrackNotificationController';
    }

    constructor(notificationsContext, blastContext) {
        this.notificationsContext = notificationsContext;
        this.blastContext = blastContext;
        this.closePermanently = false;
    }

    get isGeneHistogramNotification () {
        return this.notification === NotificationsContext.NotificationType.GeneHistogramNotification;
    }

    get isBLASTPNotification () {
        return this.notification === NotificationsContext.NotificationType.BLASTPNotification;
    }

    get visible () {
        return !this.notificationsContext.notificationIsShown(this.notification) &&
            this.getAdditionalVisibilityCriteria();
    }

    getAdditionalVisibilityCriteria () {
        if (!this.trackInstance) {
            return true;
        }
        switch (this.notification) {
            case NotificationsContext.NotificationType.GeneHistogramNotification: {
                const isHistogram = this.trackInstance &&
                    this.trackInstance.transformer &&
                    typeof this.trackInstance.transformer.isHistogramDrawingModeForViewport === 'function' &&
                    this.trackInstance.transformer.isHistogramDrawingModeForViewport(
                        this.trackInstance.viewport,
                        this.trackInstance.cache
                    );
                if (
                    isHistogram &&
                    this.trackInstance &&
                    this.trackInstance.config &&
                    /^feature_counts$/i.test(this.trackInstance.config.format) &&
                    this.trackInstance.state &&
                    this.trackInstance.state.featureCountsDisplayMode !== displayModes.features
                ) {
                    return false;
                }
                return isHistogram;
            }
            case NotificationsContext.NotificationType.BLASTPNotification:
                return this.trackInstance &&
                    this.trackInstance.cache.isProtein === true;
        }
        return true;
    }

    closeNotification () {
        this.notificationsContext.closeNotification(this.notification, this.closePermanently);
    }
}
