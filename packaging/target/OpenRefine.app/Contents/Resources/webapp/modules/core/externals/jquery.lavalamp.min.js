/**
 * Lava Lamp
 * http://lavalamp.magicmediamuse.com/
 *
 * Author
 * Richard Hung
 * http://www.magicmediamuse.com/
 *
 * Version
 * 1.0.6
 * 
 * Copyright (c) 2014 Richard Hung.
 * 
 * License
 * Lava Lamp by Richard Hung is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License.
 * http://creativecommons.org/licenses/by-nc/3.0/deed.en_US
 */


(function(e){var t={init:function(t){var i={easing:"swing",duration:700,margins:false,setOnClick:false,activeObj:".active",autoUpdate:false,updateTime:100,enableHover:true};var t=e.extend({},i,t);return this.each(function(){var i=t.easing;var s=t.duration;var o=t.margins;var u=t.setOnClick;var a=t.activeObj;var f=t.autoUpdate;var l=t.updateTime;var c=t.enableHover;var h=e(this);var p=h.children();var d=h.children(a);if(d.length==0){d=p.eq(0)}h.data({easing:i,duration:s,margins:o,setOnClick:u,active:d,enableHover:c,isAnim:false});h.addClass("lavalamp").css({position:"relative"});var v=e('<div class="lavalamp-object" />').prependTo(h).css({position:"absolute"});p.addClass("lavalamp-item").css({zIndex:5,position:"relative"});var m=d.outerWidth(o);var g=d.outerHeight(o);var y=d.position().top;var b=d.position().left;var w=d.css("marginTop");var E=d.css("marginLeft");if(!o){E=parseInt(E);w=parseInt(w);b=b+E;y=y+w}v.css({width:m,height:g,top:y,left:b});var S=false;n=function(){var t=e(this);S=true;h.lavalamp("anim",t)};r=function(){var e=h.data("active");S=false;h.lavalamp("anim",e)};if(c){h.on("mouseenter",".lavalamp-item",n);h.on("mouseleave",".lavalamp-item",r)}if(u){p.click(function(){d=e(this);h.data("active",d).lavalamp("update")})}if(f){setInterval(function(){var e=h.data("isAnim");if(S==false&&e==false){h.lavalamp("update")}},l)}})},destroy:function(){return this.each(function(){var t=e(this);var i=t.children(".lavalamp-item");var s=t.data("enableHover");if(s){t.off("mouseenter",".lavalamp-item",n);t.off("mouseleave",".lavalamp-item",r)}t.removeClass("lavalamp");i.removeClass("lavalamp-item");t.children(".lavalamp-object").remove();t.removeData()})},update:function(){return this.each(function(){var t=e(this);var n=t.children(":not(.lavalamp-object)");var r=t.data("active");var i=t.children(".lavalamp-object");n.addClass("lavalamp-item").css({zIndex:5,position:"relative"});t.lavalamp("anim",r)})},anim:function(e){var t=this;var n=t.data("duration");var r=t.data("easing");var i=t.data("margins");var s=t.children(".lavalamp-object");var o=e.outerWidth(i);var u=e.outerHeight(i);var a=e.position().top;var f=e.position().left;var l=e.css("marginTop");var c=e.css("marginLeft");if(!i){c=parseInt(c);l=parseInt(l);f=f+c;a=a+l}t.data("isAnim",true);s.stop(true,false).animate({width:o,height:u,top:a,left:f},n,r,function(){t.data("isAnim",false)})}};e.fn.lavalamp=function(n){if(t[n]){return t[n].apply(this,Array.prototype.slice.call(arguments,1))}else if(typeof n==="object"||!n){return t.init.apply(this,arguments)}else{e.error("Method "+n+" does not exist on jQuery.lavalamp")}};var n,r})(jQuery)