export default class ClientPaginationService {
    dispatcher;
    eventId;

    constructor(dispatcher, FIRST_PAGE, PAGE_SIZE, eventId) {
        this.dispatcher = dispatcher;
        this._firstPage = FIRST_PAGE;
        this._totalPages = FIRST_PAGE;
        this._currentPage = FIRST_PAGE;
        this._pageSize = PAGE_SIZE;
        this.eventId = eventId;
    }

    _firstPage;

    get firstPage() {
        return this._firstPage;
    }

    set firstPage(value) {
        this._firstPage = value;
    }

    _totalPages;

    get totalPages() {
        return this._totalPages;
    }

    set totalPages(value) {
        this._totalPages = value;
    }

    _currentPage;

    get currentPage() {
        return this._currentPage;
    }

    set currentPage(value) {
        this._currentPage = value;
    }

    _pageSize;

    get pageSize() {
        return this._pageSize;
    }

    set pageSize(value) {
        this._pageSize = value;
    }

    changePage(page) {
        this.currentPage = page;
        this.dispatcher.emit(this.eventId, page);
    }
}
