export default class ngbFilterService {

    constructor() {
    }

    setAllVcfIdList(allVcfIdList) {
        this.allVcfIdList = allVcfIdList;
    }

    getAllVcfIdList() {
        return this.allVcfIdList;
    }

}