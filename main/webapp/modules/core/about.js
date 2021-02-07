$("#about-openrefine").text($.i18n('core-index/about')+" OpenRefine");
$("#contributor").text($.i18n('core-index/contributor'));
$("#definition").text($.i18n('core-index/definition'));

const history_1 = document.createTextNode($.i18n('core-index/history-1'));
let link_1 = document.createElement('a');
link_1.setAttribute('href' ,"http://www.metaweb.com/")
link_1.textContent ="http://www.metaweb.com/";
const history_2 = document.createTextNode($.i18n('core-index/history-2'));
let link_2 = document.createElement('a');
link_2.setAttribute('href' ,"http://www.google.com/")
link_2.textContent ="http://www.google.com/";
const history_3 = document.createTextNode($.i18n('core-index/history-3'));
 
$("#history-openrefine").append(history_1 ," " ,link_1, " " ,"Metaweb Technologies, Inc."," ",history_2," ",link_2," ","Google"," " ,history_3);

$("#thanks").text($.i18n('core-index/thanks'));


