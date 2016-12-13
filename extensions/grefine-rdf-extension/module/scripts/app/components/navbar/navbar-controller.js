app.controller('NavbarCtrl', function($scope, $rootScope, $location, Metadata, $route) {
  $scope.$on('$routeChangeSuccess', function(event, current, previous) {
    $scope.path = current.$$route.originalPath;
  });

  $scope.go = function() {
    Metadata.init($scope.endpoint).then(function() {
      $route.reload();
      $location.path('/repository');
    });
  };
});