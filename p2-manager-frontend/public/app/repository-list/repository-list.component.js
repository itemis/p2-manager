angular
.module('repositoryList')
.component('repositoryList', {
    templateUrl: 'app/repository-list/repository-list.template.html',
    controller: function RepositoryListController($http, $q, $timeout, unitSearch) {
        
        this.backend = "http://localhost:8080"; // http://localhost:8080 http://p2-manager-backend:8888

        this.repositoriesAreLoading = false;
        this.allRepositoriesLoaded = false;
        this.repositories = [];
        this.repoSearch={"keywords":""};
        this.scrollLoadSize = 100;
        this.scrollDistance = 2;
        this.repositoryURL = "http://www.example.com";
        this.unitSearch = unitSearch;

        this.addRepository = () => {
            $http.post(this.backend+"/repositories?uri="+this.repositoryURL).
            then(response => {
                
                $timeout(() => {
                    this.searchRepositories();
                }, 1000);
            }); 
            
        }
        
        this.searchRepositories = () => {
            if (this.searchRepoTimeout !== undefined) {
                this.searchRepoTimeout.resolve();
            }
            this.searchRepoTimeout = $q.defer();

            this.repositories = [];
            this.allRepositoriesLoaded = false;
            this.loadMoreRepositories();
        }
        
        this.loadMoreRepositories = () => {
            if (this.repositoriesAreLoading || this.allRepositoriesLoaded) {
                return;
            }

            if (this.repoSearch.keywords === undefined) {
                return;
            }
            
            this.repositoriesAreLoading = true;
            const searchQuery = this.repoSearch.keywords.split(" ")
                                    .map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
                                    .reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
                                    
            $http.get(this.backend+'/repositories?topLevelOnly=true&limit='+this.scrollLoadSize
                                                    +"&offset="+this.repositories.length
                                                    +"&"+searchQuery)
            .then(response => {
                this.repositoriesAreLoading = false;
                
                if (response.status === 204) { // No Content 
                    this.allRepositoriesLoaded = true;
                } else {
                    this.repositories = this.repositories.concat(response.data);
                }
            });
        }
        
        this.getChildrenOfRepo = (repository) => {
            if (!repository.childrenLoaded) {
                $http.get(this.backend+'/repositories/'+repository.repoId+'/children').
                    then(response => {
                        repository.children = response.data;
                    });
                    repository.childrenLoaded = true;
            }
            
            repository.showChildren = !repository.showChildren;
        }

        this.filterUnitsByRepo = (repo) => {
            this.unitSearch.setSearchText("repo:"+repo.uri);
        }
    }
});