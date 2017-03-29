export default function config($mdThemingProvider) {

    const {extendPalette, definePalette} = $mdThemingProvider;

    definePalette('ngb-primary', extendPalette('blue', {
        '900': '#3367D6',
        '800': '#4285F4',
        '700': '#2196f3',
        '600': '#6699ff',
        '500': '#bddffc'
    }));

    definePalette('ngb-background', extendPalette('grey', {
        'A100': '#fff'
    }));

    definePalette('ngb-accent', extendPalette('ngb-primary', {
        '50': '#00BFA5',
        '100': '#f48fb1',
        '200': '#FFC107',
        '300': '#3c99c4',
        '400': '#9139c4',
        '500': '#26A69A',
        '600': '#EF5350',
        '700': '#8D6E63',
        '800': '#8bc743',
        '900': '#6699ff',

        'A100': '#fff',

        'BND': '#fff9c4',
        'DEL': '#c9d6f0',
        'DUP': '#f48fb1',
        'INS': '#f3ceb6',
        'INV': '#dce775',
        'SNV': '#d8efdd',
        'MNP': '#5799C7',
        'UNK': '#ECECEC'
    }));
    definePalette('ngb-warning', extendPalette('red', {}));

    $mdThemingProvider.theme('default')
        .primaryPalette('ngb-primary', {
            'default': '900'
        })
        .accentPalette('ngb-accent', {
            'default': '900'
        })
        .backgroundPalette('ngb-background', {
            'default': 'A100'
        })
        .warnPalette('ngb-warning', {
            'default': '900'
        });


}
