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

var CustomSuggest = {};

function sanitizeSuggestOptions(options) {
  return {
    query_param_name: options.query_param_name,
    access: options.access,
    formatter_url: options.formatter_url,
    service_url:options.service_url,
    service_path:options.service_path,
  };
}

(function() {

  /*
   *  Make suggest widgets clean up when removed.
   */
  var oldInit = $.suggest.suggest.prototype._init;
  $.suggest.suggest.prototype._init = function() {
    oldInit.call(this);

    var self = this;
    this.input.on("remove", function() {
      self._destroy();
    });
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
        create_item: function(data, response_data) {
          var css = this.options.css;

          var li =  $("<li>").addClass(css.item);

          var name = $("<div>")
          .addClass(css.item_name)
          .append(
            $("<label>").append(
                $.suggest.strongify(
                    data.name || data.guid, response_data.prefix
                )
            )
          );

          data.name = name.text(); // this converts html escaped strings like "&amp;" back to "&"
          if (data.description) {
             var descriptionSpan = $("<span></span>").text(data.description);
             name.append(descriptionSpan);
          }
          li.append(name);

          name.prepend($("<div>").addClass(css.item_type).text(data.id));

          return li;
        }
      }
    )
  );

  var originalSuggestP = $.suggest.suggestP;
  $.suggest.suggestP = function(input, options) {
    originalSuggestP.call(this, input, options);
  };
  $.suggest.suggestP.prototype = originalSuggestP.prototype;

  $.extend(
    $.suggest.suggestP, 
    {
      defaults: $.extend(
        true,
        {},
        $.suggest.suggest.defaults, {
          scoring: "schema",
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
          if (data.description) {
             var descriptionSpan = $("<span></span>").text(data.description);
             name.append(descriptionSpan);
          }

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
          scoring: "schema",
          css: { pane: "fbs-pane fbs-pane-type" }
        }
      )
    }
  );
  
})();
