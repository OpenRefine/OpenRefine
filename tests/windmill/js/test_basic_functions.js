var wm = windmill.jsTest;
var asserts = windmill.controller.asserts;

asserts.gw = {};
asserts.gw.row_count = function (count) {
    asserts.assertText( { jquery: '(".viewPanel-summary-row-count")[0]', validator: count } );
};
asserts.gw.expected_top_value = function(expected_value) {
    var actual_value =  $.trim($("a.facet-choice-label")[0].text);
    jum.assertEquals( expected_value, actual_value ); 
}


/*  haven't gotten this working yet....
waits.gw = {};
waits.gw.ajax = function () {
    { method: "waits.forElement", params: { jquery: '("body[ajax_in_progress=\'false\']")[0]'} };
};  */
    
wm.actions.gw = {};
wm.actions.gw.click_column_header = function (column_name) {
    wm.actions.click( { jquery: '(".column-header-layout tr:contains("' + column_name + '") .column-header-menu")[0]'} );
};
var gwActions = wm.actions.gw;



var test_basic_functions = new function () {
    
    // open Food project
    this.test_open_project = function () { wm.actions.click( { link: "Food" } ); };
    this.test_wait = { method: "waits.forPageLoad", params: { timeout: "20000" }};
    this.test_got_to_main_page = function () { asserts.gw.row_count("7413"); };

//    create text facet from 1st word of Short Description
//    this.test_facet_short_desc = function () { wm.actions.gw.click_column_header('Shrt_Desc'); }; // currently broken
    this.test_filter_column = function () {
        wm.actions.click( { jquery: '(".column-header-layout tr:contains(\'Shrt_Desc\') .column-header-menu")[0]'} );  //replace w/ above when able
        wm.actions.mouseOver( { jquery: '("td:contains(\'Facet\')")[0]' } ); 
    };
    this.test_wait_for_menu = { method: "waits.forElement", params: {jquery: '(".menu-item:contains(\'Custom Text Facet\')")[0]'} };
    this.test_create_facet = function () {
        wm.actions.click( { jquery: '(".menu-item:contains(\'Custom Text Facet\')")[0]'} );
        wm.actions.type( { jquery: '(".expression-preview-code")[0]', text: "value.split(',')[0]"} );
    };
    this.test_wait_for_value = {method: "waits.forElement", params: { jquery: '("td:contains(\'value.split\')")[0]'} };
    this.test_click_button = function () { wm.actions.click( { jquery: '("button:contains(\'OK\')")[0]'} ); };
    this.test_wait_for_link = {method: "waits.forElement", params: { jquery: '(".ui-button-text:contains(\'count\')")[0]'} };
    this.test_facet_worked= function () { asserts.gw.expected_top_value("ABALONE"); };
    this.test_sort_by_count = function () { wm.actions.click( { jquery: '(".ui-button-text:contains(\'count\')")[0]'} ); };
    this.test_wait_for_count = {method: "waits.forElement", params: { jquery: '(".ui-state-active .ui-button-text:contains(\'count\')")[0]'} };
    this.test_sort_worked= function () { asserts.gw.expected_top_value("BEEF"); };
    
}();



