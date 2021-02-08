const DEV = global.buildOptions.dev;

module.exports = [
    require('postcss-import')(),
    require('postcss-cssnext')({
        browsers: DEV
            ? ['Chrome >= 45']
            : [
            'Android 2.3',
            'Android >= 4',
            'Chrome >= 20',
            'Firefox >= 24',
            'Explorer >= 10',
            'iOS >= 6',
            'Opera >= 12',
            'Safari >= 6'
        ]
    })
]