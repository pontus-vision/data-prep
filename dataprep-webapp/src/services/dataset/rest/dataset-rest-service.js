(function() {
    'use strict';

    /**
     * @ngdoc service
     * @name data-prep.services.dataset.service:DatasetRestService
     * @description Dataset service. This service provide the entry point to the backend dataset REST api.<br/>
     * <b style="color: red;">WARNING : do NOT use this service directly.
     * {@link data-prep.services.dataset.service:DatasetService DatasetService} must be the only entry point for datasets</b>
     */
    function DatasetRestService($rootScope, $upload, $http, RestURLs,$log) {
        var self = this;

        /**
         * @ngdoc method
         * @name getDatasets
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Get the dataset list
         * @returns {Promise} - the GET call promise
         */
        self.getDatasets = function() {
            return $http.get(RestURLs.datasetUrl);
        };

        /**
         * @ngdoc method
         * @name create
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Create the dataset
         * @param {dataset} dataset - the dataset infos to create
         * @returns {Promise} - the $upload promise
         */
        self.create = function(dataset) {
            return $upload.http({
                url: RestURLs.datasetUrl + '?name=' + encodeURIComponent(dataset.name),
                headers: {'Content-Type': 'text/plain'},
                data: dataset.file
            });
        };

        /**
         * @ngdoc method
         * @name update
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Update the dataset
         * @param {dataset} dataset - the dataset infos to update
         * @returns {Promise} - the $upload promise
         */
        self.update = function(dataset) {
            return $upload.http({
                url: RestURLs.datasetUrl + '/' + dataset.id + '?name=' + encodeURIComponent(dataset.name),
                method: 'PUT',
                headers: {'Content-Type': 'text/plain'},
                data: dataset.file
            });
        };

        /**
         * @ngdoc method
         * @name updateMetadata
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Update the dataset metadata
         * @param {dataset} dataset - the dataset infos to update
         * @returns {Promise} The POST promise
         */
        self.updateMetadata = function(dataset){
            return $http.post(RestURLs.datasetUrl + '/' + dataset.id, dataset);
        };

        /**
         * @ngdoc method
         * @name delete
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Delete the dataset
         * @param {dataset} dataset - the dataset infos to delete
         * @returns {Promise} The DELETE promise
         */
        self.delete = function(dataset) {
            return $http.delete(RestURLs.datasetUrl + '/' + dataset.id);
        };

        /**
         * @ngdoc method
         * @name getContent
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Get the dataset content
         * @param {string} datasetId The dataset id
         * @param {boolean} metadata If false, the metadata will not be returned
         * @returns {Promise} - the GET promise
         */
        self.getContent = function(datasetId, metadata) {
            $rootScope.$emit('talend.loading.start');
            return $http.get(RestURLs.datasetUrl + '/' + datasetId + '?metadata=' + metadata )
                .then(function(res) {
                    return res.data;
                })
                .finally(function() {
                    $rootScope.$emit('talend.loading.stop');
                });
        };

        /**
         * @ngdoc method
         * @name getPreview
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Get the dataset content
         * @param {string} datasetId The dataset id
         * @param {boolean} metadata If false, the metadata will not be returned
         * @param {string} sheetName to preview
         * @returns {Promise} - the GET promise 301 status if not anymore a draft
         */
        self.getPreview = function(datasetId, metadata,sheetName) {
            $rootScope.$emit('talend.loading.start');
            return $http.get(RestURLs.datasetUrl + '/preview/' + datasetId + '?metadata=' + metadata + (sheetName?'&sheetName='+encodeURIComponent(sheetName): ''))
                .then(function(res) {
                    $log.debug("status:"+res.status);
                    return res.data;
                })
                .finally(function() {
                    $rootScope.$emit('talend.loading.stop');
                });
        };

        /**
         * @ngdoc method
         * @name processCertification
         * @methodOf data-prep.services.dataset.service:DatasetRestService
         * @description Ask certification for a dataset
         * @param {string} datasetId The dataset id
         */
        self.processCertification = function(datasetId) {
            return $http.put(RestURLs.datasetUrl + '/' + datasetId + '/processcertification');
        };
    }

    angular.module('data-prep.services.dataset')
        .service('DatasetRestService', DatasetRestService);
})();