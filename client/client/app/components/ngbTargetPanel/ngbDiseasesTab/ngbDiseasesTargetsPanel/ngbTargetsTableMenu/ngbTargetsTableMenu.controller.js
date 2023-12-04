export default class ngbTargetsTableMenuController {

    targetList = [];
    loading = false;
    failedSearch = false;
    errorMessageList = null;

    static get UID() {
        return 'ngbTargetsTableMenuController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbDiseasesTabService,
        ngbTargetsTabService,
        targetDataService,
        targetContext
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbDiseasesTabService,
            ngbTargetsTabService,
            targetDataService,
            targetContext
        });
        this.searchTarget();
    }

    async searchTarget() {
        this.loading = true;
        const emptyList = [];
        try {
            const list = await this.targetDataService.searchTarget(this.entity.target);
            this.targetList = list.length ? list : emptyList;
            this.failedSearch = false;
            this.errorMessageList = null;
            this.loading = false;
            this.$timeout(() => this.$scope.$apply());
        } catch (err) {
            this.targetList = emptyList;
            this.failedSearch = true;
            this.errorMessageList = err.message;
            this.loading = false;
        }
    }

    async editTarget(event, target) {
        event.preventDefault();
        event.stopPropagation();
        if (!target.targetId) return;
        await this.ngbTargetsTabService.getTarget(target.targetId);
        this.dispatcher.emit('target:diseases:show:targets:tab');
    }

    async launchTarget() {
        const {geneId, target} = this.entity;
        const params = {
            genesOfInterest: [geneId],
        };
        const info = {
            target: {
                name: target
            },
            interest: [{
                geneId: geneId,
                geneName: target,
            }],
        };
        const result = await this.ngbTargetsTabService.getIdentificationData(params, info);
        if (result) {
            this.dispatcher.emit('target:show:identification:tab');
            this.targetContext.setCurrentIdentification({
                name: target
            }, {
                genesOfInterest: [{
                    geneId: geneId,
                    geneName: target,
                }],
            });
        }
    }
}
