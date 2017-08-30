import angular from 'angular';
import DefaultLocalData from './defaultData';
import dictionary from './dictionary';

export default class LocalDataService {

    constructor($window) {
        this._localStorage = $window.localStorage;
        const settings = this.getSettings();
        if (!settings) {
            this.updateSettings(DefaultLocalData.defaultSettings);
        }
    }

    getBookmarks() {
        let bookmarks = angular.fromJson(this._localStorage.bookmarks);
        if (!bookmarks) {
            bookmarks = [];
            this.updateBookmarks(bookmarks);
        }
        bookmarks = bookmarks.map(::this.recoverBookmark);
        return bookmarks;
    }

    getBookmark(id) {
        const [bookmark] = this.getBookmarks().filter(bookmark => bookmark.id === id);
        if (!bookmark) {
            return null;
        }
        return bookmark;
    }

    recoverBookmark(bookmark) {
        if (bookmark.projectId !== undefined) {
            for (let i = 0; i < (bookmark.tracks || []).length; i++) {
                bookmark.tracks[i].projectId = bookmark.projectId;
            }
        }
        return bookmark;
    }

    saveBookmark(bookmark) {
        bookmark.id = LocalDataService.newUUUID();
        const bookmarks = this.getBookmarks();
        bookmarks.push(bookmark);
        this.updateBookmarks(bookmarks);
        return bookmark;
    }

    deleteBookmark(id) {
        const bookmarks = this.getBookmarks();
        const [bookmark] = bookmarks.filter(bookmark => bookmark.id === id);
        if (bookmark) {
            const index = bookmarks.indexOf(bookmark);
            bookmarks.splice(index, 1);
            this.updateBookmarks(bookmarks);
        }
    }

    updateBookmarks(bookmarks) {
        bookmarks = bookmarks.map(::this.recoverBookmark);
        this._localStorage.bookmarks = angular.toJson(bookmarks || []);
    }

    updateSettings(settings) {
        this._localStorage.settings = angular.toJson(settings);
    }

    getSettings() {
        let settings = angular.fromJson(this._localStorage.settings);
        if (!settings) {
            settings = DefaultLocalData.defaultSettings;
            this.updateSettings(settings);
        }
        if (this.recoverSettings(settings)) {
            this.updateSettings(settings);
        }
        return settings;
    }

    getSettingsCopy() {
        const settingsCopy = {};
        const settings = this.getSettings();
        this.rebuildSettings(settingsCopy, settings);
        return settingsCopy;
    }

    getDefaultSettings() {
        return DefaultLocalData.defaultSettings;
    }

    getDictionary() {
        return dictionary;
    }

    recoverSettings(settingsObject, defaultSettingsObject = null) {
        let somethingRecovered = false;
        if (defaultSettingsObject === null) {
            defaultSettingsObject = DefaultLocalData.defaultSettings;
        }
        for (const key in defaultSettingsObject) {
            if (defaultSettingsObject.hasOwnProperty(key)) {
                if (settingsObject[key] === undefined) {
                    settingsObject[key] = defaultSettingsObject[key];
                    somethingRecovered = true;
                }
                else if ((typeof defaultSettingsObject[key]).toLowerCase() === 'object') {
                    somethingRecovered = this.recoverSettings(settingsObject[key], defaultSettingsObject[key]) || somethingRecovered;
                }
            }
        }
        return somethingRecovered;
    }

    rebuildSettings(settingsObject, defaultSettingsObject = null) {
        if (defaultSettingsObject === null) {
            defaultSettingsObject = DefaultLocalData.defaultSettings;
        }
        for (const key in defaultSettingsObject) {
            if (defaultSettingsObject.hasOwnProperty(key)) {
                if ((typeof defaultSettingsObject[key]).toLowerCase() === 'object') {
                    settingsObject[key] = {};
                    this.rebuildSettings(settingsObject[key], defaultSettingsObject[key]);
                } else {
                    settingsObject[key] = defaultSettingsObject[key];
                }
            }
        }
    }

    static newUUUID() {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    }
}