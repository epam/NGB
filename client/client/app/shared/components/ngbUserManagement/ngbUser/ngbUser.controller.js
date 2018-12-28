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
            if (userInfo && userInfo.attributes && (userInfo.attributes.FirstName || userInfo.attributes.LastName)) {
                const parts = [];
                if (userInfo.attributes.FirstName) {
                    parts.push(userInfo.attributes.FirstName);
                }
                if (userInfo.attributes.LastName) {
                    parts.push(userInfo.attributes.LastName);
                }
                this.displayName = parts.join(' ');
            }
            if (!this.disableTooltip && userInfo && userInfo.attributes) {
                const getAttributesValues = () => {
                    const values = [];
                    const firstAttributes = ['FirstName', 'LastName'];
                    for (const key in userInfo.attributes) {
                        if (userInfo.attributes.hasOwnProperty(key) && firstAttributes.indexOf(key) >= 0) {
                            values.push(userInfo.attributes[key]);
                        }
                    }
                    for (const key in userInfo.attributes) {
                        if (userInfo.attributes.hasOwnProperty(key) && firstAttributes.indexOf(key) === -1) {
                            values.push(userInfo.attributes[key]);
                        }
                    }
                    return values;
                };
                this.userAttributes = getAttributesValues().join(', ');
            }
        });
    }

}




