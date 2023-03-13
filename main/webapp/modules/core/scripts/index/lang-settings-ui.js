Refine.SetLanguageUI = function(elmt) {
	var self = this;

	elmt.html(DOM.loadHTML("core", "scripts/index/lang-settings-ui.html"));

	this._elmt = elmt;
	this._elmts = DOM.bind(elmt);

	this._elmts.or_lang_label.text($.i18n('core-index-lang/label')+":");
	this._elmts.set_lan_btn.val($.i18n('core-index-lang/send-req'));
	

  $.ajax({
    url : "command/core/get-languages?",
    type : "GET",
    async : false,
    data : {
      name : "module",
      value : "core"
    },
    success : function(data) {
      for( var i = 0; i < data.languages.length; i++) {
        var l = data.languages[i];
        $('<option>').val(l.code).text(l.label).appendTo('#langDD');
      }
    }
    
  });

	this._elmts.set_lan_btn.on('click', function(e) {		
                Refine.wrapCSRF(function(token) {
                    $.ajax({
                            url : "command/core/set-preference?",
                            type : "POST",
                            async : false,
                            data : {
                               name : "userLang",
                               value : JSON.stringify($("#langDD option:selected").val()),
                               csrf_token: token 
                            },
                            success : function(data) {
                                    alert($.i18n('core-index-lang/page-reload'));
                                    location.reload(true);
                            }
                    });
                });
	});
};

Refine.actionAreas.push({
	id : "language-settings",
	label : $.i18n('core-index-lang/lang-settings'),
	uiClass : Refine.SetLanguageUI
});
