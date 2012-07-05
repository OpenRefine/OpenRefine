;(function($) {
        if (!$.suggest) {
            alert("$.suggest required");
        }

        $.suggest("suggestterm",
                  $.extend(true, {}, $.suggest.suggest.prototype, {
                         create_item: function(data,response_data) {
                             var css = this.options.css;
                             
                             var li = $("<li>").addClass(css.item);
                             var name = $("<div>").addClass(css.item_name)

                                 .append($("<label>")
                                 .append($.suggest.strongify(data.name,response_data.prefix)));
                             // this converts html escaped strings like "&amp;"
                             // back to "&"
                             data.name = name.text();
                             li.append(name);
                             name.prepend($("<div>").addClass(css.item_type).text(data.id));
                             //TODO very smelly hack to disable cache
                             $.suggest.cache = {};
                             return li;
                         },
                         	
                         flyout_request:function(data){
                        	 
                             var self = this;
                             
                             var o = this.options,
                             sug_data = this.flyoutpane.data("data.suggest");
                             if (sug_data && data.id === sug_data.id) {
                                 if (!this.flyoutpane.is(":visible")) {
                                     var s = this.get_selected();
                                     this.flyout_position(s);
                                     this.flyoutpane.show();
                                     this.input.trigger("fb-flyoutpane-show", this);
                                 }
                                 return;
                             }

                             // check $.suggest.flyout.cache
                             var cached = $.suggest.flyout.cache[data.id];
                             if (cached) {
                                 this.flyout_response(cached);
                                 return;
                             }

                             clearTimeout(this.flyout_request.timeout);
                             this.flyout_request.timeout =
                                 setTimeout(function(){self.flyout_response(data);}, o.xhr_delay);
                             
                         },
                         
                         flyout_response:function(data){
                             var o = this.options,
                             p = this.pane,
                             s = this.get_selected() || [];
                             if (p.is(":visible") && s.length) {
                                 var sug_data = s.data("data.suggest");
                                 if (sug_data && data.id === sug_data.id) {
                                     this.flyoutpane.html('<div class="fbs-flyout-content">' + data.description + '</div>');
                                     this.flyout_position(s);
                                     this.flyoutpane.show()
                                         .data("data.suggest", sug_data);
                                     this.input.trigger("fb-flyoutpane-show", this);
                                 }
                             }
                         }
                       }));



     $.extend($.suggest.suggestterm, {
         defaults:  $.extend(true, {}, $.suggest.suggest.defaults, {
             service_url: "",
             service_path: "command/rdf-extension/suggest-term",
             type_strict:"classes",
             suggest_new:"Add it",
             cache:false,
//             soft:true,
             nomatch:'<em class="fbs-nomatch-text">No suggested matches. (Shift + Enter) to add it</em>'
         })
     });

})(jQuery);
