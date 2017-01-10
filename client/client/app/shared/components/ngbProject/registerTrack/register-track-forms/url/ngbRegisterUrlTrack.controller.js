import ngbRegisterTrackBaseController from '../base/ngbRegisterTrack.base.controller';

export default class ngbRegisterUrlTrackController extends ngbRegisterTrackBaseController {
    static get UID() {
        return 'ngbRegisterUrlTrackController';
    }

    getTrackFormat() {
        return super.getTrackFormat();
    }

    get registrationWarning() {
        if (this.getTrackFormat() && this.getTrackFormat() !== 'bam') {
            return 'File will be downloaded to NGB server';
        }
        return null;
    }

    getTrackRegisterOpts() {
        const referenceId = this._ngbProjectService.getSelectedReferences()[0].id;
        return [referenceId, this.filePath, this.indexFilePath, this.trackName, 'URL'];
    }

    checkInputs() {
        let error = super.checkInputs();
        if (error) {
            return error;
        }
        const format = this.getTrackFormat();
        switch (format) {
            case 'bam': {
                if (!this.filePath.toLowerCase().startsWith('http://') && !this.filePath.toLowerCase().startsWith('https://') && !this.filePath.toLowerCase().startsWith('ftp://')) {
                    error = 'File path\'s scheme should be http, https or ftp';
                }
                else if (!this.indexFilePath.toLowerCase().startsWith('http://') && !this.indexFilePath.toLowerCase().startsWith('https://') && !this.indexFilePath.toLowerCase().startsWith('ftp://')) {
                    error = 'Index file path\'s scheme should be http, https or ftp';
                }
            }
                break;
            default: {
                if (!this.filePath.toLowerCase().startsWith('http://') && !this.filePath.toLowerCase().startsWith('https://') && !this.filePath.toLowerCase().startsWith('ftp://')) {
                    error = 'File path\'s scheme should be http, https or ftp';
                }
            }
                break;
        }
        return error;
    }
}
