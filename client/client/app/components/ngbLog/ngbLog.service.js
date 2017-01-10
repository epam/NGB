export class ngbLogService {
    /**
     * 
     * @returns {ngbLogService}
     */
    static instance() {
        return new ngbLogService();
    }

    /**
     * 
     */
    constructor() {
        this._message = [];
    }

    /**
     *
     * @param {String} message
     */
    writeToLog(message) {
        this._message.unshift(message);
    }

    get logMessage() {
        return this._message;
    }
    
    clearLog () {
        this._message = [];
    }


}