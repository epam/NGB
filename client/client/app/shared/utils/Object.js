export function isEquivalent(a, b) {
    const aProps = (a && Object.getOwnPropertyNames(a)) || [];
    const bProps = (b && Object.getOwnPropertyNames(b)) || [];

    // If number of properties is different,
    // objects are not equivalent
    if (aProps.length !== bProps.length) {
        return false;
    }

    for (let i = 0; i < aProps.length; i++) {
        const propName = aProps[i];

        // If values of same property are not equal,
        // objects are not equivalent
        if (a[propName] !== b[propName]) {
            return false;
        }
    }

    // If we made it this far, objects
    // are considered equivalent
    return true;
}

export function parseObjectInSubItems(root, fnForObject) {
    return parseObject(root, fnForObject, 'subItems');
}

export function parseObject(root, fnForObject, subFolder='content') {

    const findItems = (obj)=> {

        const result = [];

        if (obj instanceof Array) {
            for (let i = 0; i < obj.length; i++) {
                result.push(...findItems(obj[i]));
            }
        } else if (obj instanceof Object) {
            if (fnForObject(obj)) {
                result.push(fnForObject(obj));
            } else if (obj[subFolder]) {
                result.push(...findItems(obj[subFolder]));
            }
        }
        return result;
    };
    return findItems(root);
}