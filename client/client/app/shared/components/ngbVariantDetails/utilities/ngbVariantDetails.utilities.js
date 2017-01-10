export default class utilities {

    static trimString(string, maxLength = 12) {
        if (!string || Object.prototype.toString.call(string).toLowerCase() !== '[object string]')
            return string;
        if (string.length <= maxLength + 1) // no need to replace 1 letter with '...'
            return string;
        return `${string.substring(0, Math.floor(maxLength / 2))  }...${  string.substring(string.length - Math.floor(maxLength / 2))}`;
    }
    static trimStringsInArray(array, maxLength = 20) {
        const result = [];
        for (let i = 0; i < array.length; i++) {
            result.push(utilities.trimString(array[i], maxLength));
        }
        return result;
    }

    static trimArray(array, maxItemsCount = 5, maxStringLength = 20) {
        let result = [];
        if (array.length <= maxItemsCount + 1) {
            result = [...utilities.trimStringsInArray(array, maxStringLength)];
        }
        else {
            for (let i = 0; i < maxItemsCount; i++) {
                result.push(utilities.trimString(array[i], maxStringLength));
            }
            result.push(`and ${  array.length - maxItemsCount  } more.`);
        }
        return result;
    }
}
