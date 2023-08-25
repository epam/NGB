/* global Miew:false, $:false */
function initializeTerminal(viewer) {
  var _terminal = $('.miew-terminal');
  var _terminalWindow = _terminal.find('.terminal-window');
  _terminalWindow.terminal(function (command, term) {
    viewer.script(command, function (str) {
      term.echo(str);
    }, function (str) {
      term.error(str);
    });
  }, {
    greetings: 'Miew - 3D Molecular Viewer\nCopyright © 2015-2020 EPAM Systems, Inc.\n',
    prompt: 'miew> ',
    name: 'miew',
    scrollOnEcho: true,
    height: '100%',
    onInit: function (term) {
      var colors;
      if (viewer) {
        // highlight logs with different colors
        colors = {
          error: '#f00',
          warn: '#990',
          report: '#1a9cb0',
        };
        viewer.logger.addEventListener('message', function (e) {
          var msg = e.message.replace(/]/g, '\\]');
          term.echo('[[b;' + (colors[e.level] || '#666') + ';]' + msg + ']');
        });
      }
    },
  });
  _terminal.show();
  _terminalWindow.focus();
  viewer.enableHotKeys(false);
}

(function () {
  var viewer = new Miew({
    container: document.getElementsByClassName('miew-container')[0],
    load: '1CRN',
  });

  if (viewer.init()) {
    initializeTerminal(viewer);
    viewer.run();
  }
}());
