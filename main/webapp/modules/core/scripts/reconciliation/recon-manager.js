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

var ReconciliationManager = {
  customServices : [],     // services registered by core and extensions
  standardServices : [],   // services registered by user
  _urlMap : {}
};

ReconciliationManager._rebuildMap = function() {
  var map = {};
  $.each(ReconciliationManager.getAllServices(), function(i, service) {
    if ("url" in service) {
      map[service.url] = service;
    }
  });
  ReconciliationManager._urlMap = map;
};

ReconciliationManager.getServiceFromUrl = function(url) {
  return ReconciliationManager._urlMap[url];
};

ReconciliationManager.getAllServices = function() {
  return ReconciliationManager.customServices.concat(ReconciliationManager.standardServices);
};

ReconciliationManager.registerService = function(service) {
  ReconciliationManager.customServices.push(service);
  ReconciliationManager._rebuildMap();

  return ReconciliationManager.customServices.length - 1;
};

ReconciliationManager.registerStandardService = function(url, f, silent) {

  if (ReconciliationManager._urlMap.hasOwnProperty(url)) {
    if (!silent) {
      alert($.i18n('core-recon/url-already-registered'));
    }
    if (f) { f(url); }
    return;
  }

  var dismissBusy = function() {};
  if (!silent) {
    dismissBusy =  DialogSystem.showBusy($.i18n('core-recon/contact-service')+"...");
  }

  var registerService = function(data, mode) {
    data.url = url;
    data.ui = {
      "handler" : "ReconStandardServicePanel",
      "access" : mode
    };

    index = ReconciliationManager.customServices.length + 
      ReconciliationManager.standardServices.length;

    ReconciliationManager.standardServices.push(data);
    ReconciliationManager._rebuildMap();

    ReconciliationManager.save();

    dismissBusy();

    if (f) {
      f(index);
    }
  };

  // First, try with CORS (default "json" dataType)
  $.ajax(
    url,
    { "dataType" : "json",
    "timeout":5000
     }
  )
  .done(function(data, textStatus, jqXHR) {
    registerService(data, "json");
  })
  .fail(function(jqXHR, textStatus, errorThrown) {
    // If it fails, try with JSONP
    $.ajax(
        url,
        { "dataType" : "jsonp",
           "timeout": 5000
        }
    )
    .done(function(data, textStatus, jqXHR) {
      registerService(data, "jsonp");
    })
    .fail(function(jqXHR, textStatus, errorThrown) {
        if (!silent) {
          dismissBusy(); 
          alert($.i18n('core-recon/error-contact')+': ' + textStatus + ' : ' + errorThrown + ' - ' + url);
        }
    });
  });
};

ReconciliationManager.unregisterService = function(service, f) {
  for (var i = 0; i < ReconciliationManager.customServices.length; i++) {
    if (ReconciliationManager.customServices[i] === service) {
      ReconciliationManager.customServices.splice(i, 1);
      break;
    }
  }
  for (var i = 0; i < ReconciliationManager.standardServices.length; i++) {
    if (ReconciliationManager.standardServices[i] === service) {
      ReconciliationManager.standardServices.splice(i, 1);
      break;
    }
  }
  ReconciliationManager._rebuildMap();
  ReconciliationManager.save(f);
};

ReconciliationManager.save = function(f) {
  Refine.wrapCSRF(function(token) {
    $.ajax({
        async: false,
        type: "POST",
        url: "command/core/set-preference?" + $.param({ 
        name: "reconciliation.standardServices" 
        }),
        data: {
          "value" : JSON.stringify(ReconciliationManager.standardServices), 
          csrf_token: token
        },
        success: function(data) {
        if (f) { f(); }
        },
        dataType: "json"
    });
  });
};

ReconciliationManager.getOrRegisterServiceFromUrl = function(url, f, silent) {
   var service = ReconciliationManager.getServiceFromUrl(url);
   if (service == null) {
      ReconciliationManager.registerStandardService(url, function(idx) {
          ReconciliationManager.save(function() {
              f(ReconciliationManager.standardServices[idx]);
          });
      }, silent);
   } else {
      f(service);
   }  
};

ReconciliationManager.ensureDefaultServicePresent = function() {
   var lang = $.i18n('core-recon/wd-recon-lang');
   var url = "https://wikidata.reconci.link/"+lang+"/api";
   ReconciliationManager.getOrRegisterServiceFromUrl(url, function(service) { }, true);
   return url;
};

(function() {

  $.ajax({
    async: false,
    url: "command/core/get-preference?" + $.param({ 
      name: "reconciliation.standardServices" 
    }),
    success: function(data) {
      if (data.value && data.value != "null" && data.value != "[]") {
        ReconciliationManager.standardServices = JSON.parse(data.value);
        ReconciliationManager._rebuildMap();
      } else {
        ReconciliationManager.ensureDefaultServicePresent();
      }
    },
    dataType: "json"
  });
})();
