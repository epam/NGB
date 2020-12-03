import Menu from '../../../core/menu';
import header from './header';
import resize from './resize';

const CommonMenu = new Menu([{
    fields: [resize, header],
    label: 'General'
}]);

export {CommonMenu};

export default [
    resize,
    header
];
