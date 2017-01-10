export function getFormattedDate(date = new Date()) {
    const year = date.getFullYear();
    const day = _getFormattedDatePart(date.getDate());
    const month = _getFormattedDatePart(date.getMonth() + 1);

    const hours = _getFormattedDatePart(date.getHours());
    const minutes = _getFormattedDatePart(date.getMinutes());
    const seconds = _getFormattedDatePart(date.getSeconds());

    return `${year}${month}${day}_${hours}${minutes}${seconds}`;

    function _getFormattedDatePart(part) {
        return (part < 9) ? (`0${  part}`) : part;
    }
}

