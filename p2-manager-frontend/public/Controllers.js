"use strict";

const ng = angular.module('com.itemis.p2-manager-frontend', ['angular.filter', 'infinite-scroll']);
const backend = "http://localhost:8080"; // http://localhost:8080 http://p2-manager-backend:8888

angular.module('infinite-scroll').value('THROTTLE_MILLISECONDS', 200)

ng.controller('P2MController', function($scope, $http, $timeout, $q) {
	
	//TODO allow input without "http://" to be automatically completed
	$scope.addRepository = () => {
		$http.post(backend+"/repositories?uri="+$scope.repositoryURL).
        then(response => {
			
        	$timeout(() => {
        		$scope.searchRepositories();
        	}, 1000);
		}); 
	    
	}
	
	$scope.searchRepositories = () => {
		if ($scope.searchRepoTimeout !== undefined) {
			$scope.searchRepoTimeout.resolve();
		}
		$scope.searchRepoTimeout = $q.defer();

		$scope.repositories = [];
		$scope.allRepositoriesLoaded = false;
		$scope.loadMoreRepositories();
	}
	
	$scope.loadMoreRepositories = () => {
		if ($scope.repositoriesAreLoading || $scope.allRepositoriesLoaded) {
			return;
		}

		if ($scope.repoId === undefined) {
			return;
		}
		
		$scope.repositoriesAreLoading = true;
		const searchQuery = $scope.repoId.split(" ")
								.map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
								.reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
								
		$http.get(backend+'/repositories?limit='+$scope.scrollLoadSize
												+"&offset="+$scope.repositories.length
												+"&"+searchQuery)
		.then(response => {
			$scope.repositoriesAreLoading = false;
			
			if (response.status === 204) { // No Content 
				$scope.allRepositoriesLoaded = true;
			} else {
				$scope.repositories = $scope.repositories.concat(response.data);
			}
		});
	}
	
	$scope.searchUnits = () => {
		if ($scope.searchUnitTimeout !== undefined) {
			$scope.searchUnitTimeout.resolve();
		}
		$scope.searchUnitTimeout = $q.defer();

		$scope.units = [];
		$scope.allUnitsLoaded = false;
		$scope.loadMoreUnits();
	}
	
	$scope.loadMoreUnits = () => {
		if ($scope.unitsAreLoading || $scope.allUnitsLoaded) {
			return;
		}
		
		if ($scope.unitId === undefined) {
			return;
		}
		
		$scope.unitsAreLoading = true;
		const searchQuery = $scope.unitId.split(" ")
								.map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
								.reduce((keyword1, keyword2) => keyword1+"&"+keyword2);

		$http.get(backend+'/units?limit='+$scope.scrollLoadSize
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

	$scope.repositoriesAreLoading = false;
	$scope.allRepositoriesLoaded = false;
	$scope.unitsAreLoading = false;
	$scope.allUnitsLoaded = false;
	$scope.units = [];
	$scope.repositories = [];
	$scope.repoId="";
	$scope.unitId="";
	$scope.scrollLoadSize = 20;
	$scope.unitIdFormat = '[^"&]*';
	$scope.repositoryURL = "http://www.example.com";
});

ng.controller('RepositoryUnitController', function($scope, $http) {
	
	$scope.getUnitsForRepo = () => {
		if (!$scope.showUnits)
			$http.get(backend+'/repositories/'+$scope.repository.repoId+'/units').
				then(response => {
					$scope.unitsInRepository = response.data;
				});
		
		$scope.showUnits = !$scope.showUnits;
	}
});

ng.controller('UnitController', function($scope, $http, $timeout) {
	
	$scope.getRepositoriesForVersion = () => {
		if (!$scope.repositoriesLoaded) {
			$http.get(backend+'/units/'+$scope.unit.unitId+'/versions/'+$scope.unit.version+"/repositories").
				then(response => {
					$scope.repositoriesWithVersion = response.data;
				});
			$scope.repositoriesLoaded = true;
		}
		
		$scope.showRepositories = !$scope.showRepositories;
	}

	$scope.repositoriesLoaded = false;
});