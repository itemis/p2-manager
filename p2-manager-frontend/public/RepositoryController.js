var ng = angular.module('com.itemis.p2-manager-frontend', ['angular.filter'])

ng.controller('RepositoryController', function($scope, $http) {
	$http.get('http://localhost:8080/repositories').
        then(function(response) {
            $scope.repositories = response.data;
        });
});

ng.controller('UnitController', function($scope, $http) {
	$http.get('http://localhost:8080/repositories/'+$scope.repository.repoId+'/units').
    then(function(response) {
        $scope.units = response.data;
    });
});