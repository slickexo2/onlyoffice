  /**
   * Onlyoffice Editor client.
   */
(function($, cCometD, redux) {
  // ******** polyfills ********
  if (!String.prototype.endsWith) {
    String.prototype.endsWith = function(search, this_len) {
      if (this_len === undefined || this_len > this.length) {
        this_len = this.length;
      }
      return this.substring(this_len - search.length, this_len) === search;
    };
  }
  if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(searchString, position) {
      position = position || 0;
      return this.substr(position, searchString.length) === searchString;
    };
  }

  // ******** Constants ********
  var ACCESS_DENIED = "access-denied";
  var NODE_NOT_FOUND = "node-not-found";

  // ******** Utils ********
  /**
   * Stuff grabbed from CW's commons.js
   */

  var pageBaseUrl = function(theLocation) {
    if (!theLocation) {
      theLocation = window.location;
    }

    var theHostName = theLocation.hostname;
    var theQueryString = theLocation.search;

    if (theLocation.port) {
      theHostName += ":" + theLocation.port;
    }

    return theLocation.protocol + "//" + theHostName;
  };

  /**
   * Add style to current document (to the end of head).
   */
  var loadStyle = function(cssUrl) {
    if (document.createStyleSheet) {
      document.createStyleSheet(cssUrl);
      // IE way
    } else {
      if ($("head").find("link[href='" + cssUrl + "']").length == 0) {
        var headElems = document.getElementsByTagName("head");
        var style = document.createElement("link");
        style.type = "text/css";
        style.rel = "stylesheet";
        style.href = cssUrl;
        headElems[headElems.length - 1].appendChild(style);
      } // else, already added
    }
  };

  /** For debug logging. */
  var log = function(msg, err) {
    var logPrefix = "[onlyoffice] ";
    if (typeof console != "undefined" && typeof console.log != "undefined") {
      var isoTime = " -- " + new Date().toISOString();
      var msgLine = msg;
      if (err) {
        msgLine += ". Error: ";
        if (err.name || err.message) {
          if (err.name) {
            msgLine += "[" + err.name + "] ";
          }
          if (err.message) {
            msgLine += err.message;
          }
        } else {
          msgLine += (typeof err === "string" ? err : JSON.stringify(err)
              + (err.toString && typeof err.toString === "function" ? "; " + err.toString() : ""));
        }

        console.log(logPrefix + msgLine + isoTime);
        if (typeof err.stack != "undefined") {
          console.log(err.stack);
        }
      } else {
        if (err !== null && typeof err !== "undefined") {
          msgLine += ". Error: '" + err + "'";
        }
        console.log(logPrefix + msgLine + isoTime);
      }
    }
  };

  var getPortalUser = function() {
    return eXo.env.portal.userName;
  };

  var tryParseJson = function(message) {
    var src = message.data ? message.data : (message.error ? message.error : message.failure);
    if (src) {
      try {
        if (typeof src === "string" && (src.startsWith("{") || src.startsWith("["))) {
          return JSON.parse(src);
        }
      } catch (e) {
        log("Error parsing '" + src + "' as JSON: " + e, e);
      }
    }
    return src;
  };



  // ******** REST services ********
  var prefixUrl = pageBaseUrl(location);

  var initRequest = function(request) {
    var process = $.Deferred();

    // stuff in textStatus is less interesting: it can be "timeout",
    // "error", "abort", and "parsererror",
    // "success" or smth like that
    request.fail(function(jqXHR, textStatus, err) {
      if (jqXHR.status != 309) {
        // check if response isn't JSON
        var data;
        try {
          data = $.parseJSON(jqXHR.responseText);
          if (typeof data == "string") {
            // not JSON
            data = jqXHR.responseText;
          }
        } catch (e) {
          // not JSON
          data = jqXHR.responseText;
        }
        // in err - textual portion of the HTTP status, such as "Not
        // Found" or "Internal Server Error."
        process.reject(data, jqXHR.status, err, jqXHR);
      }
    });
    // hacking jQuery for statusCode handling
    var jQueryStatusCode = request.statusCode;
    request.statusCode = function(map) {
      var user502 = map[502];
      if (!user502) {
        map[502] = function() {
          // treat 502 as request error also
          process.fail("Bad gateway", 502, "error");
        };
      }
      return jQueryStatusCode(map);
    };

    request.done(function(data, textStatus, jqXHR) {
      process.resolve(data, jqXHR.status, textStatus, jqXHR);
    });

    request.always(function(data, textStatus, errorThrown) {
      var status;
      if (data && data.status) {
        status = data.status;
      } else if (errorThrown && errorThrown.status) {
        status = errorThrown.status;
      } else {
        status = 200;
        // what else we could to do
      }
      process.always(status, textStatus);
    });

    // custom Promise target to provide an access to jqXHR object
    var processTarget = {
      request : request
    };
    return process.promise(processTarget);
  };

  /*
  var configGet = function(workspace, path) {
    var request = $.ajax({
      type : "GET",
      url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + workspace + path,
      dataType : "json"
    });
    return initRequest(request);
  };
  */

  var configPost = function(workspace, path) {
    var request = $.ajax({
      type : "POST",
      url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + workspace + path,
      dataType : "json"
    });

    return initRequest(request);
  };
  
  /*
  var configGetByKey = function(key) {
    var request = $.ajax({
      type : "GET",
      url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + key,
      dataType : "json"
    });
    return initRequest(request);
  };
  */
  
  var documentPost = function(workspace, path) {
    var request = $.ajax({
      type : "POST",
      url : prefixUrl + "/portal/rest/onlyoffice/editor/document/" + workspace + path,
      dataType : "json"
    });

    return initRequest(request);
  };

  var stateGet = function(userId, fileKey) {
    var request = $.ajax({
      type : "GET",
      url : prefixUrl + "/portal/rest/onlyoffice/editor/state/" + userId + "/" + fileKey,
      dataType : "json"
    });

    return initRequest(request);
  };

  /**
   * Editor core class.
   */
  function Editor() {

    var self = this;
    var store;
    
    var docActionsReducer = function(state = {}, action){
      if(action.type === 'DOCUMENT_SAVED'){
        return {
          status: 'DOCUMENT_SAVED',
          userId: action.payload.userId
          }
      }
      log("Unknown action type:" + action.type);
      return state;
    };

    // Generates DOCUMENT SAVED action with userId
    var docSavedAction = function(userId){
      return {
        type: 'DOCUMENT_SAVED',
        payload: {
            userId: userId
          }
      }
    };

    var onError = function(event) {
      log("ONLYOFFICE Document Editor reports an error: " + JSON.stringify(event));
    };

    var onReady = function() {
      log("ONLYOFFICE Document Editor is ready");
    };

    var onBack = function() {
      log("ONLYOFFICE Document Editor on Back");
    };

    var onDocumentStateChange = function(event) {
      log("documentChange: " + JSON.stringify(event));
      if (event.data) {
        log("ONLYOFFICE The document changed");
        UI.documentChanged(event);
      } else {
        log("ONLYOFFICE Changes are collected on document editing service");
      }
    };

    /**
     * Create an editor configuration (for use to create the editor client UI).
     */
    var create = function(config) {
      var process = $.Deferred();
      if (config) {
        // prepare config
        log("ONLYOFFICE editor config: " + JSON.stringify(config));

        // customize Onlyoffice JS config
        config.type = "desktop";
        config.height = "100%";
        config.width = "100%";
        config.embedded = {
          fullscreenUrl : config.document.url,
          saveUrl : config.document.url,
          embedUrl : config.document.url,
          shareUrl : config.document.url,
          toolbarDocked : "top"
        };
        config.events = {
          "onDocumentStateChange" : onDocumentStateChange,
          "onError" : onError,
          "onReady" : onReady,
          "onBack" : onBack
        };
        config.editorConfig.customization = {
          "chat" : false,
          "compactToolbar" : true,
          "goback" : {
            "blank" : true,
            "text" : "Go to Document",
            "url" : config.explorerUrl
          },
          "help" : true,
          "logo" : {
            "image" : prefixUrl + "/onlyoffice/images/exo-icone.png",
            "imageEmbedded" : prefixUrl + "/eXoSkin/skin/images/themes/default/platform/skin/ToolbarContainer/HomeIcon.png",
            "url" : prefixUrl + "/portal"
          },
          "customer" : {
            "info" : "eXo Platform",
            "logo" : prefixUrl + "/eXoSkin/skin/images/themes/default/platform/skin/ToolbarContainer/HomeIcon.png",
            "mail" : "support@exoplatform.com",
            "name" : "Support",
            "www" : "exoplatform.com"
          }
        };
        config.editorConfig.plugins = {
          "autostart" : [],
          "pluginsData" : []
        };

        // XXX need let Onlyoffice to know about a host of API end-points,
        // as we will add their script dynamically to the DOM, the script will not be able to detect an URL
        // thus we use "extensionParams" observed in the script code
        if ("undefined" == typeof (extensionParams) || null == extensionParams) {
          extensionParams = {};
        }
        if ("undefined" == typeof (extensionParams.url) || null == extensionParams.url) {
          extensionParams.url = config.documentserverUrl;
        }

        // load Onlyoffice API script
        // XXX need load API script to DOM head, Onlyoffice needs a real element in <script> to detect the DS server URL
        $("<script>").attr("type", "text/javascript").attr("src", config.documentserverJsUrl).appendTo("head");

        // and wait until it will be loaded
        function jsReady() {
          return (typeof DocsAPI !== "undefined") && (typeof DocsAPI.DocEditor !== "undefined");
        }

        var attempts = 40;
        function waitReady() {
          attempts--;
          if (attempts >= 0) {
            setTimeout(function() {
              if (jsReady()) {
                process.resolve(config);
              } else {
                waitReady();
              }
            }, 750);
          } else {
            log("ERROR: ONLYOFFICE script load timeout: " + config.documentserverJsUrl);
            process.reject("ONLYOFFICE script load timeout. Ensure Document Server is running and accessible.");
          }
        }

        if (jsReady()) {
          process.resolve(config);
        } else {
          waitReady();
        }
      } else {
        process.reject("Editor config not found");
      }
      ;
      return process.promise();
    };
    this.create = create;

    /**
     * Initialize an editor page in current browser window.
     */
    this.initEditor = function(config) {
      // TODO Establish a Comet/WebSocket channel from this point.
      // A new editor page will join the channel and notify when the doc will be saved
      // so we'll refresh this explorer view to reflect the edited content.
      UI.initEditor();
      create(config).done(function(localConfig) {
        $(function() {
          try {
            UI.create(localConfig);
          } catch (e) {
            log("Error initializing Onlyoffice client UI " + e, e);
          }
        });
      }).fail(function(error) {
        log("ERROR: editor config failed : " + error);
        UI.showError("Error", "Failed to setup editor configuration");
      });
    };

    this.showInfo = function(title, text) {
      UI.showInfo(title, text);
    };

    this.showError = function(title, text) {
      UI.showError(title, text);
    };

    // File activity in the activity stream
    this.initActivity = function(activityId, editorLink, editorLabel){
      UI.addEditorButtonToActivity(activityId, editorLink, editorLabel);
        
    };
    
    // File preview in the activity stream
    this.initPreview = function(activityId, editorLink, previewIndex, editorLabel) {
      UI.addEditorButtonToPreview(activityId, editorLink, previewIndex, editorLabel);
     
    };
    
    // File explorer
    this.initExplorer = function(cometdInfo) {
      var $JCRFileContent = $("#UIJCRExplorer .fileContent"); 
      if ($JCRFileContent.length > 0) {
        // Init redux store
        store = redux.createStore(docActionsReducer);
        store.subscribe(() => {
            var state = store.getState();
            if(state.status === 'DOCUMENT_SAVED'){
             if(state.userId === cometdInfo.user){
               UI.refreshExplorerPreview();
             }
             else {
               UI.addRefreshBannerExplorer();
             }
            }
         });
        
        UI.addEditButtonJCRExplorer();
       
        cCometD.configure({
          "url" : prefixUrl + cometdInfo.cometdPath,
          "exoId" : cometdInfo.user,
          "exoToken" : cometdInfo.userToken,
          "maxNetworkDelay" : 30000,
          "connectTimeout" : 60000
        });

        cometd = cCometD;
        cometdContext = {
          "exoContainerName" : cometdInfo.container
        };
        var subscription = cometd.subscribe("/eXo/Application/Onlyoffice/editor/" + cometdInfo.docId, function(message) {
          // Channel message handler
          var result = tryParseJson(message);
          log(result);
          if(result.eventType === 'document_saved'){
            store.dispatch(docSavedAction(result.user));
          }
          
        }, cometdContext, function(subscribeReply) {
          // Subscription status callback
          if (subscribeReply.successful) {
            // The server successfully subscribed this client to the channel.
            log("Document updates subscribed successfully: " + JSON.stringify(subscribeReply));
          } else {
            var err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason
                : "Undefined");
            log("Document updates subscription failed for " + cometdInfo.user, err);
          }
        });
      }
    };

   
  }

  /**
   * Online Editor WebUI integration.
   */
  function UI() {
    var NOTICE_WIDTH = "380px";

    var docEditor;

    var hasDocumentChanged = false;

    var self = this;
    
    var getEditorButton = function(editorLink, editorLabel) {
      return '<li><a href="' + editorLink + '" target="_blank"><i class="uiIconEcmsOnlyofficeOpen uiIconEcmsLightGray uiIconEdit"></i> '
          + editorLabel + '</a></li>';
    }
    
    var tryAddEditorButtonToPreview = function(editorLink, editorLabel, attempts, delay) {
      var $elem = $(".previewBtn");
      if ($elem.length == 0 || !$elem.is(":visible")) {
        if (attempts > 0) {
          setTimeout(function() {
            tryAddEditorButtonToPreview(editorLink, editorLabel, attempts - 1, delay);
          }, delay);

        } else {
          log("Cannot find element " + $elem);
        }
      } else {
        $(".previewBtn").append('<div class="onlyOfficeEditBtn">' + getEditorButton(editorLink, editorLabel) + '</div>');
      }
    };

    // TODO Use this in on-close window handler
    var saveAndDestroy = function() {
      if (docEditor) {
        try {
          docEditor.processSaveResult(true);
          docEditor.destroyEditor();
        } catch (e) {
          log("Error saving and destroying ONLYOFFICE editor", e);
        } finally {
          docEditor = null;
        }
      }
    };
    
    var refreshExplorerPreview = function(){
      var $banner = $(".document-preview-content-file #toolbarContainer .documentPreviewBanner");
      if($banner.length !== 0){
        $banner.remove();
      }
      var $vieverScript = $(".document-preview-content-file script[src$='/viewer.js']")
      var viewerSrc = $vieverScript.attr('src');
      $vieverScript.remove();
      $(".document-preview-content-file").append('<script src="' + viewerSrc + '"></script>');
    };

    this.refreshExplorerPreview = refreshExplorerPreview;

    /**
     * Init editor page UI.
     */
    this.initEditor = function() {
      $("#LeftNavigation").parent(".LeftNavigationTDContainer").remove();
      $("#NavigationPortlet").remove();
      // $("body").addClass("maskLayer");
      $("#SharedLayoutRightBody").addClass("onlyofficeEditorBody");
    };

    this.isEditorLoaded = function() {
      return $("#UIPage .onlyofficeContainer").length > 0;
    };

    this.closeEditor = function() {
      saveAndDestroy();
      // Use Close menu to reload the view with right representation
      $("#UIDocumentWorkspace .fileContent .onlyofficeContainer .loading .onClose").click();
    };

    this.disableMenu = function() {
      $("#uiActionsBarContainer i.uiIconEcmsOnlyofficeOpen").parent().addClass("editorDisabled").parent().removeAttr("onclick");
      $("#uiActionsBarContainer i.uiIconEcmsOnlyofficeClose").parent().addClass("editorDisabled").parent().removeAttr("onclick");
    };

    this.documentChanged = function(event) {
      // Used for UI messages.
      hasDocumentChanged = true;
    };

    /**
     * Create an editor client UI on current page.
     */
    this.create = function(localConfig) {
      var $editorPage = $("#OnlyofficeEditorPage");
      if ($editorPage.length > 0) {
        if (!docEditor) {
          // show loading while upload to editor - it is already added by WebUI side
          var $container = $editorPage.find(".onlyofficeContainer");

          // create and start editor (this also will re-use an existing editor config from the server)
          docEditor = new DocsAPI.DocEditor("onlyoffice", localConfig);
          hasDocumentChanged = false;
          // show editor
          $container.find(".editor").show("blind");
          $container.find(".loading").hide("blind");
        } else {
          log("WARN: Editor client already initialized");
        }
      } else {
        log("WARN: Editor element not found");
      }
    };

    /**
     * Show notice to user. Options support "icon" class, "hide", "closer" and "nonblock" features.
     */
    this.showNotice = function(type, title, text, options) {
      var noticeOptions = {
        title : title,
        text : text,
        type : type,
        icon : "picon " + (options ? options.icon : ""),
        hide : options && typeof options.hide != "undefined" ? options.hide : false,
        closer : options && typeof options.closer != "undefined" ? options.closer : true,
        sticker : false,
        opacity : .75,
        shadow : true,
        width : options && options.width ? options.width : NOTICE_WIDTH,
        nonblock : options && typeof options.nonblock != "undefined" ? options.nonblock : false,
        nonblock_opacity : .25,
        after_init : function(pnotify) {
          if (options && typeof options.onInit == "function") {
            options.onInit(pnotify);
          }
        }
      };

      return $.pnotify(noticeOptions);
    };

    /**
     * Show error notice to user. Error will stick until an user close it.
     */
    this.showError = function(title, text, onInit) {
      return UI.showNotice("error", title, text, {
        icon : "picon-dialog-error",
        hide : false,
        delay : 0,
        onInit : onInit
      });
    };

    /**
     * Show info notice to user. Info will be shown for 8sec and hidden then.
     */
    this.showInfo = function(title, text, onInit) {
      return UI.showNotice("info", title, text, {
        hide : true,
        delay : 8000,
        icon : "picon-dialog-information",
        onInit : onInit
      });
    };

    /**
     * Show warning notice to user. Info will be shown for 8sec and hidden then.
     */
    this.showWarn = function(title, text, onInit) {
      return UI.showNotice("exclamation", title, text, {
        hide : false,
        delay : 30000,
        icon : "picon-dialog-warning",
        onInit : onInit
      });
    };


    this.addRefreshBannerExplorer = function(){
      var $toolbarContainer = $(".document-preview-content-file #toolbarContainer");
      if($toolbarContainer.find('.documentPreviewBanner').length === 0){
        $toolbarContainer.append('<div class="documentPreviewBanner"><div class="previewBannerContent">The document has been updated. <span class="previewBannerLink">Update</span></div></div>');
        $(".documentPreviewBanner .previewBannerLink").on('click', function(){
          refreshExplorerPreview();
        });
      }
    };
    
    this.addEditButtonJCRExplorer = function(){
      $("#UIJCRExplorer .fileContent").closest("#UIJCRExplorer").find("#uiActionsBarContainer i.uiIconEcmsOnlyofficeOpen").addClass("uiIconEdit");         
    };
    
    this.addEditorButtonToActivity = function(activityId, editorLink, editorLabel){
      $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left").append(
          getEditorButton(editorLink, editorLabel));
    };
    
    this.addEditorButtonToPreview = function(activityId, editorLink, previewIndex, editorLabel){
      $("#Preview" + activityId + "-" + previewIndex).click(function() {
        // We set timeout here to avoid the case when the element is rendered but is going to be updated soon
        setTimeout(function() {
          tryAddEditorButtonToPreview(editorLink, editorLabel, 100, 100);
        }, 100);
      });
    };
    
  }

  var editor = new Editor();
  var UI = new UI();

  $(function() {
    try {
      // load required styles
      loadStyle("/onlyoffice/skin/jquery-ui.css");
      loadStyle("/onlyoffice/skin/jquery.pnotify.default.css");
      loadStyle("/onlyoffice/skin/jquery.pnotify.default.icons.css");
      loadStyle("/onlyoffice/skin/onlyoffice.css");

      // configure Pnotify
      $.pnotify.defaults.styling = "jqueryui";
      // use jQuery UI css
      $.pnotify.defaults.history = false;
      // no history roller in the right corner
    } catch (e) {
      log("Error configuring Onlyoffice Editor style.", e);
    }
  });

  return editor;
})($, cCometD, Redux);