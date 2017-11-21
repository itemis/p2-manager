var ng = angular.module('com.itemis.p2-manager-frontend', ['angular.filter'])

ng.controller('P2MController', function($scope, $http, $timeout) {
	
	$scope.getRepositories = function() {
		$http.get('http://localhost:8080/repositories').
        then(function(response) {
            $scope.repositories = response.data;
        });
	}
	
	$scope.addRepository = function() {
		$http.post('http://localhost:8080/repositories?uri='+$scope.repositoryURL).
        then(function(response) {
        	$timeout(function() {
        		$scope.getRepositories();
        		$scope.getUnits();
        	}, 1000);
        }); 
	}
	
	$scope.getUnits = function() {
		$http.get('http://localhost:8080/units').
        then(function(response) {
            $scope.units = response.data;
        });
	}
	
	$scope.searchUnits = function() {
		var searchQuery = $scope.unitId.split(" ")
									.map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
									.reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
		
		$http.get('http://localhost:8080/units?'+searchQuery).
        then(function(response) {
            $scope.units = response.data;
        });
	}
	
	$scope.isValidUnitId = function(unitId) {
		return unitId.includes("/") || unitId.includes("\"");
	}
	
	$scope.unitIdFormat = '[^/"&]*';
	$scope.repositoryURL = "http://www.example.com";
	
	$scope.getRepositories();
	//$scope.getUnits();
});

ng.controller('RepositoryUnitController', function($scope, $http) {
	
	$scope.getUnitsForRepo = function() {
		if (!$scope.isLoaded) 
			$http.get('http://localhost:8080/repositories/'+$scope.repository.repoId+'/units').
			then(function(response) {
				$scope.unitsInRepository = response.data;
			});
		
		$scope.showUnits = !$scope.showUnits;
	}
});

ng.controller('UnitController', function($scope, $http, $timeout) {
	
	$scope.getRepositoriesForVersion = function() {
		if (!$scope.isLoaded) 
			$http.get('http://localhost:8080/units/'+$scope.unit.unitId+'/versions/'+$scope.unit.version+"/repositories").
			then(function(response) {
				$scope.repositoriesWithVersion = response.data;
			});
		
		$scope.showRepositories = !$scope.showRepositories;
	}
});