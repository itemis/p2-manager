"use strict";

const ng = angular.module('com.itemis.p2-manager-frontend', ['angular.filter', 'infinite-scroll'])

ng.controller('P2MController', function($scope, $http, $timeout, $q) {
	
	$scope.getRepositories = () => {
		$http.get('http://localhost:8080/repositories').
        then(response => {
            $scope.repositories = response.data;
        });
	}
	
	//TODO allow input without "http://" to be automatically completed
	$scope.addRepository = () => {
		$http.post("http://localhost:8080/repositories?uri="+$scope.repositoryURL).
        then(response => {
			
        	$timeout(() => {
        		$scope.getRepositories();
        	}, 1000);
		}); 
	    
	}
	
	$scope.searchUnits = () => {
		if ($scope.searchTimeout !== undefined) {
			$scope.searchTimeout.resolve();
		}
		$scope.searchTimeout = $q.defer();

		$scope.units = [];
		$scope.allUnitsLoaded = false;
		$scope.loadMoreUnits();
	}
	
	$scope.loadMoreUnits = () => {
		if ($scope.unitsAreLoading || $scope.allUnitsLoaded) {
			return;
		}
		
		$scope.unitsAreLoading = true;
		const searchQuery = $scope.unitId.split(" ")
								.map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
								.reduce((keyword1, keyword2) => keyword1+"&"+keyword2);

		$http.get('http://localhost:8080/units?limit='+$scope.scrollLoadSize
											+"&offset="+$scope.units.length
											+"&"+searchQuery)
		.then(response => {
			$scope.unitsAreLoading = false;
			
			if (response.status === 204) { // No Content 
				$scope.allUnitsLoaded = true;
			} else {
				$scope.units = $scope.units.concat(response.data);
			}
		});
	}
	
	$scope.isValidUnitId = (unitId) => {
		return unitId.includes("/") || unitId.includes("\"");
	}

	$scope.unitsAreLoading = false;
	$scope.allUnitsLoaded = false;
	$scope.units = [];
	$scope.repositories = [];
	$scope.unitId="";
	$scope.scrollLoadSize = 20;
	$scope.unitIdFormat = '[^/"&]*';
	$scope.repositoryURL = "http://www.example.com";
	
	$scope.getRepositories();
});

ng.controller('RepositoryUnitController', function($scope, $http) {
	
	$scope.getUnitsForRepo = () => {
		if (!$scope.showUnits)
			$http.get('http://localhost:8080/repositories/'+$scope.repository.repoId+'/units').
				then(response => {
					$scope.unitsInRepository = response.data;
				});
		
		$scope.showUnits = !$scope.showUnits;
	}
});

ng.controller('UnitController', function($scope, $http, $timeout) {
	
	$scope.getRepositoriesForVersion = () => {
		if (!$scope.repositoriesLoaded) {
			$http.get('http://localhost:8080/units/'+$scope.unit.unitId+'/versions/'+$scope.unit.version+"/repositories").
				then(response => {
					$scope.repositoriesWithVersion = response.data;
				});
			$scope.repositoriesLoaded = true;
		}
		
		$scope.showRepositories = !$scope.showRepositories;
	}

	$scope.repositoriesLoaded = false;
});