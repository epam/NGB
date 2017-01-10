import ngbRegisterTrackBaseController from '../base/ngbRegisterTrack.base.controller';

export default class ngbRegisterLocalComputerTrackController extends ngbRegisterTrackBaseController {
    static get UID() {
        return 'ngbRegisterLocalComputerTrackController';
    }

    checkInputs() {
        return super.checkInputs();
    }
}
