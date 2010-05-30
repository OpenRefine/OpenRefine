
var test = [];

function newTest() {
    return [];
}

windmill.macros = {};

// ---------------------- actions extensions ----------------------------

function action(test, what, params) {
    if (typeof what == "function") {
        test.push(what);
    } else if (what in windmill.macros.actions) {
        windmill.macros.actions[what](params);
    } else {
        test.push({ 
            method: what, 
            params: params
        });
    }
}

windmill.macros.actions = {};

windmill.macros.actions.clickColumnHeader = function (test,params) {
    action(test,"click", { jquery: '(".column-header-layout tr:contains("' + params.column_name + '") .column-header-menu")[0]'} );
};

// ---------------------- wait extensions -------------------------------

function wait(test, what, params) {
    if (typeof what == "function") {
        test.push(what);
    } else if (what in windmill.macros.waits) {
        windmill.macros.waits[what](test,params);
    } else {
        test.push({ 
            method: "waits." + what, 
            params: params
        });
    }
}

windmill.macros.waits = {};

windmill.macros.waits.forAjaxEnd = function (test) {
    wait(test,"forElement", { jquery: '("body[ajax_in_progress=\'false\']")[0]' } );
};

windmill.macros.waits.forMenuItem = function (test,params) {
    wait(test,"forElement", { jquery: '(".menu-item:contains(\'' + params.name + '\')")[0]' } );
};

// ---------------------- asserts extensions ----------------------------

function assert(test, what, params) {
    if (typeof what == "function") {
        test.push(what);
    } else if (what in windmill.macros.asserts) {
        windmill.macros.asserts[what](test,params);
    } else {
        test.push({ 
            method: "asserts." + what, 
            params: params
        });
    }
}

windmill.macros.asserts = {};

windmill.macros.asserts.rowCount = function (test, count) {
    wait(test,"forElement", { jquery:  '(".viewPanel-summary-row-count")[0]' } );
    assert(test,"assertText", { jquery: '(".viewPanel-summary-row-count")[0]', validator: count } );
};

windmill.macros.asserts.expectedTopFacetValue = function (test, expected_value) {
    wait(test, "forElement", { jquery: '("a.facet-choice-label")[0]' } );
    assert(test, function() {
        jum.assertEquals(expected_value, $.trim($("a.facet-choice-label")[0].text));
    });
};

// ----------------------- register tests ---------------------------

windmill.jsTest.register([
  "test_facets"
]);

windmill.jsTest.require("facets.js");
