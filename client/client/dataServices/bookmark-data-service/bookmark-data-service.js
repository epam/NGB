import {DataService} from '../data-service';
/**
 * data service for bookmarks
 * @extends DataService
 */
export class BookmarkDataService extends DataService {
    /**
     * Returns information about current bookmark according to bookmark id
     * @returns {promise}
     */
    loadBookmark(bookmarkId) {
        return this.get(`bookmark/${bookmarkId}`);
    }

    /**
     * Delete current bookmark.
     * @returns {promise}
     */
    deleteBookmark(bookmarkId) {
        return this.delete(`bookmark/${bookmarkId}`);
    }

    /**
     * Returns information about current bookmark according to project id
     * @returns {promise}
     */
    loadBookmarks() {
        return new Promise(resolve => {
            this.get('users')
                .then((data) => {
                    resolve(data || []);
                })
                .catch(() => {
                    resolve([]);
                });
        });
    }

    /**
     * Save bookmark
     * @returns {promise}
     */
    saveBookmark(query) {
        return this.post('bookmark/save', query);
    }
}
