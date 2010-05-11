
// ---------------------- actions extensions ----------------------------

var actions = windmill.jsTest.actions;

function action(what,params) {
params = params || {};    
    return {
        method : what,
        params : params
    };
}


actions.gw_wait4ajaxend = function () {
    return action("waits.forElement", { jquery: '("body[ajax_in_progress=\'false\']")[0]' } );
};

actions.gw_wait4menuitem = function (params) {
    return action("waits.forElement", { jquery: '(".menu-item:contains(\"' + params.name + '\")")[0]' } );
};

actions.gw_click_column_header = function (params) {
    return action("click", { jquery: '(".column-header-layout tr:contains("' + params.column_name + '") .column-header-menu")[0]'} );
};

// ---------------------- asserts extensions ----------------------------

var asserts = windmill.controller.asserts;

function assert(what,params) {
    return function() {
        windmill.controller.asserts[what](params);
    };
}
    
//TODO: is there a way to make the assert wait first, rather than putting this before each call of the assert?,
asserts.gw_row_count = function (count) {
    //action("waits.forElement", { jquery:  '(".viewPanel-summary-row-count")[0]' } ); // doesn't work bc waits can't go inside functions
    asserts.assertText( { jquery: '(".viewPanel-summary-row-count")[0]', validator: count } );
};

asserts.gw_expected_top_value = function (expected_value) {
    // action("waits.forElement", { jquery: '("a.facet-choice-label")[0]' } );   // doesn't work bc waits can't go inside functions
    asserts.assertEquals(expected_value, $.trim($("a.facet-choice-label")[0].text)); 
};

// ----------------------- register tests ---------------------------

windmill.jsTest.register([
  "test_facets"
]);

windmill.jsTest.require("facets.js");
