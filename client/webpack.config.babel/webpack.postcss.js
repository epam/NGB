const DEV = global.buildOptions.dev;

// export default [
//     require('postcss-import')(),
//     require('postcss-cssnext')({
//         browsers: DEV
//             ? ['Chrome >= 45']
//             : [
//             'Android 2.3',
//             'Android >= 4',
//             'Chrome >= 20',
//             'Firefox >= 24',
//             'Explorer >= 10',
//             'iOS >= 6',
//             'Opera >= 12',
//             'Safari >= 6'
//         ]
//     })
// ]

module.exports = {
  plugins: [
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
};

// module.exports = {
//   parser: 'sugarss',
//   plugins: {
//     'postcss-import': {},
//     'postcss-cssnext': {
//       browsers: DEV
//         ? ['Chrome >= 45']
//         : [
//           'Android 2.3',
//           'Android >= 4',
//           'Chrome >= 20',
//           'Firefox >= 24',
//           'Explorer >= 10',
//           'iOS >= 6',
//           'Opera >= 12',
//           'Safari >= 6'
//         ]
//     },
//     'cssnano': {}
//   }
// };