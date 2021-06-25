import Menu from '../../../core/menu';
import downloadFile from './downloadFile';
import header from './header';
import resize from './resize';

const CommonMenu = new Menu([{
    fields: [resize, header, downloadFile],
    label: 'General'
}]);

export {CommonMenu};

export default [
    resize,
    header,
    downloadFile
];
