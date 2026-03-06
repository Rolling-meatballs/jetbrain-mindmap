angular.module('kityminderEditor').directive('export', [
  'commandBinder',
  function() {
    return {
      restrict: 'E',
      templateUrl: 'ui/directive/export/export.html',
      scope: {
        minder: '=',
      },
      replace: true,
      link: function($scope) {
        var minder = $scope.minder;

        $scope.save = function() {
          window.vscode.postMessage({
            command: 'save',
            exportData: JSON.stringify(minder.exportJson(), null, 4),
          });
        };
        $scope.exportToImage = function() {
          Promise.resolve(minder.exportData('png'))
            .then(function(res) {
              if (!res) {
                throw new Error('empty PNG payload');
              }
              window.vscode.postMessage({
                command: 'exportToImage',
                exportData: res,
              });
            })
            .catch(function(err) {
              window.vscode.postMessage({
                command: 'clientError',
                message: 'PNG export failed: ' + ((err && err.message) || String(err)),
              });
            });
        };
      },
    };
  },
]);
