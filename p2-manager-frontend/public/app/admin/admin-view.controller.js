angular.module('admin')
.controller('adminController', function ($http, $mdToast, constants) {
    this.backend = constants.backend;
    
    this.addRepository = () => {
        $http.post(this.backend+"/repositories?uri="+this.repositoryURL)
            .then(response => {
                $mdToast.show(
                    $mdToast.simple()
                        .textContent('Repository was added!')
                        .position('top right')
                        .hideDelay(2500)
                );
            });
    }
})