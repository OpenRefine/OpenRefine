var test_facets = new function() {

    // test opening Food project
    this.test_open_project = [
        action("click",             { link: "Food" } ),
        action("waits.forPageLoad", { timeout: "20000" } ),
        assert("gw_row_count", "7413" )
    ];

    // create text facet from 1st word of Short Description
    this.test_facet_creation = [
        action('click',            { jquery: '(".column-header-layout tr:contains(\'Shrt_Desc\') .column-header-menu")[0]' } ),
        action('mouseOver',        { jquery: '("td:contains(\'Facet\')")[0]' } ),
        action("gw_wait4menuitem", {   name: 'Custom Text Facet' } ),
        action("click",            { jquery: '(".menu-item:contains(\'Custom Text Facet\')")[0]' } ),
        action("type",             { jquery: '(".expression-preview-code")[0]', text: "value.split(',')[0]" } ),
        action("waits.forElement", { jquery: '("td:contains(\'value.split\')")[0]' } ),
        action("click",            { jquery: '("button:contains(\'OK\')")[0]' } )
    ];

    // test created facet
    this.test_created_facet = [
        action("waits.forElement", { jquery: '(".ui-button-text:contains(\'count\')")[0]' } ),
        assert("gw_expected_top_value","ABALONE")
    ];
    
    // sort the facet by count and test the result
    this.test_sorted_facet = [
        action("click",            { jquery: '(".ui-button-text:contains(\'count\')")[0]' } ),
        action("waits.forElement", { jquery: '(".ui-state-active .ui-button-text:contains(\'count\')")[0]' } ),
        assert("gw_expected_top_value","BEEF")
    ];
    
};



