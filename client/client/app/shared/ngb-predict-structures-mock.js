import ngbConstants from "../../constants";

const urls = [{
    name: '1CRN',
    url: undefined,
}, {
    name: '4CRN',
    url: undefined,
}, {
    name: '3CRN',
    url: undefined,
}, {
    name: '2CRN',
    url: undefined,
}];

export default function ngbPredictStructuresMock() {
    let base = ngbConstants.urlPrefix || '';
    if (base && base.length) {
        if (!base.endsWith('/')) {
            base = base.concat('/');
        }
    }
    return urls.map((url) => ({
        name: url.name,
        url: `${base}miew/index.html?load=${url.url || url.name}`,
    }));
}