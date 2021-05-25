import {
    SessionExpirationBehavior,
    SessionExpirationBehaviorStorageKey
} from './utils/session-expiration-behavior';
import BluebirdPromise from 'bluebird';
import ngbConstants from '../constants';

const AUTH_ERROR_CODE = 401;

/**
 * Data Service class
 */
export class DataService {
    static get serviceFactory() {
        return (dispatcher) => new this(dispatcher);
    }

    ngbConstants = ngbConstants;
    _serverUrl;
    _urlPrefix;
    _dispatcher;

    constructor(dispatcher) {
        this._serverUrl = this.ngbConstants.urlPrefix;
        this._dispatcher = dispatcher;
        if (this._serverUrl[this._serverUrl.length - 1] !== '/') {
            this._serverUrl += '/';
        }
        this._urlPrefix = this._serverUrl;
        this._serverUrl += 'restapi/';
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
            .then((xhr) => {
                if (xhr.status === AUTH_ERROR_CODE) {
                    this.handleAuthenticationError();
                    return Promise.reject(xhr.response);
                }
                return (xhr.response && xhr.response.status === 'OK')
                    ? xhr.response.payload
                    : Promise.reject(xhr.response);
            });
    }

    handleAuthenticationError() {
        const behavior = localStorage.getItem(SessionExpirationBehaviorStorageKey);
        if (behavior) {
            switch (behavior) {
                case SessionExpirationBehavior.auto:
                    this.authenticate();
                    break;
                case SessionExpirationBehavior.confirm:
                default:
                    if (this._dispatcher) {
                        this._dispatcher.emitGlobalEvent('confirm:authentication:redirect');
                    }
                    break;
            }
        } else {
            // First initialization. Should redirect
            this.authenticate();
        }
    }

    authenticate() {
        window.location = `${this._urlPrefix}saml/logout`;
    }
}


function $http(method, url, data) {
    if (arguments.length < 4)
        return $http(arguments[0], arguments[1], undefined, arguments[2]);

    const token = localStorage.getItem('token');
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
