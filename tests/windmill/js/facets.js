var test_facets = new function() {

	// TODO: if I'm in a project, go back to main page (useful when working on this test)
    //if () { 
    	//go back
    //};
	
    // test opening Food project
    test = newTest();
    assert (test, "assertText", { jquery: '("h1")[0]', validator: "Welcome to Gridworks" });
    this.test_home_page = test;
    
    // make sure the dataset was loaded properly
    test = newTest();
    action (test, "click",        { link:    "Food" });
    wait   (test, "forPageLoad",  { timeout: "20000" });
    assert (test, "rowCount", "7413" );
    this.test_open_project = test;

    // create text facet from 1st word of Short Description column
    test = newTest();
    action (test, 'click',       { jquery: '(".column-header-layout tr:contains(\'Shrt_Desc\') .column-header-menu")[0]' });
    action (test, 'mouseOver',   { jquery: '("td:contains(\'Facet\')")[0]' });
    wait   (test, "forMenuItem", {   name: 'Custom Text Facet' });
    action (test, "click",       { jquery: '(".menu-item:contains(\'Custom Text Facet\')")[0]' });
    action (test, "type",        { jquery: '(".expression-preview-code")[0]', text: "value.split(',')[0]" });
    wait   (test, "forElement",  { jquery: '("td:contains(\'value.split\')")[0]' });
    action (test, "click",       { jquery: '("button:contains(\'OK\')")[0]' });
    wait   (test, "forAjaxEnd");
    assert (test, "expectedTopFacetValue", "ABALONE");
    this.test_facet = test;
        
    // sort the facet by count and test the result
    test = newTest();
    action (test, "click",      { jquery: '(".ui-button-text:contains(\'count\')")[0]' });
    wait   (test, "forElement", { jquery: '(".ui-state-active .ui-button-text:contains(\'count\')")[0]' }); // wait til count is the active sort
    assert (test, "expectedTopFacetValue", "BEEF");
    this.test_sort_text_facet = test;
        
    // filter down to BEEF
    test = newTest();
    action (test, "click",      { jquery: '("a.facet-choice-label")[0]' });
    wait   (test, "forElement", { jquery: '(".viewPanel-summary-row-count")[0]' });
    assert (test, "rowCount", "457");
    this.test_fitler_text_facet = test;
        
    // create numeric filter from Water column
    test = newTest();
    action (test, "click",       { jquery: '(".column-header-layout tr:contains(\'Water\') .column-header-menu")[0]' });
    action (test, "mouseOver",   { jquery: '("td:contains(\'Facet\')")[0]' });
    wait   (test, "forMenuItem", {   name: 'Numeric Facet' });
    action (test, "click",       { jquery: '(".menu-item:contains(\'Numeric Facet\')")[0]' });
    // TODO: What to wait on, to know we're ready to do the following assert? Talk to dfhuynh.
    //assert (test, function() {
    //    jum.assertTrue($(".facet-panel span:contains(\'Water\')").length > 0);
    //});
    this.test_create_numeric_facet = test;

    // filter out BEEF with lower water content
    test = newTest();
    wait   (test, "forAjaxEnd"); 
    wait   (test, "forElement",   { jquery: '((".slider-widget-draggable.left"))[0]' }),
    action (test, "dragDropElem", { jquery: '((".slider-widget-draggable.left"))[0]', pixels: '150, 0' });
    // TODO: What to wait on, to know we're ready to do the following assert? Talk to dfhuynh.
    //assert (test, "rowCount", "153");
    this.test_filter_numeric_facet = test;
    
};
