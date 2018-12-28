export default class ngbUserController {

    static get UID() {
        return 'ngbUserController';
    }

    disableTooltip;
    displayName;
    user;
    userAttributes;
    userInfo;

    constructor($scope, ngbUserService) {
        this.displayName = this.user;
        ngbUserService.getUserInfo(this.user).then(userInfo => {
            this.userInfo = userInfo;
            if (userInfo && userInfo.attributes && userInfo.attributes.Name) {
                this.displayName = userInfo.attributes.Name;
            }
            if (!this.disableTooltip && userInfo && userInfo.attributes) {
                const getAttributesValues = () => {
                    const values = [];
                    for (const key in userInfo.attributes.attributes) {
                        if (userInfo.attributes.attributes.hasOwnProperty(key)) {
                            values.push(userInfo.attributes.attributes[key]);
                        }
                    }
                    return values;
                };
                this.userAttributes = getAttributesValues().join(', ');
            }
        });
    }

}




