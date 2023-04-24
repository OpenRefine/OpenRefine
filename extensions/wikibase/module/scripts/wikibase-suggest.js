
/**
 * Adaptation of the suggest widget to query the Wikibase autocomplete API directly
 */

(function() {

  /**
   * Options:
   * mediawiki_endpoint: url of the MediaWiki API
   * entity_type: type of entity to suggest (one of form, item, lexeme, property, sense, mediainfo…)
   * language: language code of the language to search in
   */

  $.suggest(
    "suggestWikibase",
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
          if (data.description) {
             var descriptionSpan = $("<span></span>").text(data.description);
             name.append(descriptionSpan);
          }

          li.append(name);

          name.prepend($("<div>").addClass(css.item_type).text(data.id));

          return li;
        },

        flyout_request: function() {
           // disable flyout requests, not used for Wikibase
        },

        request: function(val, cursor) {
          var self = this,
              o = this.options;

          var query = val;

          var entity_type = o.entity_type || 'item';

          var data = {
            action: 'wbsearchentities',
            language: o.language,
            search: query,
            type: entity_type,
            format: 'json',
            origin: '*',
            uselang: o.language
          };

          if (cursor) {
            data['continue'] = cursor;
          }

          var url = o.mediawiki_endpoint + "?" + $.param(data, true);
          var cached = $.suggest.cache[url];
          if (cached) {
            this.response(cached, cursor ? cursor : -1, true);
            return;
          }

          clearTimeout(this.request.timeout);

          var ajax_options = {
            url: o.mediawiki_endpoint,
            data: data,
            traditional: true,
            beforeSend: function(xhr) {
              var calls = self.input.data("request.count.suggest") || 0;
              if (!calls) {
                self.trackEvent(self.name, "start_session");
              }
              calls += 1;
              self.trackEvent(self.name, "request", "count", calls);
              self.input.data("request.count.suggest", calls);
            },
            success: function(data) {
              // translate the results of the MediaWiki API to that of the reconciliation API
              var translated = {
                  prefix: val, // keep track of prefix to match up response with input value
                  result: (data.search || []).map(result => { return {
                      id: result.id,
                      name: result.label,
                      description: result.description};})
              };
              $.suggest.cache[url] = translated;
              self.response(translated, cursor ? cursor : -1);
            },
            error: function(xhr) {
              self.status_error();
              self.trackEvent(self.name, "request", "error", {
                url: this.url,
                response: xhr ? xhr.responseText : ''
              });
              self.input.trigger("fb-error", Array.prototype.slice.call(arguments));
            },
            complete: function(xhr) {
              if (xhr) {
                self.trackEvent(self.name, "request", "tid",
                xhr.getResponseHeader("X-Metaweb-TID"));
              }
            },
            dataType: "json",
            cache: true
          };

          this.request.timeout = setTimeout(function() {
            $.ajax(ajax_options);
          }, o.xhr_delay);
        }

      }));

  $.extend(
    $.suggest.suggestWikibase, 
    {
      defaults: $.extend(
        true,
        {},
        $.suggest.suggest.defaults, {
          scoring: "schema",
          css: { pane: "fbs-pane fbs-pane-type" }
        }
      )
    }
  );

})();


