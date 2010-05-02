  if (typeof window.Sign == 'undefined') {
    window.Sign = {
      window_position: function() {
        var position = {};
        
        if (typeof(window.innerWidth) == 'number') {
          // Non-IE
          position['width'] = window.outerWidth;
          position['height'] = window.outerHeight;
          position['top'] = window.screenY;
          position['left'] = window.screenX;
        } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
          // IE 6+ in 'standards compliant mode'
          position['width'] = document.body.clientWidth;
          position['height'] = document.body.clientHeight;
          position['top'] = window.screenTop;
          position['left'] = window.screenLeft;
        }
        
        return position;
      },
      
      popup : function(url, width, height, windowname) {
        width = width || 700;
        height = height || 500;
        
        var pos = window.Sign.window_position();
        var left = Math.floor((pos['width']-width)/2) + pos['left'];
        var top = Math.floor((pos['height']-height)/2) + pos['top'];
        
        // Chrome might fix this bug, but until then add some padding
        //  to the height of the popup for the urlbar 
        var is_chrome = /chrome/.test(navigator.userAgent.toLowerCase());
        if (is_chrome) {
          height += 50;
        }
        
        var params = {
          width: width,
          height: height,
          top: top,
          left: left,
          directories: 'no',
          location: 'no',
          menubar: 'no',
          resizable: 'no',
          scrollbars: 'yes',
          status: 'no',
          toolbar: 'no'
        };
        
        var params_list = [];
        for (var key in params) {
          params_list.push(key + "=" + params[key]);
        }
                
        return window.open(url, windowname || "", params_list.join(","));
      },
                  
      signintize : function(cont) {
        $('.signedin').show();
        $('.signedout').hide();
        if (window.user) {
          $('.user').html('<a href="http://freebase.com/view' + window.user.id + '">' + window.user.username + '</a>');
        }
        if (typeof cont == 'function') cont();
      },
      
      signin : function(success, provider, width, height) {
        var newwin = window.Sign.popup("/command/authorize/" + provider, width, height);
        
        if (newwin !== null) {
            newwin.opener = window;
        }
        
        window.onauthorization = function() {
          if (typeof success == 'undefined') {
            window.location.reload();
          } else {
            $.ajax({
              url: "/command/check-authorization/" + provider,
              dataType: "json",
              success: function(data) {
                window.user = data;
                window.Sign.signintize(success);
              }
            });
          }
        };
        
        if (window.focus && newwin !== null) {
          newwin.focus();
        }
        
        return false;
      },
      
      signoutize : function(cont) {
        $('.signedin').hide();
        $('.signedout').show();              
        if (typeof cont == 'function') cont();
      },
      
      signout : function(success,provider) {
        $.ajax({
          url: "/command/deauthorize/" + provider,
          success: function() {
            if (typeof success == 'undefined') {
              window.location.reload();
            } else {
              window.Sign.signoutize(success);
            }
          }
        });
      }
    };
  }
  