/**
 * Copyright (c) 2008, Ben XO <me@ben-xo.com>
 * 
 * Dual licensed under the MIT (MIT-LICENSE.txt)
 * and GPL (GPL-LICENSE.txt) licenses.
 * 
 * 
 * This is a jQuery plugin that allows events to be handled in reverse order of
 * binding - add events to the head of the queue rather than the end.
 * 
 * Particularly useful if you apply your "do"s in a modular order and wish to 
 * apply your "undo"s in reverse order.
 * 
 * It adds the following two methods:
 * 
 * reverse(event): reverses the order that functions attached to <event> are 
 *                 applied to this element.
 * 
 * stack( event, data, fn ): just like bind(), except adds to the front of the 
 *                           event queue instead of the end.
 *                           
 * Also, every event method (.click(), .focus(), .resize(), .submit(), etc - 
 * 21 methods in all) can take an extra parameter "reverse" that controls if 
 * the event is added to the front or end of the event queue. For example:
 * 
 * $("body").click(function() { alert("1"); });
 * $("body").click(function() { alert("2"); });
 * $("body").click(function() { alert("3"); }, true);
 * 
 * ... will show "3", "1" then "2" when the document is clicked.
 * 
 * @version 0.3
 */

jQuery.fn.extend({
	
	// reverse the order of events bound to an element
	reverse: function( event ) {
	    var events = this.data("events");
		if ( events != undefined && events[event] != undefined) {
			// can't reverse what's not there
		    var reverseEvent = new Array;
			for (var e in events[event]) {
				reverseEvent.unshift(events[event][e]);
			}
			events[event] = reverseEvent;
		}
		return this;
	},
	
	// add an event to the front of an event queue rather than the back
	// we do this by reversing the queue, adding, then reversing again
	stack: function( event, data, fn ) {
		return this
			.reverse( event )
			.bind( event, data, fn )
			.reverse( event )
		;
	},
	
	hover: function( fnOver, fnOut, reverse ) {
		if(reverse) {
			return this
				.stack('mouseenter', fnOver)
				.stack('mouseleave', fnOut)
			;
		} else {
			return this			
				.bind('mouseenter', fnOver)
				.bind('mouseleave', fnOut)
			;
		}
	} 
});

// every event handler - blur(), focus(), click() etc now has a 2nd param which
// if true will add the passed function to the beginning of the event list 
// instead of the end
jQuery.each( ("blur,focus,load,resize,scroll,unload,click,dblclick," +
	"mousedown,mouseup,mousemove,mouseover,mouseout,change,select," +
	"submit,keydown,keypress,keyup,error").split(","), function(i, name){

	// Handle event binding
	jQuery.fn[name] = function(fn, reverse){
		if(fn) {
			return reverse ? 
				this.stack(name, fn) : 
				this.bind(name, fn)
			;
		} else {
			return this.trigger(name);	
		}
	};
});