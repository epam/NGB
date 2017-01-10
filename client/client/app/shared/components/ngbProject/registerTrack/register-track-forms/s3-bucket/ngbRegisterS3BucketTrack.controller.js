import ngbRegisterTrackBaseController from '../base/ngbRegisterTrack.base.controller';

export default class ngbRegisterS3BucketTrackController extends ngbRegisterTrackBaseController {
    static get UID() {
        return 'ngbRegisterS3BucketTrackController';
    }

    _s3bucketId = null;
    _s3buckets = [];

    _bucketDataService = null;

    _isRegisteringNewBucket = false;

    _bucketName = null;
    _accessKeyId = null;
    _accessKeySecret = null;

    constructor(dispatcher,
                ngbProjectService,
                bamDataService,
                wigDataService,
                vcfDataService,
                geneDataService,
                bedDataService,
                segDataService,
                mafDataService,
                bucketDataService, $mdToast, $scope, ngbRegisterTrackConstants) {
        super(dispatcher, ngbProjectService, bamDataService, wigDataService, vcfDataService, geneDataService, bedDataService, segDataService, mafDataService, bucketDataService, $mdToast, $scope, ngbRegisterTrackConstants);
        this._bucketDataService = bucketDataService;
        (async() => {
            const buckets = await this._bucketDataService.loadAllBuckets();
            if (buckets && buckets.length > 0) {
                this._s3bucketId = buckets[0].id;
            }
            else {
                this._s3bucketId = null;
            }
            this._s3buckets = buckets;
            $scope.$apply();
        })();
    }

    get registrationWarning() {
        if (this.getTrackFormat() && this.getTrackFormat() !== 'bam') {
            return 'File will be downloaded to NGB server';
        }
        return null;
    }

    checkInputs() {
        let error = super.checkInputs();
        if (error) {
            return error;
        }
        if (!this._s3bucketId) {
            error = 'You should specify s3 bucket';
        }
        return error;
    }

    getTrackRegisterOpts() {
        const referenceId = this._ngbProjectService.getSelectedReferences()[0].id;
        return [referenceId, this.filePath, this.indexFilePath, this.trackName, 'S3', this._s3bucketId];
    }

    openNewBucketRegistrationForm() {
        this._isRegisteringNewBucket = true;
    }

    async registerNewBucket(form) {
        this.isProgressShown = true;
        try {
            const newBucket = await this._bucketDataService.saveBucket({accessKeyId: this._accessKeyId, secretAccessKey: this._accessKeySecret, bucketName: this._bucketName});
            if (newBucket) {
                this._s3bucketId = newBucket.id;
                this._s3buckets = await this._bucketDataService.loadAllBuckets();
            }
            this.isProgressShown = false;
            this.closeRegisterNewBucketForm(form);
        } catch (error) {
            this.isProgressShown = false;
            this.closeRegisterNewBucketForm(form);
            this._showAlert(error.message);
        }
    }

    closeRegisterNewBucketForm() {
        this._bucketName = null;
        this._accessKeyId = null;
        this._accessKeySecret = null;
        this._isRegisteringNewBucket = false;
    }
}
