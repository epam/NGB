import BluebirdPromise from 'bluebird';
import ngbConstants from '../constants';
/**
 * Data Service class
 */
export class DataService {
    static get serviceFactory() {
        return () => new this();
    }

    ngbConstants = ngbConstants;
    _serverUrl;

    constructor() {
        this._serverUrl = this.ngbConstants.urlPrefix;
        if (this._serverUrl[this._serverUrl.length - 1] !== '/') {
            this._serverUrl += '/';
        }
        this._serverUrl += 'restapi/'
    }

    /**
     *
     * @param url
     * @param config
     * @returns {promise}
     */
    get(url, config) {
        return this.callMethod('get', url, config);
    }

    /**
     *
     * @param url
     * @param data
     * @param config
     * @returns {promise}
     */
    put(url, data, config = {}) {
        return this.callMethod('put', url, data, config);
    }

    /**
     *
     * @param url
     * @param data
     * @param config
     * @returns {promise}
     */
    post(url, data, config = {}) {
        return this.callMethod('post', url, data, config);
    }

    /**
     *
     * @param url
     * @param data
     * @param config
     * @returns {promise}
     */
    delete(url, data, config = {}) {
        return this.callMethod('delete', url, data, config);
    }

    callMethod(method, url, ...rest) {
        return $http(method, this._serverUrl + url, ...rest)
            .then((xhr) =>
                (xhr.response && xhr.response.status === 'OK')
                    ? xhr.response.payload
                    : Promise.reject(xhr.response));
    }
}


function $http(method, url, data) {
    if (arguments.length < 4)
        return $http(arguments[0], arguments[1], undefined, arguments[2]);

    let token = localStorage.getItem('token');
    return new BluebirdPromise((resolve, reject, onCancel) => {
        const xhr = new XMLHttpRequest();
        if (onCancel instanceof Function)
            onCancel(() => xhr.abort());

        xhr.addEventListener('load', () => resolve(xhr));
        xhr.addEventListener('error', reject);
        xhr.addEventListener('abort', reject);
        xhr.responseType = 'json';
        xhr.open(method, url);
        switch (true) {
            case data === undefined:
                return xhr.send();
            case data instanceof ArrayBuffer:
            case data instanceof Blob:
            case typeof data === 'string':
                return xhr.send(data);
            default:
                xhr.setRequestHeader('Content-Type', 'application/json');
                if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
                return xhr.send(JSON.stringify(data));
        }
    });
}