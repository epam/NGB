import  baseController from '../../../shared/baseController';

export default class ngbFeatureInfoController extends baseController {

    static get UID() {
        return 'ngbFeatureInfoController';
    }

    errorMessageList = [];
    featureInfoStorage = {};

    constructor($anchorScroll, $scope, dispatcher, ngbFeatureInfoConstant, ngbFeatureInfoService) {
        super();
        Object.assign(this, {
            $anchorScroll,
            $scope,
            dispatcher,
            ngbFeatureInfoConstant,
            ngbFeatureInfoService
        });

        this.initEvents();
    }

    events = {
        'feature:info:select:ensembl': ::this.getFeatureInfo,
        'feature:info:select:ncbi': ::this.getFeatureInfo,
        'feature:info:select:uniprot': ::this.getFeatureInfo
    };

    getFeatureInfo(opts) {
        if(opts.db !== this.$scope.$ctrl.db) return;

        if(this.featureInfoStorage[opts.db]) {
            this._updateFeatureInfo(this.featureInfoStorage[opts.db]);
            return;
        }

        this.loadFeatureInfo(opts);
    }

    async loadFeatureInfo(opts) {
        this.errorMessageList = [];
        this.isInfoLoading = true;
        this.ngbFeatureInfoService.getFeatureInfo(opts.featureId, opts.db).then((featureInfo) => {
            this.featureInfoStorage[opts.db] = featureInfo;
            this._updateFeatureInfo(featureInfo);

            this.isInfoLoading = false;
            this.$scope.$apply();
        }, (er) => {
            this._onError(this.ngbFeatureInfoConstant.ErrorMessage.NoDataAvailable);
            this._onError(er.message);
            this.isInfoLoading = false;
            this.$scope.$apply();
        });
    }

    articleInfoToString(article){
        const otherAuthor = article.multiple_authors ? ', et all' : '';
        const info = `${article.author.name}${otherAuthor}. ${article.source}, ${article.pubdate}. PMID ${article.uid}`;
        return info;
    }

    _updateFeatureInfo(featureInfo){
        this.featureInfo = featureInfo.data;
        this.summaryProperties = featureInfo.summaryProperties;
        this.tableProperties = featureInfo.tableProperties;
    }
    _onError(message) {
        this.errorMessageList.push(message);
    }
}