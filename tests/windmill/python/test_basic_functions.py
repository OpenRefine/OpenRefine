from windmill.authoring import WindmillTestClient

def test_gridworks():
    client = WindmillTestClient(__name__)

    # food.csv tests
    client.click(link=u'Food')
    client.waits.forPageLoad(timeout=u'20000')
    
    # create text facet from 1st word of Short Description
    filter_column(client, 'Shrt_Desc')
    client.click(jquery=u'(".menu-item:contains(\'Custom Text Facet\')")[0]')
    client.type(jquery=u'(".expression-preview-code")[0]', text=u"value.split(',')[0]")
    client.click(jquery=u'("button:contains(\'OK\')")[0]')
    client.click(jquery=u'("a:contains(\'re-sort by count\')")[0]')
    assert_expected_top_value(client, 'BEEF')

    # Filter down to BEEF. Result == 457 rows
    client.click(jquery=u'("a.facet-choice-label:contains(\'BEEF\')")[0]') 
    client.asserts.assertText(jquery=u'("span.viewPanel-summary-row-count")[0]', validator=u'457')
    
    # create numeric filter from Water column
    # assert that there's NOT a facet panel named 'Water' yet   
    assert client.execJS(js=u'$(".facet-panel span:contains(\'Water\')").length')['output'] == 0
    filter_column(client, 'Water')
    client.click(jquery=u'(".menu-item:contains(\'Numeric Facet\')")[0]')
    # assert that there's a facet panel named 'Water'
    assert client.execJS(js=u'$(".facet-panel span:contains(\'Water\')").length')['output'] > 0

    # drag to filter down set. Result == 10 rows
    client.dragDropElem(jquery=u'("a.ui-slider-handle")[0]', pixels=u'150, 0')    
    client.asserts.assertText(jquery=u'("span.viewPanel-summary-row-count")[0]', validator=u'457')
    
    # Next TODO
    # lowercase the Shrt_Desc
    # TitleCase the Shrt_Desc

'''
    # history test -- in progress
    client.mouseOver(classname=u'history-panel-body-collapsed')
    client.waits.forElement(link=u"Create new column Desc, split based on column Shrt_Desc by filling 0 rows with gel:split(value , ',')", timeout=u'8000')
    client.click(jquery=u'(".history-future .history-entry")[0]')
    client.click(jquery=u'(".history-past .history-entry")[0]')
'''

def assert_row_count(client, count):
    client.asserts.assertText(jquery=u'("span.viewPanel-summary-row-count")[0]', validator=u'{0}'.format(count))


def filter_column(client, column):
    client.click(jquery=u'(".column-header-layout tr:contains({0}) .column-header-menu")[0]'.format(column))
    client.mouseOver(jquery=u'("td:contains(\'Filter\')")[0]')    
    
    
def assert_expected_top_value(client, expected_value):
    actual_value = client.execJS(js=u'$("a.facet-choice-label")[0].text')['output'].strip()
    assert actual_value == expected_value, "Expected actual_value to be '{0}'. Got '{1}'".format(expected_value, actual_value)
