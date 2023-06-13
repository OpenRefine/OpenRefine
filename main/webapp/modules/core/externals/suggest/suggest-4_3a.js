// This version is slightly later than v4.3 and incorporates a couple
// of bug fixes from Google Code SVN as well as a bunch of local changes.
/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Dae Park (daepark@google.com)
 */
(function($, undefined){

  if (!("console" in window)) {
    var c = window.console = {};
    c.log = c.warn = c.error = c.debug = function(){};
  }

  /**
   * jQuery UI provides a way to be notified when an element is removed from the DOM.
   
   * The following logic tries to determine if "remove" event is already present, else
   * tries to mimic what jQuery UI does (as of 1.8.5) by adding a hook to $.cleanData or $.fn.remove.
   */
  $(function() {
    var div = $("<div>");
    $(document.body).append(div);
    var t = setTimeout(function() {
      // copied from jquery-ui
      // for remove event
      if ( $.cleanData ) {
        var _cleanData = $.cleanData;
        $.cleanData = function( elems ) {
          for ( var i = 0, elem; (elem = elems[i]) != null; i++ ) {
            $( elem ).triggerHandler( "remove" );
          }
          _cleanData( elems );
        };
      }
      else {
        var _remove = $.fn.remove;
        $.fn.remove = function( selector, keepData ) {
          return this.each(function() {
            if ( !keepData ) {
              if ( !selector || $.filter( selector, [ this ] ).length ) {
                $( "*", this ).add( [ this ] ).each(function() {
                  $( this ).triggerHandler( "remove" );
                });
              }
            }
            return _remove.call( $(this), selector, keepData );
          });
        };
      }
    }, 1);
    div.on("remove", function() {
      clearTimeout(t);
    });
    div.remove();
  });

  /**
   * These are the search parameters that are transparently passed
   * to the search service as specified by service_url + service_path
   */
  var SEARCH_PARAMS = {
      key:1, filter:1, spell:1, exact:1,
      lang:1, scoring:1, prefixed:1, stemmed:1, format:1, mql_output:1,
      output:1, type:1
  };

  $.suggest = function(name, prototype) {

    $.fn[name] = function(options) {
      if (!this.length) {
        console.warn('Suggest: invoked on empty element set');
      }
      return this
        .each(function() {
          if (this.nodeName) {
            if (this.nodeName.toUpperCase() === 'INPUT') {
              if (this.type && this.type.toUpperCase() !== 'TEXT') {
                console.warn('Suggest: unsupported INPUT type: '+this.type);
              }
            }
            else {
              console.warn('Suggest: unsupported DOM element: '+this.nodeName);
            }
          }
          var instance = $.data(this, name);
          if (instance) {
            instance._destroy();
          }
          $.data(this, name, new $.suggest[name](this, options))._init();
        });
    };

    $.suggest[name] = function(input, options) {
      var self = this,
          o = this.options = $.extend(true, {},
                                      $.suggest.defaults,
                                      $.suggest[name].defaults,
                                      options),
          pfx = o.css_prefix = o.css_prefix || "",
          css = o.css;
      this.name = name;
      $.each(css, function(k, v) {
        css[k] = pfx + css[k];
      });

      // suggest parameters
      o.ac_param = {};
      $.each(SEARCH_PARAMS, function(k) {
        var v = o[k];
        if (v === null || v === "") {
          return;
        }
        o.ac_param[k] = v;
      });

      // status texts
      this._status = {
        START: "",
        LOADING: "",
        SELECT: "",
        ERROR: ""
      };
      if (o.status && o.status instanceof Array && o.status.length >= 3) {
        this._status.START = o.status[0] || "";
        this._status.LOADING = o.status[1] || "";
        this._status.SELECT = o.status[2] || "";
        if (o.status.length === 4) {
          this._status.ERROR = o.status[3] || "";
        }
      }

      // create the container for the drop down list
      var s = this.status = $('<div style="display:none;">').addClass(css.status),
          l = this.list = $("<ul>").addClass(css.list),
          p = this.pane = $('<div style="display:none;" class="fbs-reset">').addClass(css.pane);

      p.append(s).append(l);

      if (o.parent) {
        $(o.parent).append(p);
      }
      else {
        p.css("position","absolute");
        if (o.zIndex) {
          p.css("z-index", o.zIndex);
        }
        $(document.body).append(p);
      }
      p.on("mousedown", function(e) {
        //console.log("pane mousedown");
        self.input.data("dont_hide", true);
        e.stopPropagation();
      })
      .on("mouseup", function(e) {
        //console.log("pane mouseup");
        if (self.input.data("dont_hide")) {
          self.input.trigger('focus');
        }
        self.input.removeData("dont_hide");
        e.stopPropagation();
      })
      .on("click", function(e) {
        //console.log("pane click");
        e.stopPropagation();
        var s = self.get_selected();
        if (s) {
          self.onselect(s, true);
          self.hide_all();
        }
      });
      var hoverover = function(e) {
        self.hoverover_list(e);
      };
      var hoverout = function(e) {
        self.hoverout_list(e);
      };
      l.on('mouseenter',hoverover).on('mouseleave',hoverout);
      //console.log(this.pane, this.list);

      this.input = $(input)
        .attr("autocomplete", "off")
        .off(".suggest")
        .on("remove.suggest", function(e) {
          self._destroy();
        })
        .on("keydown.suggest", function(e) {
          self.keydown(e);
        })
        .on("keypress.suggest", function(e) {
          self.keypress(e);
        })
        .on("keyup.suggest", function(e) {
          self.keyup(e);
        })
        .on("blur.suggest", function(e) {
          self.blur(e);
        })
        .on("textchange.suggest", function(e) {
          self.textchange();
        })
        .on("focus.suggest", function(e) {
          self.focus(e);
        })
        .on("paste.suggest input.suggest", function(e) {
          clearTimeout(self.paste_timeout);
          self.paste_timeout = setTimeout(function() {
            self.textchange();
          }, 0);
        });


        $(window)
          .on("resize.suggest", this.onresize)
          .on("scroll.suggest", this.onresize);
    };

    $.suggest[name].prototype = $.extend({}, $.suggest.prototype, prototype);
  };

  // base suggest prototype
  $.suggest.prototype = {

    _init: function() {},

    _destroy: function() {
      this.pane.remove();
      this.list.remove();
      this.input.off(".suggest");
      $(window)
        .off("resize.suggest", this.onresize)
        .off("scroll.suggest", this.onresize);
    },

    invalidate_position: function() {
      self._position = null;
    },

    status_start: function() {
      this.hide_all();
      this.status.siblings().hide();
      if (this._status.START) {
        this.status.text(this._status.START).show();
        if (!this.pane.is(":visible")) {
          this.position();
          this.pane_show();
        }
      }
      if (this._status.LOADING) {
        this.status.removeClass("loading");
      }
    },

    status_loading: function() {
      this.status.siblings().show();

      if (this._status.LOADING) {
        this.status.addClass("loading").text(this._status.LOADING).show();
        if (!this.pane.is(":visible")) {
          this.position();
          this.pane_show();
        }
      }
      else {
        this.status.hide();
      }
    },

    status_select: function() {
      this.status.siblings().show();
      if (this._status.SELECT) {
        this.status.text(this._status.SELECT).show();
      }
      else {
        this.status.hide();
      }
      if (this._status.LOADING) {
        this.status.removeClass("loading");
      }
    },

    status_error: function() {
      this.status.siblings().show();
      if (this._status.ERROR) {
        this.status.text(this._status.ERROR).show();
      }
      else {
        this.status.hide();
      }
      if (this._status.LOADING) {
        this.status.removeClass("loading");
      }
    },

    focus: function(e) {
      //console.log("focus", this.input.val() === "");
      var o = this.options,
          v = this.input.val();
      if (v === "") {
        this.status_start();
      }
      else {
        this.focus_hook(e);
      }
    },

    // override to be notified on focus and input has a value
    focus_hook: function(e) {
      //console.log("focus_hook", this.input.data("data.suggest"));
      if (!this.input.data("data.suggest") &&
          !this.pane.is(":visible") &&
          $("." + this.options.css.item, this.list).length) {
        this.position();
        this.pane_show();
      }
    },

    keydown: function(e) {
      var key = e.keyCode;
      if (key === 9) { // tab
        this.tab(e);
      }
      else if (key === 38 || key === 40) { // up/down
        if (!e.shiftKey) {
          // prevents cursor/caret from moving (in Safari)
          e.preventDefault();
        }
      }
    },

    keypress: function(e) {
      var key = e.keyCode;
      if (key === 38 || key === 40) { // up/down
        if (!e.shiftKey) {
          // prevents cursor/caret from moving
          e.preventDefault();
        }
      }
      else if (key === 13) { // enter
        this.enter(e);
      }
    },

    keyup: function(e) {
      var key = e.keyCode;
      //console.log("keyup", key);
      if (key === 38) { // up
        e.preventDefault();
        this.up(e);
      }
      else if (key === 40) { // down
        e.preventDefault();
        this.down(e);
      }
      else if (e.ctrlKey && key === 77) {
        $(".fbs-more-link", this.pane).click();
      }
      else if ($.suggest.is_char(e)) {
        //this.textchange();
        clearTimeout(this.keypress.timeout);
        var self = this;
        this.keypress.timeout = setTimeout(function() {
                                             self.textchange();
                                           }, 0);
      }
      else if (key === 27) {
        // escape - WebKit doesn't fire keypress for escape
        this.escape(e);
      }
      return true;
    },

    blur: function(e) {
      //console.log("blur", "dont_hide", this.input.data("dont_hide"),
      //            "data.suggest", this.input.data("data.suggest"));
      if (this.input.data("dont_hide")) {
        return;
      }
      var data = this.input.data("data.suggest");
      this.hide_all();
    },

    tab: function(e) {
      if (e.shiftKey || e.metaKey || e.ctrlKey) {
        return;
      }

      var o = this.options,
      visible = this.pane.is(":visible") &&
        $("." + o.css.item, this.list).length,
        s = this.get_selected();

      //console.log("tab", visible, s);

      if (visible && s) {
        this.onselect(s);
        this.hide_all();
      }
    },

    enter: function(e) {
      var o = this.options,
          visible = this.pane.is(":visible");

      //console.log("enter", visible);

      if (visible) {
        if (e.shiftKey) {
          this.shift_enter(e);
          e.preventDefault();
          return;
        }
        else if ($("." + o.css.item, this.list).length) {
          var s = this.get_selected();
          if (s) {
            this.onselect(s);
            this.hide_all();
            e.preventDefault();
            return;
          }
          else if (!o.soft) {
            var data = this.input.data("data.suggest");
            if ($("."+this.options.css.item + ":visible", this.list).length) {
              this.updown(false);
              e.preventDefault();
              return;
            }
          }
        }
      }
      if (o.soft) {
        // submit form
        this.soft_enter();
      }
      else {
        e.preventDefault();
      }
    },

    soft_enter: function(e) {},

    shift_enter: function(e) {},

    escape: function(e) {
      this.hide_all();
    },

    up: function(e) {
      //console.log("up");
      this.updown(true, e.ctrlKey || e.shiftKey);
    },

    down: function(e) {
      //console.log("up");
      this.updown(false, null, e.ctrlKey || e.shiftKey);
    },

    updown: function(goup, gofirst, golast) {
      //console.log("updown", goup, gofirst, golast);
      var o = this.options,
          css = o.css,
          p = this.pane,
          l = this.list;

      if (!p.is(":visible")) {
        if (!goup) {
          this.textchange();
        }
        return;
      }
      var li = $("."+css.item + ":visible", l);

      if (!li.length) {
        return;
      }

      var first = $(li[0]),
          last = $(li[li.length-1]),
          cur = this.get_selected() || [];

      clearTimeout(this.ignore_mouseover.timeout);
      this._ignore_mouseover = false;

      if (goup) {//up
        if (gofirst) {
          this._goto(first);
        }
        else if (!cur.length) {
          this._goto(last);
        }
        else if (cur[0] == first[0]) {
          first.removeClass(css.selected);
          this.input.val(this.input.data("original.suggest"));
          this.hoverout_list();
        }
        else {
          var prev = cur.prevAll("."+css.item + ":visible:first");
          this._goto(prev);
        }
      }
      else {//down
        if (golast) {
          this._goto(last);
        }
        else if (!cur.length) {
          this._goto(first);
        }
        else if (cur[0] == last[0]) {
          last.removeClass(css.selected);
          this.input.val(this.input.data("original.suggest"));
          this.hoverout_list();
        }
        else {
          var next = cur.nextAll("."+css.item + ":visible:first");
          this._goto(next);
        }
      }
    },

    _goto: function(li) {
      li.trigger("mouseover.suggest");
      var d = li.data("data.suggest");
      this.input.val(d ? d.name : this.input.data("original.suggest"));
      this.scroll_to(li);
    },

    scroll_to: function(item) {
      var l = this.list,
          scrollTop = l.scrollTop(),
          scrollBottom = scrollTop + l.innerHeight(),
          item_height = item.outerHeight(),
          offsetTop = item.prevAll().length * item_height,
          offsetBottom = offsetTop + item_height;
      if (offsetTop < scrollTop) {
        this.ignore_mouseover();
        l.scrollTop(offsetTop);
      }
      else if (offsetBottom > scrollBottom) {
        this.ignore_mouseover();
        l.scrollTop(scrollTop + offsetBottom - scrollBottom);
      }
    },

    textchange: function() {
      this.input.removeData("data.suggest");
      this.input.trigger("fb-textchange", this);
      var v = this.input.val();
      if (v === "") {
        this.status_start();
        return;
      }
      else {
        this.status_loading();
      }
      this.request(v);
    },

    request: function() {},

    response: function(data) {
      if (!data) {
        return;
      }
      if ("cost" in data) {
        this.trackEvent(this.name, "response", "cost", data.cost);
      }

      if (!this.check_response(data)) {
        return;
      }
      var result = [];

      if (Array.isArray(data)) {
        result = data;
      }
      else if ("result" in data) {
        result = data.result;
      }

      var args = $.map(arguments, function(a) {
        return a;
      });

      this.response_hook.apply(this, args);

      var first = null,
          self = this,
          o = this.options;

      $.each(result, function(i,n) {
        if (!n.id && n.mid) {
            // For compatitibility reasons, store the mid as id
            n.id = n.mid;
        }
        var li = self.create_item(n, data)
          .on("mouseover.suggest", function(e) {
            self.mouseover_item(e);
          });
          li.data("data.suggest", n);
          self.list.append(li);
          if (i === 0) {
            first = li;
          }
        });

      this.input.data("original.suggest", this.input.val());

      if ($("."+o.css.item, this.list).length === 0 && o.nomatch) {
        var $nomatch = $('<li class="fbs-nomatch">');
        if (typeof o.nomatch === "string") {
          $nomatch.text(o.nomatch);
        }
        else {
          if (o.nomatch.title) {
            $nomatch.append($('<em class="fbs-nomatch-text">').text(o.nomatch.title));
          }
          if (o.nomatch.heading) {
            $nomatch.append($('<h3>').text(o.nomatch.heading));
          }
          var tips = o.nomatch.tips;
          if (tips && tips.length) {
            var $tips = $('<ul class="fbs-search-tips">');
            $.each(tips, function(i,tip) {
              $tips.append($("<li>").text(tip));
            });
            $nomatch.append($tips);
          }
        }
        $nomatch.on("click.suggest", function(e) {
          e.stopPropagation();
        });
        this.list.append($nomatch);
      }

      args.push(first);
      this.show_hook.apply(this, args);
      this.position();
      this.pane_show();
    },

    pane_show: function() {
      var show = false;
      if ($("> li", this.list).length) {
        show = true;
      }
      if (!show) {
        this.pane.children(":not(." + this.options.css.list + ")")
          .each(function() {
            if ($(this).css("display") != "none") {
              show = true;
              return false;
            }
          });
      }
      if (show) {
        if (this.options.animate) {
          var self = this;
          this.pane.slideDown("fast", function() {
            self.input.trigger("fb-pane-show", self);
          });
        }
        else {
          this.pane.show();
          this.input.trigger("fb-pane-show", this);
        }
      }
      else {
        this.pane.hide();
        this.input.trigger("fb-pane-hide", this);
      }
    },

    create_item: function(data, response_data) {
      var css = this.options.css;
      var li = $("<li>").addClass(css.item);
      var label = $("<label>").text(data.name);
      var div = $("<div>").addClass(css.item_name).append(label);
      if(data.description) {
          div.append($('<span></span>').text(data.description));
      }
      li.append(div);
      return li;
    },

    mouseover_item: function(e) {
      if (this._ignore_mouseover) {
        return;
      }
      var target = e.target;
      if (target.nodeName.toLowerCase() !== "li") {
        target = $(target).parents("li:first");
      }
      var li = $(target),
          css = this.options.css,
          l = this.list;
      $("."+css.item, l)
        .each(function() {
          if (this !== li[0]) {
            $(this).removeClass(css.selected);
          }
        });
      if (!li.hasClass(css.selected)) {
        li.addClass(css.selected);
        this.mouseover_item_hook(li);
      }
    },

    mouseover_item_hook: function($li) {},

    hoverover_list: function(e) {},

    hoverout_list: function(e) {},

    check_response: function(response_data) {
      return true;
    },

    response_hook: function(response_data) {
      //this.pane.hide();
      this.list.empty();
    },

    show_hook: function(response_data) {
      // remove anything next to list - added by other suggest plugins
      this.status_select();
    },

    position: function() {
      var p  = this.pane,
          o = this.options;

      if (o.parent) {
        return;
      }

      if (!self._position) {
        var inp = this.input,
            pos = inp.offset(),
            input_width = inp.outerWidth(true),
            input_height = inp.outerHeight(true);
        pos.top += input_height;

        // show to calc dimensions
        var pane_width = p.outerWidth(),
            pane_height = p.outerHeight(),
            pane_right = pos.left + pane_width,
            pane_bottom = pos.top + pane_height,
            pane_half = pos.top + pane_height / 2,
            scroll_left =  $(window).scrollLeft(),
            scroll_top =  $(window).scrollTop(),
            window_width = $(window).width(),
            window_height = $(window).height(),
            window_right = window_width + scroll_left,
            window_bottom = window_height + scroll_top;

        // is input left or right side of window?
        var left = true;
        if ('left' == o.align ) {
          left = true;
        }
        else if ('right' == o.align ) {
          left = false;
        }
        else if (pos.left > (scroll_left + window_width/2)) {
          left = false;
        }
        if (!left) {
          left = pos.left - (pane_width - input_width);
          if (left > scroll_left) {
            pos.left = left;
          }
        }

        if (pane_half > window_bottom) {
          // can we see at least half of the list?
          var top = pos.top - input_height - pane_height;
          if (top > scroll_top) {
            pos.top = top;
          }
        }
        this._position = pos;
      }
      p.css({top:this._position.top, left:this._position.left});
    },

    ignore_mouseover: function(e) {
      this._ignore_mouseover = true;
      var self = this;
      this.ignore_mouseover.timeout =
        setTimeout(function() {
          self.ignore_mouseover_reset();
        }, 1000);
    },

    ignore_mouseover_reset: function() {
      this._ignore_mouseover = false;
    },

    get_selected: function() {
      var selected = null,
      select_class = this.options.css.selected;
      $("li", this.list)
        .each(function() {
          var $this = $(this);
          if ($this.hasClass(select_class) &&
              $this.is(":visible")) {
            selected = $this;
            return false;
          }
        });
      return selected;
    },

    onselect: function($selected, focus) {
      var data = $selected.data("data.suggest");
      if (data) {
        this.input.val(data.name)
          .data("data.suggest", data)
          .trigger("fb-select", data);

        this.trackEvent(this.name, "fb-select", "index",
        $selected.prevAll().length);
      }
    },

    trackEvent: function(category, action, label, value) {
      this.input.trigger("fb-track-event", {
        category: category,
        action:action,
        label: label,
        value: value
      });
      //console.log("trackEvent", category, action, label, value);
    },

    hide_all: function(e) {
      this.pane.hide();
      this.input.trigger("fb-pane-hide", this);
    }

  };


  $.extend($.suggest, {

    defaults: {

      status: [
        'Start typing to get suggestions...',
        'Searching...',
        'Select an item from the list:',
        'Sorry, something went wrong. Please try again later'
      ],

      soft: false,

      nomatch: "no matches",

      // CSS default class names
      css: {
        pane: "fbs-pane",
        list: "fbs-list",
        item: "fbs-item",
        item_name: "fbs-item-name",
        selected: "fbs-selected",
        status: "fbs-status"
      },

      css_prefix: null,

      parent: null,

      // option to animate suggest list when shown
      animate: false,

      zIndex: null
    },

    strongify: function(str, substr) {
      // safely markup substr within str with <strong>
      var strong;
      var index = str.toLowerCase().indexOf(substr.toLowerCase());
      if (index >= 0) {
        var substr_len = substr.length;
        var pre = document.createTextNode(str.substring(0, index));
        var em = $("<strong>").text(str.substring(index, index + substr_len));
        var post = document.createTextNode(str.substring(index + substr_len));
        strong = $("<div>")
                   .append(pre).append(em).append(post);
      }
      else {
        strong = $("<div>").text(str);
      }
      return strong;
    },

    keyCode: {
      //BACKSPACE: 8,
      CAPS_LOCK: 20,
      //COMMA: 188,
      CONTROL: 17,
      //DELETE: 46,
      DOWN: 40,
      END: 35,
      ENTER: 13,
      ESCAPE: 27,
      HOME: 36,
      INSERT: 45,
      LEFT: 37,
      //NUMPAD_ADD: 107,
      //NUMPAD_DECIMAL: 110,
      //NUMPAD_DIVIDE: 111,
      NUMPAD_ENTER: 108,
      //NUMPAD_MULTIPLY: 106,
      //NUMPAD_SUBTRACT: 109,
      PAGE_DOWN: 34,
      PAGE_UP: 33,
      //PERIOD: 190,
      RIGHT: 39,
      SHIFT: 16,
      SPACE: 32,
      TAB: 9,
      UP: 38,
      OPTION: 18,
      APPLE: 224
    },

    is_char: function(e) {
      if (e.type === "keypress") {
        if ((e.metaKey || e.ctrlKey) && e.charCode === 118) {
          // ctrl+v
          return true;
        }
        else if ("isChar" in e) {
          return e.isChar;
        }
      }
      else {
        var not_char = $.suggest.keyCode.not_char;
        if (!not_char) {
          not_char = {};
          $.each($.suggest.keyCode, function(k,v) {
            not_char[''+v] = 1;
          });
          $.suggest.keyCode.not_char = not_char;
        }
        return !(('' + e.keyCode) in not_char);
      }
    },

    /**
     * Parse input string into actual query string and structured name:value list
     *
     * "bob dylan type:artist" -> ["bob dylan", ["type:artist"]]
     * "Dear... type:film name{full}:Dear..." -> ["Dear...", ["type:film", "name{full}:Dear..."]]
     */
    parse_input: function(str) {
        // only pick out valid name:value pairs
        // a name:value is valid
        // 1. if there are no spaces before/after ":"
        // 2. name does not have any spaces
        // 3. value does not have any spaces OR value is double quoted
        var regex = /(\S+)\:(?:\"([^\"]+)\"|(\S+))/g;
        var qstr = str;
        var filters = [];
        var overrides = {};
        var m = regex.exec(str);
        while (m) {
            if (m[1] in SEARCH_PARAMS) {
                overrides[m[1]] = $.isEmptyObject(m[2]) ? m[3] : m[2];
            }
            else {
                filters.push(m[0]);
            }
            qstr = qstr.replace(m[0], "");
            m = regex.exec(str);
        }
        qstr = jQueryTrim(qstr.replace(/\s+/g, " "));
        return [qstr, filters, overrides];
    },

    /**
     * Convenient methods and regexs to determine valid mql ids.
     */
    mqlkey_fast: /^[_A-Za-z0-9][A-Za-z0-9_-]*$/,
    mqlkey_slow: /^(?:[A-Za-z0-9]|\$[A-F0-9]{4})(?:[A-Za-z0-9_-]|\$[A-F0-9]{4})*$/,
    check_mql_key: function(val) {
        if ($.suggest.mqlkey_fast.test(val)) {
            return true;
        }
        else if ($.suggest.mqlkey_slow.test(val)) {
            return true;
        }
        return false;
    },
    check_mql_id: function(val) {
        if (val.indexOf("/") === 0) {
            var keys = val.split("/");
            // remove beginning '/'
            keys.shift();
            if (keys.length == 1 && keys[0] === "") {
                // "/" is a valid id
                return true;
            }
            else {
                for (var i=0,l=keys.length; i<l; i++) {
                    if (!$.suggest.check_mql_key(keys[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        else {
            return false;
        }
    },

    is_system_type: function(type_id) {
      if (type_id == null) {
        return false;
      }
      return (type_id.indexOf("/type/") === 0);
    }
  });


  // some base implementation that we overwrite but want to call
  var base = {
    _destroy: $.suggest.prototype._destroy,
    show_hook: $.suggest.prototype.show_hook
  };


  // *THE* Freebase suggest implementation
  $.suggest("suggest", {
    _init: function() {
      var self = this,
          o = this.options;
    },

    shift_enter: function(e) {
      if (this.options.suggest_new) {
        this.suggest_new();
        this.hide_all();
      }
    },

    request: function(val, cursor) {
      var self = this,
          o = this.options;

      var query = val;

      var filter = o.ac_param.filter || [];

      // SEARCH_PARAMS can be overridden inline
      var extend_ac_param = null;

      if (typeof filter === "string") {
          // the original filter may be a single filter param (string)
          filter = [filter];
      }
      // clone original filters so that we don't modify it
      filter = filter.slice();
      if (o.advanced) {
          // parse out additional filters in input value
          var structured = $.suggest.parse_input(query);
          query = structured[0];
          if (structured[1].length) {
              // all advance filters are ANDs
              filter.push("(all " + structured[1].join(" ") + ")");
          }
          extend_ac_param = structured[2];
          if ($.suggest.check_mql_id(query)) {
              // handle anything that looks like a valid mql id:
              // filter=(all%20alias{start}:/people/pers)&prefixed=true
              filter.push("(any alias{start}:\"" + query + "\" mid:\"" +
                          query + "\")");
              extend_ac_param['prefixed'] = true;
              query = "";
          }
      }

      var data = {};
      data[o.query_param_name] = query;

      if (cursor) {
        data.cursor = cursor;
      }
      $.extend(data, o.ac_param, extend_ac_param);
      if (filter.length) {
          data.filter = filter;
      }

      var url = o.service_url + o.service_path + "?" + $.param(data, true);
      var cached = $.suggest.cache[url];
      if (cached) {
        this.response(cached, cursor ? cursor : -1, true);
        return;
      }

      clearTimeout(this.request.timeout);

      var ajax_options = {
        url: o.service_url + o.service_path,
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
          $.suggest.cache[url] = data;
          data.prefix = val;  // keep track of prefix to match up response with input value
          self.response(data, cursor ? cursor : -1);
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
        dataType: o.access === undefined ? "jsonp" : o.access,
        cache: true
      };

      this.request.timeout = setTimeout(function() {
        $.ajax(ajax_options);
      }, o.xhr_delay);
    },

    create_item: function(data, response_data) {
      var css = this.options.css;
      var li =  $("<li>").addClass(css.item);
      var label = $("<label>")
        .append($.suggest.strongify(data.name || data.id, response_data.prefix));
      var name = $("<div>").addClass(css.item_name)
        .append(label);
      var nt = data.notable;
      if (data.under) {
        $(":first", label).append($("<small>").text(" ("+data.under+")"));
      }
      if ((nt != null && $.suggest.is_system_type(nt.id)) ||
          (this.options.scoring != null  &&
           this.options.scoring.toUpperCase() === 'SCHEMA')) {
        $(":first", label).append($("<small>").text(" ("+data.id+")"));
      }
      var types = data.type;
      li.append(name);
      var type = $("<div>").addClass(css.item_type);
      if (nt && nt.name) {
        type.text(nt.name);
      }
      else if (this.options.show_id && data.id) {
          // display human readable id if no notable type
          type.text(data.id);
      }
      name.prepend(type);
      if(data.description) {
          name.append($("<span></span>").text(data.description));
      }

      // If we know of a view URL for this suggest service,
      // clicking with the middle button sends the user to
      // the view page.
      if('view_url' in this.options && data.id) {
        var view_url = this.options.view_url.replace('{{id}}', data.id).replace('${id}', data.id);
        li.on('mousedown', function(e) {
           if (e.which == 2) {
              var win = window.open(view_url, '_blank');
              win.focus();
              e.preventDefault();
           }
        });
      }

      //console.log("create_item", li);
      return li;
    },


    check_response: function(response_data) {
      return response_data.prefix === this.input.val();
    },


    show_hook: function(response_data, cursor, first) {
      base.show_hook.apply(this, [response_data]);

      var o = this.options,
          self = this,
          p = this.pane,
          l = this.list,
          result = response_data.result,
          more = $(".fbs-more", p),
          suggestnew = $(".fbs-suggestnew", p),
          status = $(".fbs-status", p);

      // spell/correction
      var correction = response_data.correction;
      if (correction && correction.length) {
        var spell_link = $('<a class="fbs-spell-link" href="#">').text(correction[0])
          .on("click.suggest", function(e) {
            e.preventDefault();
            e.stopPropagation();
            self.input.val(correction[0]).trigger("textchange");
          });
        self.status
          .empty()
          .append("Search instead for ")
          .append(spell_link)
          .show();
      }

      // more
      if (result && result.length && "cursor" in response_data) {
        if (!more.length) {
          var more_link = $('<a class="fbs-more-link" href="#" title="(Ctrl+m)">view more</a>');
          more = $('<div class="fbs-more">').append(more_link);
          more_link.on("click.suggest", function(e) {
            e.preventDefault();
            e.stopPropagation();
            var m = $(this).parent(".fbs-more");
            self.more(m.data("cursor.suggest"));
          });
          l.after(more);
        }
        more.data("cursor.suggest", response_data.cursor);
        more.show();
      }
      else {
        more.remove();
      }

      // suggest_new
      if (o.suggest_new) {
        if (!suggestnew.length) {
          // create suggestnew option
          var button = $('<button class="fbs-suggestnew-button">');
          button.text(o.suggest_new);
          suggestnew = $('<div class="fbs-suggestnew">')
            .append('<div class="fbs-suggestnew-description">Your item not in the list?</div>')
            .append(button)
            .append('<span class="fbs-suggestnew-shortcut">(Shift+Enter)</span>')
            .on("click.suggest", function(e) {
              e.stopPropagation();
              self.suggest_new(e);
            });
          p.append(suggestnew);
        }
        suggestnew.show();
      }
      else {
        suggestnew.remove();
      }

      // scroll to first if clicked on "more"
      if (first && first.length && cursor > 0) {
        var top = first.prevAll().length * first.outerHeight();
        var scrollTop = l.scrollTop();
        l.animate({scrollTop: top}, "slow", function() {
          first.trigger("mouseover.suggest");
        });
      }
    },

    suggest_new: function(e) {
      var v = this.input.val();
      if (v === "") {
        return;
      }
      //console.log("suggest_new", v);
      this.input
        .data("data.suggest", v)
        .trigger("fb-select-new", v);
      this.trackEvent(this.name, "fb-select-new", "index", "new");
      this.hide_all();
    },

    more: function(cursor) {
      if (cursor) {
        var orig = this.input.data("original.suggest");
        if (orig !== null) {
          this.input.val(orig);
        }
        this.request(this.input.val(), cursor);
        this.trackEvent(this.name, "more", "cursor", cursor);
      }
      return false;
    },

  });

  // Freebase suggest settings
  $.extend($.suggest.suggest, {

    defaults: {
      /**
       * filter, spell, lang, exact, scoring, key, prefixed, stemmed, format
       *
       * are the new parameters used by the new freebase search on googleapis.
       * Please refer the the API documentation as these parameters
       * will be transparently passed through to the search service.
       *
       * @see http://wiki.freebase.com/wiki/ApiSearch
       */

      // search filters
      filter: null,

      // spelling corrections
      spell: "always",

      exact: false,

      scoring: null,

      // language to search (default to en)
      lang: null, // NULL defaults to "en",

      // API key: required for googleapis
      key: null,

      prefixed: true,

      stemmed: null,

      format: null,

      // Enable structured input name:value pairs that get appended to the search filters
      // For example:
      //
      //   "bob dylan type:artist"
      //
      // Would get translated to the following request:
      //
      //    /freebase/v1/search?query=bob+dylan&filter=<original filter>&filter=(all type:artist)
      //
      advanced: true,

      // If an item does not have a "notable" field, display the id or mid of the item
      show_id: true,

      // query param name for the search service.
      // If query name was "foo": search?foo=...
      query_param_name: "query",

      // base url for autocomplete service
      service_url: "https://www.googleapis.com/freebase/v1",

      // service_url + service_path = url to autocomplete service
      service_path: "/search",

      // 'left', 'right' or null
      // where list will be aligned left or right with the input
      align: null,

      

      // text snippet you want to show for the suggest
      // new option
      // clicking will trigger an fb-select-new event
      // along with the input value
      suggest_new: null,

      nomatch: {
        title: "No suggested matches",
        heading: "Tips on getting better suggestions:",
        tips: [
          "Enter more or fewer characters",
          "Add words related to your original search",
          "Try alternate spellings",
          "Check your spelling"
        ]
      },

      
    },

    /**
     * Get a value from an object multiple levels deep.
     */
     get_value_by_keys: function(obj, var_args) {
       var keys = $.isArray(var_args) ? var_args :
           Array.prototype.slice.call(arguments, 1);
       for (var i = 0; i < keys.length; i++) {
         obj = obj[keys[i]];
         if (obj == null) {
           break;
         }
       }
       return obj;
     },

    /**
     * Utility method to get values of an object specified by one or more
     * (nested) keys. For example:
     * <code>
     *   get_value(my_dict, ['foo', 'bar'])
     *   // Would resolve to my_dict['foo']['bar'];
     * </code>
     * The method will return null, if any of the path specified refers to
     * a null or undefined value in the object.
     *
     * If resolved_search_values is TRUE, this will flatten search api
     * values that are arrays of entities ({mid, name})
     * to an array of their names and ALWAYS return an array of strings
     * of length >= 0.
     */
    get_value: function(obj, path, resolve_search_values) {
      if (obj == null || path == null || path.length == 0) {
        return null;
      }
      if (!$.isArray(path)) {
        path = [path];
      }
      var v =  $.suggest.suggest.get_value_by_keys(obj, path);
      if (resolve_search_values) {
        if (v == null) {
          return [];
        }
        if (!$.isArray(v)) {
          v = [v];
        }
        var values = [];
        $.each(v, function(i, value) {
          if ($.type(value) === 'object') {
            if (value['name'] != null) {
              value = value['name'];
            }
            else if (value['id'] || value['mid']) {
              value = value['id'] || value['mid'];
            }
            else if (value['value'] != null) {
              // For cvts, value may contain other useful info (like date, etc.)
              var cvts = [];
              $.each(value, function(k, v) {
                if (k !== 'value') {
                  cvts.push(v);
                }
              });
              value = value['value'];
              if (cvts.length) {
                value += ' (' + cvts.join(', ') + ')';
              }
            }
          }
          if ($.isArray(value) && value.length) {
            value = value[0].value;
          }
          if (value != null) {
            values.push(value);
          }
        });
        return values;
      }
      // Cast undefined to null.
      return v == null ? null : v;
    },

    is_commons_id: function(id) {
      if (/^\/base\//.test(id) || /^\/user\//.test(id)) {
        return false;
      }
      return true;
    },

  });


  var f = document.createElement("input");

})(jQuery);
