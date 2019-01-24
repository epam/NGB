export function getUserAttributesString (user) {
    const values = [];
    const firstAttributes = ['FirstName', 'LastName'];
    for (const key in user.attributes) {
        if (user.attributes.hasOwnProperty(key) && firstAttributes.indexOf(key) >= 0) {
            values.push(user.attributes[key]);
        }
    }
    for (const key in user.attributes) {
        if (user.attributes.hasOwnProperty(key) && firstAttributes.indexOf(key) === -1) {
            values.push(user.attributes[key]);
        }
    }
    return values.join(' ');
}

export function getUserDisplayNameString (user) {
    let displayName = user.userName || user.name;
    if (user.attributes && user.attributes.Name) {
        displayName = user.attributes.Name;
    }
    if (user.attributes && (user.attributes.FirstName || user.attributes.LastName)) {
        const parts = [];
        if (user.attributes.FirstName) {
            parts.push(user.attributes.FirstName);
        }
        if (user.attributes.LastName) {
            parts.push(user.attributes.LastName);
        }
        displayName = parts.join(' ');
    }

    return displayName;
}
