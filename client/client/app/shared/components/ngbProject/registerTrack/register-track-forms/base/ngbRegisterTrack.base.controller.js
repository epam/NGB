export default class ngbRegisterTrackBaseController {
    static get UID() {
        return 'ngbRegisterTrackBaseController';
    }

    /** @ngInject */
    constructor(dispatcher,
                ngbProjectService,
                bamDataService,
                wigDataService,
                vcfDataService,
                geneDataService,
                bedDataService,
                segDataService,
                mafDataService,
                bucketDataService, $mdToast, $scope) {
        this._dispatcher = dispatcher;
        this._trackServices = {
            'bam': bamDataService,
            'vcf': vcfDataService,
            'gtf': geneDataService,
            'gff': geneDataService,
            'gff3': geneDataService,
            'bw': wigDataService,
            'wig': wigDataService,
            'bed': bedDataService,
            'seg': segDataService,
            'maf': mafDataService
        };
        this._ngbProjectService = ngbProjectService;
        this._mdToast = $mdToast;

        $scope.$on('$destroy', () => this._mdToast.cancel());
    }

    get registrationWarning() {
        return null;
    }

    get trackName() {
        if (!this.filePath)
            return null;
        let parts = this.filePath.split('\/');
        let name = parts[parts.length - 1];
        parts = name.split('\\');
        name = parts[parts.length - 1];
        return name;
    }

    checkInputs() {
        let error = null;
        if (this.getTrackFormat() === 'bam' && !this.indexFilePath) {
            error = 'You should specify index file for .bam track';
        }
        return error;
    }

    getTrackFormat() {
        if (!this.filePath)
            return null;
        const listForCheckingFileType = this.filePath.split('.');
        if (listForCheckingFileType[listForCheckingFileType.length - 1].toLowerCase() === 'gz') {
            listForCheckingFileType.splice(listForCheckingFileType.length - 1, 1);
        }
        return listForCheckingFileType[listForCheckingFileType.length - 1].toLowerCase();
    }

    getTrackRegisterOpts() {
        const referenceId = this._ngbProjectService.getSelectedReferences()[0].id;
        return [referenceId, this.filePath, this.indexFilePath, this.trackName];
    }

    async registerTrack(form) {
        const tracksFormats = Object.keys(this._trackServices)
                .sort((prev, next)=>prev > next)
                .map(track=> ` .${track}`),
            errorMessage =
                `This file type can't be registered. You must enter only:${tracksFormats} file types`;
        const registrableFormatOfTrack = this.getTrackFormat();
        const error = this.checkInputs();
        if (error) {
            this._showAlert(error);
        }
        else if (this._trackServices.hasOwnProperty(registrableFormatOfTrack)) {
            this.isProgressShown = true;
            try {
                await this._trackServices[registrableFormatOfTrack].register(...this.getTrackRegisterOpts());
                this.isProgressShown = false;
                this._ngbProjectService.onNewTrackRegister();
                this.onCancel(form);
            } catch (error) {
                this.isProgressShown = false;
                this._showAlert(error.message);
            }
        } else {
            this._showAlert(errorMessage);
        }
    }

    onCancel(form) {
        this.filePath = this.indexFilePath = '';
        form.$setUntouched();
        this.collapsibleControllerObject.onClick();
        this._mdToast.cancel();
    }

    _showAlert(message) {
        this._mdToast.show(
            this._mdToast.simple()
                .textContent(message)
                .position('top start')
                .hideDelay(0)
                .action('OK')
        );
    }
}