import moment from 'moment';

function dateString(date) {
    if (!date) {
        return date;
    }
    const parsed = moment.utc(date);
    if (parsed.isValid()) {
        return moment(parsed.toDate()).format('MMM D, YYYY');
    }
    return undefined;
}


export default function () {
    return function (value) {
        return dateString(value) || value || '';
    };
}
