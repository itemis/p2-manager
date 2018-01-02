angular.module('admin')
.controller('adminController', function ($http, $mdToast, constants) {
    this.backend = constants.backend;
    let last = {
        bottom: false,
        top: true,
        left: false,
        right: true
    };
    
    this.addRepository = () => {
        $http.post(this.backend+"/repositories?uri="+this.repositoryURL)
            .then(response => {
                $mdToast.show(
                    $mdToast.simple()
                      .textContent('Repository was added!')
                      .position(this.getToastPosition())
                      .hideDelay(2500)
                  );
            });
    }

    this.getToastPosition = function() {
        let toastPosition = angular.extend({},last);

        return Object.keys(toastPosition)
            .filter(function(pos) { return toastPosition[pos]; })
            .join(' ');
    };
})