angular.module('unitList')
.factory('unitSearch', () => {
    return {
        onSearchTextChange: onSearchTextChange,
        setSearchText: setSearchText
    };

    function onSearchTextChange(func) {
        callback = func;
    }
    
    function setSearchText(keywords) {
        keywords = keywords;
        callback(keywords);
    }

});