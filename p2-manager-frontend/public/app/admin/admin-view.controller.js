angular.module('admin')
.controller('adminController', function ($http) {
    this.backend = "http://localhost:8080";
    
    this.addRepository = () => {
        $http.post(this.backend+"/repositories?uri="+this.repositoryURL);
    }
})