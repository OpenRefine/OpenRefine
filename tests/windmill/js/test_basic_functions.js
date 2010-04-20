var test_basicFunctionality =  new function()  {//try cutting 'new' at some point
  this.test_openProject = [
    { params: { "link": "Food" }, method: "click" },
    { params: {"timeout": "20000"}, method: "waits.forPageLoad"}
    //assert the project is open
  ];
  var column_name = 'Shrt_Desc';
  this.test_filterColumn = [
//    {method: "click", params: { jquery: '(".column-header-layout tr:contains( '+ column_name + ' ) .column-header-menu")[0]' } },
    {method: "click", params: { jquery: '(".column-header-layout tr:contains(\'Shrt_Desc\') .column-header-menu")[0]'} },
    {method: "mouseOver", params: { jquery: '("td:contains(\'Facet\')")[0]'} },
    {method: "click", params: { jquery: '(".menu-item:contains(\'Custom Text Facet\')")[0]'} },
    {method: "type", params: { jquery: '(".expression-preview-code")[0]', text: "value.split(',')[0]"} },
    {method: "waits.forElement", params: { jquery: '("td:contains(\'value.split\')")[0]'} },  
    {method: "click", params: { jquery: '("button:contains(\'OK\')")[0]'} },
    {method: "waits.forElement", params: { jquery: '("a:contains(\'Count\')")[0]'} },	    
    {method: "click", params: { jquery: '("a:contains(\'Count\')")[0]'} }	
//    assert_expected_top_value(client, 'BEEF')    

  ];	
};
    


//    {method: "waits.forElement", params: { jquery: '("body[ajax_in_progress=\'false\']")[0]'} },  //wants to be a method 