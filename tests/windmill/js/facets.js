var test_facets = new function() {

	// TODO: if I'm in a project, go back to main page (useful when working on this test)
    //if () { 
    	//go back
    //};
	
	// test opening Food project
    this.test_open_project = [
        action("click",             { link: "Food" } ),
        action("waits.forPageLoad", { timeout: "20000" } ),
        assert("gw_row_count", "7413" )
    ];

    // create text facet from 1st word of Short Description column
    this.test_create_text_facet = [
        action('click',            { jquery: '(".column-header-layout tr:contains(\'Shrt_Desc\') .column-header-menu")[0]' } ),
        action('mouseOver',        { jquery: '("td:contains(\'Facet\')")[0]' } ),
        action("gw_wait4menuitem", {   name: 'Custom Text Facet' } ),
        action("click",            { jquery: '(".menu-item:contains(\'Custom Text Facet\')")[0]' } ),
        action("type",             { jquery: '(".expression-preview-code")[0]', text: "value.split(',')[0]" } ),
        action("waits.forElement", { jquery: '("td:contains(\'value.split\')")[0]' } ),
        action("click",            { jquery: '("button:contains(\'OK\')")[0]' } ),
        action("gw_wait4ajaxend"), //doesn't help yet, since ajax_in_progress isn't yet reliable
        action("waits.forElement", { jquery: '("a.facet-choice-label")[0]' } ),
        assert("gw_expected_top_value","ABALONE")
    ];
        
    // sort the facet by count and test the result
    this.test_sort_text_facet = [
        action("click",            { jquery: '(".ui-button-text:contains(\'count\')")[0]' } ),
        action("waits.forElement", { jquery: '(".ui-state-active .ui-button-text:contains(\'count\')")[0]' } ), //wait til count is the active sort
        assert("gw_expected_top_value","BEEF")
    ];
        
    // filter down to BEEF
    this.test_fitler_text_facet = [
        action("click",            { jquery: '("a.facet-choice-label")[0]' } ),
        action("waits.forElement", { jquery:  '(".viewPanel-summary-row-count")[0]' } ),
        assert("gw_row_count", "457")
    ];
        
    // create numeric filter from Water column
    this.test_create_numeric_facet = [
        action("click",            { jquery: '(".column-header-layout tr:contains(\'Water\') .column-header-menu")[0]' } ),
        action('mouseOver',        { jquery: '("td:contains(\'Facet\')")[0]' } ),
        action("gw_wait4menuitem", {   name: 'Numeric Facet' } ),
        action("click",            { jquery: '(".menu-item:contains(\'Numeric Facet\')")[0]' } )//,  
        //TODO: (How) can I run a JSUnit assert w/in an object?
        //function () { jum.assertTrue( $(".facet-panel span:contains(\'Water\')").length ); }
    ]; 

    // filter out BEEF with lower water content
    this.test_filter_numeric_facet = [
        action("gw_wait4ajaxend"), 
        action("waits.forElement", { jquery: '((".slider-widget-draggable.left"))[0]' } ),
        action("dragDropElem",     { jquery: '((".slider-widget-draggable.left"))[0]', pixels: '150, 0' } )//,
        // TODO: What to wait on, to know we're ready to do the following assert? Talk to dfhuynh.
        //assert("gw_row_count", "153")
    ];

        
};



