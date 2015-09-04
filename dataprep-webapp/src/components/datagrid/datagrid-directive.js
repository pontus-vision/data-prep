(function () {
    'use strict';

    /**
     * @ngdoc directive
     * @name data-prep.datagrid.directive:Datagrid
     * @description This directive create the SlickGrid datagrid<br/>
     * Watchers :
     * <ul>
     *         <li>Grid header directive list : insert grid header in SlickGrid headers</li>
     *         <li>Grid Metadata : it means the loaded dataset has changed, the styles are reset</li>
     *         <li>Grid data : update grid headers and styles</li>
     *         <li>Filters : reset the styles</li>
     * </ul>
     *
     * @requires data-prep.datagrid.service:DatagridGridService
     * @requires data-prep.datagrid.service:DatagridColumnService
     * @requires data-prep.datagrid.service:DatagridStyleService
     * @requires data-prep.datagrid.service:DatagridSizeService
     * @requires data-prep.datagrid.service:DatagridTooltipService
     * @requires data-prep.datagrid.service:DatagridExternalService
     * @requires data-prep.services.playground.service:DatagridService
     * @requires data-prep.services.filter.service:FilterService
     * @restrict E
     */
    function Datagrid(DatagridGridService, DatagridColumnService, DatagridStyleService, DatagridSizeService,
                      DatagridTooltipService, DatagridExternalService, DatagridService, FilterService) {
        return {
            restrict: 'E',
            templateUrl: 'components/datagrid/datagrid.html',
            bindToController: true,
            controllerAs: 'datagridCtrl',
            controller: 'DatagridCtrl',
            link: function (scope, iElement) {
                var grid;
                var columnTimeout, externalTimeout, focusTimeout;

                //------------------------------------------------------------------------------------------------------
                //--------------------------------------------------GETTERS---------------------------------------------
                //------------------------------------------------------------------------------------------------------
                /**
                 * @ngdoc method
                 * @name getMetadata
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Get the loaded metadata
                 */
                var getMetadata = function getMetadata() {
                    return DatagridService.metadata;
                };

                /**
                 * @ngdoc method
                 * @name getData
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Get the loaded data
                 */
                var getData = function getData() {
                    return DatagridService.data;
                };

                /**
                 * @ngdoc method
                 * @name getFilters
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Get the filter list
                 */
                var getFilters = function getFilters() {
                    return FilterService.filters;
                };

                //------------------------------------------------------------------------------------------------------
                //---------------------------------------------------UTILS----------------------------------------------
                //------------------------------------------------------------------------------------------------------
                /**
                 * @ngdoc method
                 * @name onMetadataChange
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Reset cell styles, scroll to top and expect to recreate all columns on next update
                 */
                var onMetadataChange = function onMetadataChange() {
                    if (grid) {
                        DatagridStyleService.resetCellStyles();
                        DatagridStyleService.resetColumnStyles();
                        grid.scrollRowToTop(0);
                        DatagridColumnService.renewAllColumns(true);
                    }
                };

                /**
                 * @ngdoc method
                 * @name onDataChange
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Update and resize the columns with its headers, set grid styles
                 */
                var onDataChange = function onDataChange(data) {
                    if (data) {
                        initGridIfNeeded();
                        var columns;

                        //create columns, manage style and size, set columns in grid, and insert headers
                        clearTimeout(columnTimeout);
                        columnTimeout = setTimeout(function() {

                            columns = DatagridColumnService.createColumns(data.columns, data.preview);
                            DatagridStyleService.manageColumnStyle(columns, data.preview);
                            DatagridSizeService.autosizeColumns(columns); // IMPORTANT : this set columns in the grid

                            DatagridColumnService.renewAllColumns(false);
                        }, 0);

                        //manage column selection (external)
                        clearTimeout(externalTimeout);
                        if(!data.preview) {
                            externalTimeout = setTimeout(function() {
                                var selectedColumn = DatagridStyleService.selectedColumn(columns);
                                if (selectedColumn) {
                                    DatagridExternalService.updateSuggestionPanel(selectedColumn);
                                }
                            }, 0);
                        }

                        //focus specific column
                        clearTimeout(focusTimeout);
                        focusTimeout = setTimeout(DatagridGridService.navigateToFocusedColumn, 300);
                    }
                };

                /**
                 * @ngdoc method
                 * @name onFiltersChange
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Refresh cell styles and scroll to top
                 */
                var onFiltersChange = function onFiltersChange() {
                    if (grid) {
                        DatagridStyleService.resetCellStyles();
                        grid.scrollRowToTop(0);
                    }
                };


                /**
                 * @ngdoc method
                 * @name initDragDropColumnsHeader
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Manage the reordering for index column 'tdpId'
                 */
                var initDragDropColumnsHeader = function initDragDropColumnsHeader() {

                    var isDown = false;   // Tracks status of mouse button

                    $('#datagrid').mousedown(function() {
                        isDown = true;      // When mouse goes down, set isDown to true
                    }).mouseup(function() {
                        isDown = false;    // When mouse goes up, set isDown to false
                    });

                    $('#datagrid').mouseleave(function() {
                        if(isDown) {
                            // Triggers mouseup on the body.
                            var e = $.Event( 'mouseup', { which: 1 } );
                            $('body').trigger(e);
                        }
                    });

                    $('#datagrid').mousemove(function(event) {
                        if(isDown) {
                            var parentOffset = $(this).parent().offset();
                            var relX = event.pageX - parentOffset.left;
                            if((relX <= 85)) { //tdpId header size
                                // Triggers mouseup on the body.
                                var e = $.Event( 'mouseup', { which: 1 } );
                                $('body').trigger(e);
                            }
                        }
                    });
                };


                //------------------------------------------------------------------------------------------------------
                //---------------------------------------------------INIT-----------------------------------------------
                //------------------------------------------------------------------------------------------------------
                /**
                 * @ngdoc method
                 * @name initGridIfNeeded
                 * @methodOf data-prep.datagrid.directive:Datagrid
                 * @description [PRIVATE] Init Slick grid and init datagrid private services.
                 */
                var initGridIfNeeded = function () {
                    if (!grid) {
                        grid = DatagridGridService.initGrid('#datagrid');

                        // the tooltip ruler is used compute a cell text regardless of the font and zoom used.
                        // To do so, the text is put into an invisible span so that the span can be measured.
                        DatagridTooltipService.tooltipRuler = iElement.find('#tooltip-ruler').eq(0);

                        initDragDropColumnsHeader();
                    }
                };

                //------------------------------------------------------------------------------------------------------
                //-------------------------------------------------WATCHERS---------------------------------------------
                //------------------------------------------------------------------------------------------------------

                /**
                 * Scroll to top when loaded dataset change
                 */
                scope.$watch(getMetadata, onMetadataChange);

                /**
                 * Update grid columns and invalidate grid on data change
                 */
                scope.$watch(getData, onDataChange);

                /**
                 * When filter change, displayed values change, so we reset active cell and cell styles
                 */
                scope.$watchCollection(getFilters, onFiltersChange);
            }
        };
    }

    angular.module('data-prep.datagrid')
        .directive('datagrid', Datagrid);
})();

