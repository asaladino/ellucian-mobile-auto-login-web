var AutoLoginWeb = {
    loadUserIn : function(username, password) {
        var extraFields = {{{extraFields}}};
        for(var selector in extraFields) {
            document.querySelector(selector).value = extraFields[selector];
        }
        var usernameElement = document.querySelector("{{usernameSelector}}");
        if(usernameElement) {
            usernameElement.value = username;
        }
        var passwordElement = document.querySelector("{{passwordSelector}}");
        if(passwordElement) {
            passwordElement.value = password;
        }
        var formElement = document.querySelector("{{formSelector}}");
        if(formElement) {
            formElement.click();
            formElement.submit();
        }
    },
    checkForFields: function() {
        AutoLoginWeb.foundSelector("{{usernameSelector}}");
    },
    checkLoggedInAndRedirect: function() {
        AutoLoginWeb.foundSelectorThenRedirect("{{redirectSelector}}", "{{redirectUrl}}");
    },
    foundSelector: function(selector) {
        var item = document.querySelector(selector);
        if(item) {
            AutoLoginWebInterface.foundForm();
        }
    },
    foundSelectorThenRedirect: function(selector, redirect) {
        var item = document.querySelector(selector);
        if(item) {
            window.location = redirect;
        }
    },
    addCss: function(id, url) {
        var htmlStyle = document.createElement('link');
        htmlStyle.setAttribute("rel", "stylesheet");
        htmlStyle.setAttribute("id", id);
        htmlStyle.setAttribute("type", "text/css");
        htmlStyle.setAttribute("href", url);
        document.querySelector('head').appendChild(htmlStyle);
    },
    addJs: function(url) {
        var htmlJs = document.createElement('script');
        htmlJs.setAttribute("src", url);
        document.querySelector('head').appendChild(htmlJs);
    },
    removeClass: function(search, theClass) {
        var elems = document.querySelectorAll(search);
        for (var i in elems) {
            elems[i].setAttribute("class", elems[i].getAttribute().replace(theClass, ""));
        }
    },
    changeTag: function(fromTag, toTag) {
        var count = 0;
        while (count < 100) {
            count++;
            var a = document.querySelectorAll(fromTag);
            if(a == 0) {
                break;
            }
            for (var i = 0; i < a.length; i++) {
                var src = a[i];
                var el = document.createElement(toTag);
                var attrs = src.attributes;
                for (var j = 0; j < attrs.length; j++) {
                    el.setAttribute(attrs[j].name, attrs[j].value);
                }
                el.innerHTML = src.innerHTML;
                src.parentNode.replaceChild(el, src);
            }
        }
    },
    removeElements: function(selectors) {
      var elements = document.querySelectorAll(selectors);
      for (var i in elements) {
        var elem = elements[i];
        if (typeof elem === "object") {
          elem.remove();
        }
      }
    },
    deleteCss: function() {
      var css = document.querySelectorAll('link, style');
      for (var i in css) {
        var style = css[i];
        if (typeof style === "object" &&
          style.id == "" &&
          style.getAttribute("rel") === "stylesheet") {
          style.remove();
        }
      }
    }
};
