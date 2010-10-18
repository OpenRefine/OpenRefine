(function() {
    /*
     *  Make suggest widgets clean up when removed.
     */
    var oldInit = $.suggest.suggest.prototype._init;
    $.suggest.suggest.prototype._init = function() {
        oldInit.call(this);
        
        var self = this;
        this.input.bind("remove", function() {
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
      
      if ("ac_param" in options) {
          var ac_param = options.ac_param;
          if ("schema" in ac_param) {
              this.options.ac_param.schema = ac_param.schema;
          }
      }
  };
  $.suggest.suggestP.prototype = originalSuggestP.prototype;
  
  $.extend(
    $.suggest.suggestP, 
    {
      defaults: $.extend(
        true,
        {},
        $.suggest.suggest.defaults, {
            service_url: Refine.refineHelperService,
            service_path: "/suggest_property",
            flyout_service_url: "http://www.freebase.com",
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