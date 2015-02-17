/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

var ExtendDataManager = {
		customServices : [],     // services registered by core and extensions
		standardServices : [],   // services registered by user
		_urlMap : {}
};

ExtendDataManager._rebuildMap = function() {
	var map = {};
	$.each(ExtendDataManager.getAllServices(), function(i, service) {
		if ("url" in service) {
			map[service.url] = service;
		}
	});
	ExtendDataManager._urlMap = map;
};

ExtendDataManager.getServiceFromUrl = function(url) {
	return ExtendDataManager._urlMap[url];
};

ExtendDataManager.getAllServices = function() {
	return ExtendDataManager.customServices.concat(ExtendDataManager.standardServices);
};

ExtendDataManager.registerService = function(service) {
	ExtendDataManager.customServices.push(service);
	ExtendDataManager._rebuildMap();

	return ExtendDataManager.customServices.length - 1;
};

ExtendDataManager.registerStandardService = function(url, name, f) {
	var data = {};

	// I don't see an API call to find the schema space or identifier space,
	// but here would be an appropriate place to find it.
	data.url = url;
	data.name = name ? name : url;
	if (url.indexOf("https://www.googleapis.com/freebase/v1") > -1) {
		data.identifierSpace = "http://rdf.freebase.com/ns/type.object.mid";
		data.schemaSpace = "http://rdf.freebase.com/ns/type.object.id";
		data.viewUrl = "http://www.freebase.com/view{{id}}";
	} else {
		data.identifierSpace = "";
		data.schemaSpace = "";
		data.viewUrl = "{{id}}";
	}

	index = ExtendDataManager.customServices.length + ExtendDataManager.standardServices.length;

	ExtendDataManager.standardServices.push(data);
	ExtendDataManager._rebuildMap();

	ExtendDataManager.save();

	if (f) {
		f(index);
	}
};

ExtendDataManager.unregisterService = function(service, f) {
	for (var i = 0; i < ExtendDataManager.customServices.length; i++) {
		if (ExtendDataManager.customServices[i] === service) {
			ExtendDataManager.customServices.splice(i, 1);
			break;
		}
	}
	for (var i = 0; i < ExtendDataManager.standardServices.length; i++) {
		if (ExtendDataManager.standardServices[i] === service) {
			ExtendDataManager.standardServices.splice(i, 1);
			break;
		}
	}
	ExtendDataManager._rebuildMap();
	ExtendDataManager.save(f);
};

ExtendDataManager.save = function(f) {
	$.ajax({
		async: false,
		type: "POST",
		url: "command/core/set-preference?" + $.param({
			name: "freebase.mqlServices"
		}),
		data: { "value" : JSON.stringify(ExtendDataManager.standardServices) },
		success: function(data) {
			if (f) { f(); }
		},
		dataType: "json"
	});
};

(function() {
	$.ajax({
		async: false,
		url: "command/core/get-preference?" + $.param({
			name: "freebase.mqlServices"
		}),
		success: function(data) {
			if (data.value && data.value != "null") {
				ExtendDataManager.standardServices = JSON.parse(data.value);
				ExtendDataManager._rebuildMap();
			}
			else {
				ExtendDataManager.registerStandardService("https://www.googleapis.com/freebase/v1/mqlread", "Freebase (v1)");
			}
		},
		dataType: "json"
	});
})();
