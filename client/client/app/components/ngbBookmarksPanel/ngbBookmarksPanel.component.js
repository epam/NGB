export default  {
    controller: function($scope) {
        $scope.searchPattern = null;
        this.clearSearch = function() {
            $scope.searchPattern = null;
        };
    },
    template: require('./ngbBookmarksPanel.html')
};