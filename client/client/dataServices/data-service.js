import {
    SessionExpirationBehavior,
    SessionExpirationBehaviorStorageKey
} from './utils/session-expiration-behavior';
import BluebirdPromise from 'bluebird';
import ngbConstants from '../constants';

const AUTH_ERROR_CODE = 401;
const ERROR_CODE_RANGE_START = 400;

class DataServicesConfiguration {
    _token;
    _authenticationMode;
    get authenticationMode () {
        return this._authenticationMode;
    }
    set authenticationMode (auth) {
        this._authenticationMode = auth || 'default';
        switch (this._authenticationMode.toLowerCase()) {
            case 'js_api_token':
                break;
            default:
                this.resolve();
                break;
        }
    }
    get token () {
        return this._token;
    }
    set token (value) {
        this._token = value;
        if (value) {
            this.resolve();
        }
    }
    constructor() {
        this._tokenReceivedPromise = new Promise((resolve) => {
            this._resolve = resolve;
        });
    }
    waitUntilTokenReceived () {
        return this._tokenReceivedPromise;
    }
    resolve () {
        if (typeof this._resolve === 'function') {
            this._resolve();
            this._resolve = undefined;
        }
    }
}

export const dataServicesConfiguration = new DataServicesConfiguration();

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

    downloadFile(method, url, data, ...rest) {
        return $http(method, this._serverUrl + url, data, ...rest)
            .then((xhr) => {
                if (xhr.status === AUTH_ERROR_CODE) {
                    this.handleAuthenticationError();
                    return Promise.reject(xhr.response);
                }
                if (xhr.status >= ERROR_CODE_RANGE_START) {
                    return Promise.reject(xhr.response);
                }
                return xhr.response;
            });
    }

    getRawFile(method, url, data, ...rest) {
        return $http(method, this._serverUrl + url, data, ...rest)
            .then((xhr) => {
                if (xhr.status === AUTH_ERROR_CODE) {
                    this.handleAuthenticationError();
                    return Promise.reject(xhr.response);
                }
                if (xhr.status >= ERROR_CODE_RANGE_START) {
                    return Promise.reject(xhr.response);
                }
                return xhr.responseText;
            });
    }

    getFullUrl(url) {
        return this._serverUrl + url;
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


export function $http(method, url, data, config) {
    if (arguments.length < 4)
        return $http(arguments[0], arguments[1], undefined, arguments[2]);

    return new BluebirdPromise(async (resolve, reject, onCancel) => {
        await dataServicesConfiguration.waitUntilTokenReceived();
        const xhr = new XMLHttpRequest();
        if (onCancel instanceof Function)
            onCancel(() => xhr.abort());

        xhr.addEventListener('load', () => resolve(xhr));
        xhr.addEventListener('error', reject);
        xhr.addEventListener('abort', reject);
        xhr.responseType = (config && config.customResponseType) || 'json';
        xhr.open(method, url);
        if (dataServicesConfiguration.token) {
            xhr.setRequestHeader('Authorization', `Bearer ${dataServicesConfiguration.token}`);
        }
        switch (true) {
            case data === undefined:
                return xhr.send();
            case data instanceof ArrayBuffer:
            case data instanceof Blob:
            case data instanceof FormData:
            case typeof data === 'string':
                return xhr.send(data);
            default:
                xhr.setRequestHeader('Content-Type', 'application/json');
                return xhr.send(JSON.stringify(data));
        }
    });
}
