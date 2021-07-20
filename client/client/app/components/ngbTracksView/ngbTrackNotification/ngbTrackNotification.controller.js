import NotificationsContext from '../../../shared/notificationsContext';

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

    get visible () {
        return !this.notificationsContext.notificationIsShown(this.notification) &&
            this.getAdditionalVisibilityCriteria();
    }

    getAdditionalVisibilityCriteria () {
        if (!this.trackInstance) {
            return true;
        }
        switch (this.notification) {
            case NotificationsContext.NotificationType.GeneHistogramNotification:
                return this.trackInstance &&
                    this.trackInstance.transformer &&
                    typeof this.trackInstance.transformer.isHistogramDrawingModeForViewport === 'function' &&
                    this.trackInstance.transformer.isHistogramDrawingModeForViewport(
                        this.trackInstance.viewport,
                        this.trackInstance.cache
                    );
        }
        return true;
    }

    closeNotification () {
        this.notificationsContext.closeNotification(this.notification, this.closePermanently);
    }
}
