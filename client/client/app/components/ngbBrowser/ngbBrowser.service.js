import {$http} from '../../../dataServices';
export default class ngbBrowserService {
    getMarkdown(url) {
        return $http('get', url, {customResponseType: 'text'})
        .then((xhr) => (xhr.response)
                ? xhr.response
                : Promise.reject(xhr.response));
    }
}
