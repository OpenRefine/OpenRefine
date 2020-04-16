// Load the localization file
var dictionary = {};
$.ajax({
	url : "command/core/load-language?",
	type : "POST",
	async : false,
	data : {
	  module : "phonetic",
//		lang : lang
	},
	success : function(data) {
		dictionary = data['dictionary'];
		lang = data['lang'];
	}
});
$.i18n().load(dictionary, lang);



