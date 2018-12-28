export default class ngbUserService {

    static instance(userDataService) {
        return new ngbUserService(userDataService);
    }

    _userDataService;
    users = [];

    constructor(userDataService) {
        this._userDataService = userDataService;
    }

    getUserInfo(userName) {
        return this._userDataService.getCachedUsers()
            .then(users => {
                const [user] = (users || []).filter(u => u.userName === userName);
                if (user) {
                    return user;
                }
                return null;
            });
    }
}
