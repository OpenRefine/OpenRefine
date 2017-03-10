(function(){var form = angular.module('metadata.form', []);

form.service('CommService', function() {
  return {
    ping: function() {
      return 'Hello World';
    }
  };
});

(function(w) {
  w.metadata = w.metadata || {
    callbacks: [],
    addListener: function(callback) {
      this.callbacks.push(callback);
    },
    notify: function(model) {
      angular.forEach(this.callbacks, function(callback) {
        callback(model);
      });
    }
  };
})(window);
form.directive('metadataForm', function() {
  return {
    restrict: 'EA',
    templateUrl: 'form/form.tpl.html',
    controller: 'FormController'
  };
});
angular.module('metadata.form').run(['$templateCache', function($templateCache) {$templateCache.put('field/field-multiple.tpl.html','<span>multiple</span>\n<metadata-field-base ng-repeat="value in model[field.url] track by $index"></metadata-field-base>');
$templateCache.put('field/field-nested.tpl.html','<span>nested</span>');
$templateCache.put('field/field.tpl.html','<label ng-attr-for="{{id}}">\n    <span>{{field.name}}</span>\n</label>\n\n<input class="form-control"\n    ng-attr-name="{{id}}"\n    ng-attr-type="{{field.type}}"\n    ng-attr-id="{{id}}"\n    ng-focus=""\n    ng-required="{{field.required}}"\n    metadata-field-decorator/>');
$templateCache.put('form/form.tpl.html','<form>\n    <metadata-field ng-repeat="(id,field) in components">\n    </metadata-field>\n    <button type="submit">Test</button>\n</form>');}]);
form.controller('FormController', ["$scope", "$window", function($scope, $window) {
  $scope.model = {};

  $scope.$watch('model', function(n, o) {
    $window.metadata.notify(n);
  }, true);

  $scope.components = {
    title: {
      name: 'Title',
      url: 'http://example.com/title',
      type: 'text',
      description: 'The title of this resource',
      required: true
    },
    homepage: {
      name: 'Homepage',
      url: 'http://example.com/homepage',
      type: 'url',
      description: 'The landing page of this resource',
      required: false,
      multiple: true
    },
    language: {
      name: 'Language',
      url: 'http://example.com/language',
      type: 'url',
      description: 'The primary language of this resources',
      required: true,
      autocomplete: {
        source: 'language.json'
      }
    },
    keyword: {
      name: 'Keyword',
      url: 'http://example.com/keyword',
      type: 'text',
      description: 'Keywords describing the resource',
      required: false,
      multiple: true
    }
  };

  $scope.model[$scope.components.homepage.url] = ['']
  $scope.model[$scope.components.keyword.url] = [''];
}]);
form.directive('metadataField', function() {
  return {
    restrict: 'EA',
    link: function(scope, element, attrs) {
      if (scope.field.multiple === true) {
        scope.tpl = 'field/field-multiple.tpl.html';
      } else if (scope.field.nested) {
        scope.tpl = 'field/field-nested.tpl.html';
      } else {
        scope.tpl = 'field/field.tpl.html';
      }

      scope.add = function($index) {
        console.log('add multiple for field', scope.field.name, 'at index', $index);
        if (!scope.model[scope.field.url]) {
          scope.model[scope.field.url] = [];
        }
        scope.model[scope.field.url].splice($index + 1, 0, '');//push('');
      };
      scope.remove = function($index) {
        console.log('remove multiple for field', scope.field.name, 'from index', $index);
        scope.model[scope.field.url].splice($index, 1);
      };
    },
    template: '<ng-include src="tpl"></ng-include><br/>'
  };
});
form.directive('metadataFieldDecorator', ["$compile", function($compile) {
  return {
    restrict: 'A',
    link: function(scope, element, attrs) {
      if (scope.field.autocomplete) {
        // append attributes
        element.attr('uib-typeahead', 'testing');

        // append resource loading indicator element
        var indicator = angular.element('<span ng-show="fubar">loading</span>');
        element.after(indicator);
        $compile(indicator)(scope);
      }

      if (scope.field.multiple === true) {
        // append attributes
        element.attr('ng-model', 'model[field.url][$index]');
        element.removeAttr('metadata-field-decorator');
        $compile(element)(scope);
        
        // append plus and minus buttons to the element
        var plus = angular.element('<a href="#" ng-click="add($index)">[+]</a>');
        element.after(plus);
        $compile(plus)(scope);

        var minus = angular.element('<a href="#" ng-click="remove($index)">[-]</a>');
        element.after(minus);
        $compile(minus)(scope);
      }

      if (!scope.field.multiple) {
        element.attr('ng-model', 'model[field.url]');
        element.removeAttr('metadata-field-decorator');
        $compile(element)(scope);
      }
    }
  };
}]);
form.directive('metadataFieldBase', function() {
  return {
    restrict: 'E',
    templateUrl: 'field/field.tpl.html'
  };
});})();