angular
.module('repositoryList')
.component('repositoryList', {
    templateUrl: 'app/repository-list/repository-list.template.html',
    controller: function RepositoryListController($http, $q, $timeout, unitSearch, constants) {
        
        this.backend = constants.backend;
        this.repoSearch={"keywords":""};
        this.unitSearch = unitSearch;

        this.repositories = {
            repositoryList: [],
            scrollLoadSize: 100,
            allRepositoriesLoaded: false,
            repositoriesAreLoading: false,
            
            getItemAtIndex: function(index) {
                if (index >= this.repositoryList.length) {
                    this.loadMoreRepositories(index);
                    return null;
                }
  
                return this.repositoryList[index];
            },

            getLength: function() {
                if (this.allRepositoriesLoaded) {
                    return this.repositoryList.length
                }
                return this.repositoryList.length + 5;
            },
  
            loadMoreRepositories: function(index) {
                if (index < this.repositoryList.length || this.repositoriesAreLoading || this.allRepositoriesLoaded) {
                    return;
                }
                
                this.repositoriesAreLoading = true;
                const searchQuery = this.ctrl.repoSearch.keywords.split(" ")
                                        .map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
                                        .reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
        
                $http.get(this.ctrl.backend+'/repositories?topLevelOnly=true&limit='+this.scrollLoadSize
                                                    +"&offset="
                                                    +this.repositoryList.length
                                                    +"&"+searchQuery)
                .then(response => {
                    this.repositoriesAreLoading = false;
                    
                    if (response.status === 204) { // No Content 
                        this.allRepositoriesLoaded = true;
                    } else {
                        for (let repo of response.data) {
                            this.repositoryList.push(repo);
                        }
                    }
                });
            },

            reset: function() {
                this.repositoryList = [];
                this.allRepositoriesLoaded = false;
            }
        };
        this.repositories.ctrl = this;
        
        this.searchRepositories = () => {
            if (this.searchRepoTimeout !== undefined) {
                this.searchRepoTimeout.resolve();
            }
            this.searchRepoTimeout = $q.defer();

            this.repositories.reset();
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