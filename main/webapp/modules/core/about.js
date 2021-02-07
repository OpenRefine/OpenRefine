$("#about-openrefine").text($.i18n('core-index/about')+" OpenRefine");
$("#contributor").text($.i18n('core-index/contributor'));
$("#definition").text($.i18n('core-index/definition'));
// $("#history-1").text($.i18n('core-index/history-1'));
// $("#history-2").text($.i18n('core-index/history-2'));
// $("#history-3").text($.i18n('core-index/history-3'));
// $("#thanks").text($.i18n('core-index/thanks'));
 let history_1 = $.i18n('core-index/history-1');
 let history_2 = $.i18n('core-index/history-2');
 let history_3 = $.i18n('core-index/history-3');

 let testContent =  `${history_1} + ${history_2} +${history_3}`;
$("#history").text(textContent);

