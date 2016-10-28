Refine.SetLanguageUI = function(elmt) {
	var self = this;

	elmt.html(DOM.loadHTML("core", "scripts/index/lang-settings-ui.html"));

	this._elmt = elmt;
	this._elmts = DOM.bind(elmt);

	this._elmts.or_lang_label.text($.i18n._('core-index-lang')["label"]+":");
	this._elmts.set_lan_btn.attr("value", $.i18n._('core-index-lang')["send-req"]);
	

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

	this._elmts.set_lan_btn.bind('click', function(e) {		
		$.ajax({
			url : "command/core/set-preference?",
			type : "POST",
			async : false,
			data : {
			  name : "userLang",
				value : $("#langDD option:selected").val()
			},
			success : function(data) {
				alert($.i18n._('core-index-lang')["page-reload"]);
				location.reload(true);
			}
		});
	});
};

Refine.SetLanguageUI.prototype.resize = function() {
};

Refine.actionAreas.push({
	id : "lang-settings",
	label : $.i18n._('core-index-lang')["lang-settings"],
	uiClass : Refine.SetLanguageUI
});
