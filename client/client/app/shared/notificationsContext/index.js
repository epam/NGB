const LOCAL_STORAGE_KEY = 'tracks-notifications';

class NotificationsContext {
    static instance() {
        return new NotificationsContext();
    }

    static NotificationType = {
        GeneHistogramNotification: 'gene-histogram'
    };

    /**
     * NotificationVersions is used to clear permanently closed notifications settings (if version changes)
     * @type {{[NotificationsContext.NotificationType]: number}}
     */
    static NotificationVersions = {
        [NotificationsContext.NotificationType.GeneHistogramNotification]: 1
    };

    constructor () {
        this.closedNotifications = {};
        this.session = {};
        this.initialize();
    }

    initialize () {
        try {
            this.closedNotifications = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '{}');
            Object.entries(this.closedNotifications)
                .forEach(([key, value]) => {
                    if (
                        NotificationsContext.NotificationVersions.hasOwnProperty(key) &&
                        +(NotificationsContext.NotificationVersions[key]) > +(value)
                    ) {
                        delete this.closedNotifications[key];
                    }
                });
        } catch (_) {
            this.closedNotifications = {};
        } finally {
            this.session = {...this.closedNotifications};
        }
    }

    storePermanentlyClosedNotifications () {
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(this.closedNotifications));
    }

    notificationIsShown (type) {
        return this.session.hasOwnProperty(type) && this.session[type] !== undefined;
    }

    closeNotification (type, permanently = false) {
        this.session[type] = NotificationsContext.NotificationVersions[type] || 1;
        if (permanently) {
            this.closedNotifications[type] = NotificationsContext.NotificationVersions[type] || 1;
            this.storePermanentlyClosedNotifications();
        }
    }
}

export default NotificationsContext;
