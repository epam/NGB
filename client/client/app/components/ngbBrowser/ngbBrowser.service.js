import { DataService } from '../../../dataServices';

export default class ngbBrowserService extends DataService {
    getMarkdown(url) {
        return this.get(url, {customResponseType: 'text'});
    }
}
