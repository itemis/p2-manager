angular.module('admin')
.controller('adminController', function ($http, constants) {
    this.backend = constants.backend;
    
    this.addRepository = () => {
        $http.post(this.backend+"/repositories?uri="+this.repositoryURL);
    }
})