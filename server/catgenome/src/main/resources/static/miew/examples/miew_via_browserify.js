var Miew = require('../dist/Miew');

window.onload = function () {
  var viewer = new Miew({
    container: document.getElementsByClassName('miew-container')[0],
    load: '1CRN',
  });

  if (viewer.init()) {
    viewer.run();
  }
};
