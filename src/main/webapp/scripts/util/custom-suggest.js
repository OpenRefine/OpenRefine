(function() {
  var oldResponse = $.suggest.suggest.prototype.response;
  var typeToIncludedTypes = {};
  var resortByType = function(data, type) {
    var schemaPrefixes = [ type + "/" ];
    
    var includedTypes = typeToIncludedTypes[type];
    for (var i = 0; i < includedTypes.length; i++) {
        schemaPrefixes.push(includedTypes[i] + "/");
    }
    
    var results = data.result;
    var entries1 = [];
    var entries2 = [];
    
    for (var i = 0; i < results.length; i++) {
        var result = results[i];
        
        var matched = false;
        for (var j = 0; j < schemaPrefixes.length; j++) {
            var schemaPrefix = schemaPrefixes[j];
            if (result.id.substring(0, schemaPrefix.length) == schemaPrefix) {
                matched = true;
                break;
            }
        }
        
        if (matched) {
            entries1.push(result);
        } else {
            entries2.push(result);
        }
    }
    
    data.result = entries1.concat(entries2);
  };

  /*
   *  Property suggest
   */
  $.suggest(
    "suggestP",
    $.extend(
      true,
      {},
      $.suggest.suggest.prototype, 
      {
        request: function(val, start) {
            if (this.ac_xhr) {
                this.ac_xhr.abort();
                this.ac_xhr = null;
            }
            
            if (val.charAt(0) == '/') {
                this.request_mqlread(val, start);
            } else {
                this.request_relevance(val, start);
            }
        },
        
        request_mqlread: function(val, start) {
            var self = this;
            var o = this.options;
            
            var query  = [{
                "id" : val,
                "name" : null,
                "guid" : null,
                "x:type" : "/type/property",
                "/type/property/expected_type" : null
            }];
            var data = {
                query: JSON.stringify({ query: query })
            };
            
            var baseUrl = "http://api.freebase.com/api/service/mqlread";
            var url = baseUrl + "?" + $.param(data),
            
            cached = $.suggest.cache[url];
            if (cached) {
                this.response(cached, start ? start : -1, true);
                return;
            }
            
            clearTimeout(this.request.timeout);
            this.request.timeout =
                setTimeout(function() {
                    self.ac_xhr = $.ajax({
                        url: baseUrl,
                        data: data,
                        beforeSend: function() {
                            var calls = self.input.data("request.count.suggest") || 0;
                            if (!calls) {
                                self.trackEvent(self.name, "start_session");
                            }
                            calls += 1;
                            self.trackEvent(self.name, "request", "count", calls);
                            self.input.data("request.count.suggest", calls);
                        },
                        success: function(r) {
                            r.prefix = val; // we need this so that the rest of suggest wouldn't error out
                            if (!("result" in r)) {
                                r.result = [];
                            }
                            self.response(r, start ? start : -1);
                        },
                        error: function(xhr) {
                            self.trackEvent(self.name, "request", "error", {url:this.url, response: xhr ? xhr.responseText : ''});
                        },
                        complete: function(xhr) {
                            if (xhr) {
                                self.trackEvent(self.name, "request", "tid", xhr.getResponseHeader("X-Metaweb-TID"));
                            }
                        },
                        dataType: "jsonp",
                        cache: true
                    });
                }, o.xhr_delay);
        },
        
        request_relevance: function(val, start) {
            var self = this;
            var o = this.options;
                
            var data = {
                query: val,
                mql_output: JSON.stringify([{
                    "id" : null,
                    "name" : null,
                    "/type/property/expected_type" : null
                }])
            };
            if (start) {
                data.start = start;
            }

            $.extend(data, o.ac_param);
            $.extend(data, { limit: 100 });

            var baseUrl = "http://api.freebase.com/api/service/search";
            var url = baseUrl + "?" + $.param(data),
            cached = $.suggest.cache[url];
            if (cached) {
                this.response(cached, start ? start : -1, true);
                return;
            }

            clearTimeout(this.request.timeout);
            this.request.timeout =
                setTimeout(function() {
                    self.ac_xhr = $.ajax({
                            url: baseUrl,
                            data: data,
                            beforeSend: function() {
                                var calls = self.input.data("request.count.suggest") || 0;
                                if (!calls) {
                                    self.trackEvent(self.name, "start_session");
                                }
                                calls += 1;
                                self.trackEvent(self.name, "request", "count", calls);
                                self.input.data("request.count.suggest", calls);
                            },
                            success: function(data) {
                                data.prefix = val; // we need this so that the rest of suggest wouldn't error out
                                
                                if ("schema" in o) {
                                    var type = o.schema;
                                    var apply = function() {
                                        resortByType(data, type);
                                        
                                        $.suggest.cache[url] = data;
                                        self.response(data, start ? start : -1);
                                    };
                                    if (type in typeToIncludedTypes) {
                                        apply();
                                    } else {
                                        var query = {
                                            query: {
                                                "id" : type,
                                                "/freebase/type_hints/included_types": []
                                            }
                                        };
                                        $.getJSON(
                                            "http://api.freebase.com/api/service/mqlread?" + $.param({ query: JSON.stringify(query) }) + "&callback=?",
                                            null,
                                            function(d) {
                                                var types = [];
                                                if ("result" in d) {
                                                    types = d.result["/freebase/type_hints/included_types"];
                                                }
                                                typeToIncludedTypes[type] = types;
                                                
                                                apply();
                                            },
                                            "jsonp"
                                        );
                                    }
                                } else {
                                    self.response(data, start ? start : -1);
                                }
                            },
                            error: function(xhr) {
                                self.trackEvent(self.name, "request", "error", {url:this.url, response: xhr ? xhr.responseText : ''});
                            },
                            complete: function(xhr) {
                                if (xhr) {
                                    self.trackEvent(self.name, "request", "tid", xhr.getResponseHeader("X-Metaweb-TID"));
                                }
                            },
                            dataType: "jsonp",
                            cache: true
                        });
                    }, o.xhr_delay);
        },
        create_item: function(data, response_data) {
            var css = this.options.css;

            var li =  $("<li>").addClass(css.item);

            var name = $("<div>")
                .addClass(css.item_name)
                .append(
                    $("<label>").append($.suggest.strongify(data.name || data.guid, response_data.prefix)));
            
            data.name = name.text(); // this converts html escaped strings like "&amp;" back to "&"
            li.append(name);

            name.prepend($("<div>").addClass(css.item_type).text(data.id));
            
            return li;
        }
      }
    )
  );
  
  $.extend(
    $.suggest.suggestP, 
    {
      defaults: $.extend(
        true,
        {},
        $.suggest.suggest.defaults, {
            css: { pane: "fbs-pane fbs-pane-property" }
        }
      )
    }
  );
  
  /*
   *  Type suggest
   */
  $.suggest(
    "suggestT",
    $.extend(
      true,
      {},
      $.suggest.suggest.prototype, 
      {
        create_item: function(data, response_data) {
            var css = this.options.css;

            var li =  $("<li>").addClass(css.item);

            var name = $("<div>")
                .addClass(css.item_name)
                .append(
                    $("<label>")
                        .append($.suggest.strongify(data.name || data.guid, response_data.prefix)));

            data.name = name.text(); // this converts html escaped strings like "&amp;" back to "&"
            li.append(name);

            name.prepend($("<div>").addClass(css.item_type).text(data.id));
            
            return li;
        }
      }
    )
  );
  
  $.extend(
    $.suggest.suggestT, 
    {
      defaults: $.extend(
        true,
        {},
        $.suggest.suggest.defaults, {
            css: { pane: "fbs-pane fbs-pane-type" }
        }
      )
    }
  );
})();