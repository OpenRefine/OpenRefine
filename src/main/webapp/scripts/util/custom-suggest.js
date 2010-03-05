(function() {
  var base = {
    response: $.suggest.suggest.prototype.response
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
        response: function(data) {
            if ("schema" in this.options) {
                var schema = this.options.schema + "/";
                
                var results = data.result;
                var entries1 = [];
                var entries2 = [];
                
                for (var i = 0; i < results.length; i++) {
                    var result = results[i];
                    if (result.id.substring(0, schema.length) == schema) {
                        entries1.push(result);
                    } else {
                        entries2.push(result);
                    }
                }
                
                data.result = entries1.concat(entries2);
            }
            base.response.apply(this, [ data ]);
        },
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