describe('Suggestions Profile directive', function () {
    'use strict';

    var scope, createElement, element, stateMock;

    beforeEach(module('data-prep.suggestions-stats', function ($provide) {
        stateMock = {
            playground: {
                suggestions: {
                    isLoading: false
                }
            }
        };
        $provide.constant('state', stateMock);
    }));
    beforeEach(module('htmlTemplates'));

    beforeEach(inject(function ($rootScope, $compile, $timeout) {
        scope = $rootScope.$new();
        createElement = function () {
            element = angular.element('<column-profile></column-profile>');
            angular.element('body').append(element);
            $compile(element)(scope);
            scope.$digest();
            $timeout.flush();
        };
    }));

    afterEach(function () {
        scope.$destroy();
        element.remove();
    });

    it('should render chart ghost when fetching statistics', inject(function ($q, PlaygroundService, StatisticsService) {
            //given
            spyOn(PlaygroundService, 'updateStatistics').and.returnValue($q.when());
            StatisticsService.histogram = {data: []};

            //when
            createElement();

            //then
            expect(element.find('#column-profile-refresh').length).toBe(1);
            expect(element.find('#column-profile-chart').length).toBe(0);
        })
    );
});