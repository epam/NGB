import angular from 'angular';

export default angular.module('ngbAutofocus', [])
  .directive('ngbAutofocus', ['$timeout', function($timeout) {
      return {
          link : function($scope, $element) {
              $scope.$watch('emitFocus', function(focus) {
                  if (focus) {
                      $timeout(function() {
                          $element[0].focus();
                      });
                  }
              });
          },
          restrict: 'A',
          scope: { 'emitFocus': '=' },
      };
  }])
  .name;
