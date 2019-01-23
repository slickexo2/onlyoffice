/**
 * Onlyoffice Editor client.
 */
(function($) {

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
  /**
   * @see http://stackoverflow.com/q/7616461/940217
   * @return {number}
   */
  if (!String.prototype.hashCode) {
    String.prototype.hashCode = function(){
      if (Array.prototype.reduce){
          return this.split("").reduce(function(a,b){a=((a<<5)-a)+b.charCodeAt(0);return a&a},0);              
      } 
      var hash = 0;
      if (this.length === 0) return hash;
      for (var i = 0; i < this.length; i++) {
          var character  = this.charCodeAt(i);
          hash  = ((hash<<5)-hash)+character;
          hash = hash & hash; // Convert to 32bit integer
      }
      return hash;
    }
  }
  
	// ******** Constants ********
	var ACCESS_DENIED = "access-denied";
	var NODE_NOT_FOUND = "node-not-found";

	// ******** Context ********
	// Node workspace and path currently open in ECMS explorer view
	var currentNode;

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

	/** Set cookie */
	var setCookie = function(name, value, millis, toDocument, toPath, toDomain) {
		var expires;
		if (millis) {
			var date = new Date();
			date.setTime(date.getTime() + millis);
			expires = "; expires=" + date.toGMTString();
		} else {
			expires = "";
		}
		( toDocument ? toDocument : document).cookie = name + "=" + encodeURIComponent(value) + expires + "; path=" + ( toPath ? toPath : "/") + ( toDomain ? "; domain=" + toDomain : "");
	};

	/** Read cookie */
	var getCookie = function(name, fromDocument) {
		var nameEQ = name + "=";
		var ca = ( fromDocument ? fromDocument : document).cookie.split(';');
		for (var i = 0; i < ca.length; i++) {
			var c = ca[i];
			while (c.charAt(0) == ' ') {
				c = c.substring(1, c.length);
			}
			if (c.indexOf(nameEQ) == 0) {
				var v = c.substring(nameEQ.length, c.length);
				// clean value from leading quotes (actual if set via eXo WS)
				return decodeURIComponent(v.match(/([^\"]+)/g));
			}
		}
		return null;
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

	var getIEVersion = function()
	// Returns the version of Windows Internet Explorer or a -1
	// (indicating the use of another browser).
	{
		var rv = -1;
		// Return value assumes failure.
		if (navigator.appName == "Microsoft Internet Explorer") {
			var ua = navigator.userAgent;
			var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
			if (re.exec(ua) != null)
				rv = parseFloat(RegExp.$1);
		}
		return rv;
	};

	var decodeString = function(str) {
		if (str) {
			try {
				str = str.replace(/\+/g, " ");
				str = decodeURIComponent(str);
				return str;
			} catch(e) {
				log("WARN: error decoding string " + str + ". " + e, e);
			}
		}
		return null;
	};

	var encodeString = function(str) {
		if (str) {
			try {
				str = encodeURIComponent(str);
				return str;
			} catch(e) {
				log("WARN: error decoding string " + str + ". " + e, e);
			}
		}
		return null;
	};

	/**
	 * Method adapted from org.exoplatform.services.cms.impl.Utils.refine().
	 */
	var refineSize = function(size) {
		if (!size || size == 0) {
			return "";
		}
		var strSize = size.toFixed(2);
		return "," + Math.round(parseInt(strSize) / 100.0);
	};

	/**
	 * Method adapted from org.exoplatform.services.cms.impl.Utils.fileSize().
	 */
	var sizeString = function(size) {
		var byteSize = size % 1024;
		var kbSize = (size % 1048576) / 1024;
		var mbSize = (size % 1073741824) / 1048576;
		var gbSize = size / 1073741824;

		if (gbSize >= 1) {
			return gbSize.toFixed(2) + " GB";
		} else if (mbSize >= 1) {
			return mbSize.toFixed(2) + " MB";
		} else if (kbSize > 1) {
			return kbSize.toFixed(2) + " KB";
		}
		if (byteSize > 0) {
			return byteSize + " B";
		} else {
			return "";
			// return empty not 1 KB as ECMS does
		}
	};

	// ******** UI utils **********

	/**
	 * Open pop-up.
	 */
	var popupWindow = function(url) {
		var w = 850;
		var h = 600;
		var left = (screen.width / 2) - (w / 2);
		var top = (screen.height / 2) - (h / 2);
		return window.open(url, 'contacts', 'width=' + w + ',height=' + h + ',top=' + top + ',left=' + left);
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
					if ( typeof data == "string") {
						// not JSON
						data = jqXHR.responseText;
					}
				} catch(e) {
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

	var configGet = function(workspace, path) {
		var request = $.ajax({
			type : "GET",
			url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + workspace + path,
			dataType : "json"
		});

		return initRequest(request);
	};
	
	var configPost = function(workspace, path) {
		var request = $.ajax({
			type : "POST",
			url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + workspace + path,
			dataType : "json"
		});

		return initRequest(request);
	};
	
	var configGetByKey = function(key) {
    var request = $.ajax({
       type : "GET",
       url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + key,
       dataType : "json"
    });
    return initRequest(request);
	};

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
	
	var editorClose = function(userId, fileKey) {
		var request = $.ajax({
			type : "POST",
			url : prefixUrl + "/portal/rest/onlyoffice/editor/ui/" + userId + "/" + fileKey,
			dataType : "json",
			data : {
				command : "close"
			}
		});

		return initRequest(request);
	};


	/**
	 * Editor core class.
	 */
	function Editor() {

		// Used by init() and open()/close()
		var loadingProcess;
		
		// Used by download() TODO deprecated
		var downloadProcess;

		// Used by download() and closeUI()
		var currentConfig;
		
		var onError = function(event) {
			log("ONLYOFFICE Document Editor reports an error: " + JSON.stringify(event));
			if (downloadProcess) {
				// TODO is it correct to reject download at this kind of error?
				downloadProcess.reject(event);
			}
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
		
		var waitClosed = function(config) {
			var process = $.Deferred();
			// wait a bit (up to ~60sec) to let Onlyoffice post document status
			var attempts = 20;
			function checkClosed() {
				attempts--;
				stateGet(config.editorConfig.user.id, config.document.key).done(function(state) {
					log("Editor state: " + JSON.stringify(state));
					if (state.saved || state.error) {
						process.resolve(state);
					} else {
						if (attempts >= 0 && (state.users.length == 0 || (state.users.length == 1 && state.users[0] == config.editorConfig.user.id))) {
							log("Continue to wait for editor closing...");
							setTimeout(checkClosed, 3000);
						} else {
							// resolve as-is, this will cover co-editing when others still edit
							process.resolve(state);
						}
					}
				}).fail(function(error) {
					log("Editor state error: " + JSON.stringify(error));
					process.reject(error);
				});
			}
			checkClosed();
			return process.promise();
		};

		/**
		 * Initialize context and UI on Document explorer pages.
		 */
		this.init = function(nodeWorkspace, nodePath) {
			// currently open node in ECMS explorer
			// FYI we don't touch currentConfig as may contain an editor opened on the page
			currentNode = {
				workspace : nodeWorkspace,
				path : nodePath
			};
			if (loadingProcess) { // TODO not required on a new page
				loadingProcess.reject("init");
			}
			loadingProcess = $.Deferred();
			$(function() {
				try {
					UI.init();
				} catch(e) {
					log("Error initializing Onlyoffice context: " + e, e);
				}
			});
		};
		
		/**
		 * Show existing editor.
		 * TODO deprecated
		 */
		this.show = function() {
			if (currentNode && UI.isEditorLoaded()) {			
				var loadingTimeout = setTimeout(function() {
					loadingProcess.resolve();				
				}, 750);
				loadingProcess.done(function() {
					var closeEditor = function() {
						setTimeout(function() {
							UI.closeEditor();
						}, 1500);
					};
					log("Loading without editor command.");
					// This logic should work only in case of showing editor w/o Open/Close command, thus in case of closing state.
					// Check if current document hasn't an editor already. Note that at this point we haven't called open() or close(). 
					// Thus if no config found or already closed then we do nothing,
					// if editor found in closing state, then keep showing loader (it's already) and wait for editor will gone
					// (status 404) or become closed (co-editing case) - then we need load docview instead of the editor,
					// if editor already open we call open() (this can be a case if page reloaded).
					configGet(currentNode.workspace, currentNode.path).done(function(config) {
						log("Found existing editor: " + JSON.stringify(config));
						if (config.closing) {
							log("Wait for closing editor...");
							waitClosed(config).done(function(state) {
								if (state.error) {
									log("ERROR: editor in erroneous state: " + state.error);
									UI.showError("Error closing editor", state.error);
								} else if (state.saved || state.users.length > 0) {
									closeEditor();
								}
							}).fail(function(error) {
								log("ERROR: getting editor state failed : " + error);
							});
						} else if (config.closed) {
							log("Found closed editor..."); 
							// we get the status to find number of users for co-editing case, in case of already closed this will reopen the editor
							waitClosed(config).done(function(state) {
								if (state.error) {
									log("ERROR: editor in erroneous state: " + state.error);
									UI.showError("Error editor", state.error);
								} else if (state.saved || state.users.length > 0) {
									log("Automatically re-open closed editor...");
									UI.open(config);
								}
							}).fail(function(error) {
								log("ERROR: getting editor state failed : " + error);
							});
						} else if (config.open) {
							log("Automatically open editor...");
							UI.open(config);
						}
					}).fail(function(state, status, errorText) {
						if (status == 404) {
							closeEditor();
						} else {
							log("ERROR: getting editor config failed : " + status + ". " + state.error);
						}
					}).always(function(res, status) {
						// finally do on-page-ready initialization of UI, disable editor menu if in saving or closed state
						if (status == 200 && res.closing) {
							UI.disableMenu();
						}
					});
				}).fail(function(command) {
					clearTimeout(loadingTimeout);
					log("Loaded editor for '" + command + "' command.");
				});
			} else {
				log("Editor UI not found or not initialized.");
			}
		};

		/**
		 * Create an editor configuration (for use to create the editor client UI).
		 */
		var create = function(config) {
			// TODO for debug only
			/*var testConfig_ = {
				"width" : "100%",
				"height" : "100%",
				"type" : "desktop",
				"documentType" : "text",
				"document" : {
					"title" : "Simple_Document.docx",
					"url" : "http://192.168.1.101:8080/onlysample/app_data/192.168.1.101/Simple_Document.docx",
					"fileType" : "docx",
					"key" : "-1598705153",
					"info" : {
						"author" : "Me",
						"created" : "02/25/2016"
					},
					"permissions" : {
						"edit" : true,
						"download" : true
					}
				},
				"editorConfig" : {
					"mode" : "edit",
					"lang" : "en",
					"callbackUrl" : "http://192.168.1.101:8080/onlysample/IndexServlet?type=track&userAddress=192.168.1.101&fileName=Simple_Document.docx",
					"user" : {
						"id" : "192.168.1.101",
						"firstname" : "John",
						"lastname" : "Smith"
					},
					"embedded" : {
						"saveUrl" : "http://192.168.1.101:8080/onlysample/app_data/192.168.1.101/Simple_Document.docx",
						"embedUrl" : "http://192.168.1.101:8080/onlysample/app_data/192.168.1.101/Simple_Document.docx",
						"shareUrl" : "http://192.168.1.101:8080/onlysample/app_data/192.168.1.101/Simple_Document.docx",
						"toolbarDocked" : "top"
					},
					"customization" : {
						"about" : true,
						"feedback" : true,
						"goback" : {
							"url" : "http://192.168.1.101:8080/onlysample/IndexServlet"
						}
					}
				},
				"events" : {}
			};*/

			var process = $.Deferred();
			if (currentNode) {
				// request config
				var configReady = $.Deferred();
				if (config) {
					configReady.resolve(config);
				} else {
					var attempts = 10;
					function createConfig() {
						attempts--;
						if (attempts >= 0) {
							configPost(currentNode.workspace, currentNode.path).done(function(config) {
								if (config.closing) {
								  // Wait for previous edit session completion (actual if editor was closed and we need wait for 
			            // the DS state in seconds).
			            // We need wait for downloading of the previous edition and only then use it for a new editor session.
									setTimeout(createConfig, 2250);
								} else {
									configReady.resolve(config);							
								}
							}).fail(function(state, status, errorText) {
								log("ERROR: editor config request failed : " + status + ". " + state.error);
								configReady.reject(state.error);
							});
						} else {
							log("ERROR: ONLYOFFICE configuration load timeout");
							process.reject("ONLYOFFICE configuration load timeout. Reload the page and try again please.");
						}
					}
					createConfig();
				}
				
				configReady.done(function(config) {
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

					currentConfig = config;

					// XXX need let Onlyoffice to know about a host of API end-points,
					// as we will add their script dynamically to the DOM, the script will not be able to detect an URL
					// thus we use "extensionParams" observed in the script code
					if ("undefined" == typeof (extensionParams) || null == extensionParams) {
						extensionParams = {};
					}
					if ("undefined" == typeof (extensionParams.url) || null == extensionParams.url) {
						extensionParams.url = config.documentserverUrl;
					}

					// create new deferred for future download
					downloadProcess = $.Deferred();

					// load Onlyoffice API script
					// XXX need load API script to DOM head, Onlyoffice needs a real element in <script> to detect the DS server URL
					$("<script>").attr("type", "text/javascript").attr("src", config.documentserverJsUrl).appendTo("head");

					// and wait until it will be loaded
					function jsReady() {
						return ( typeof DocsAPI !== "undefined") && ( typeof DocsAPI.DocEditor !== "undefined");
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
				}).fail(function(error) {
					process.reject(error);
				});
			} else {
				log("ERROR: current node not initialized");
				process.reject("Current document not initialized");
			}
			return process.promise();
		};
		this.create = create;

		/**
		 * TODO deprecated
		 */
		this.download = function() {
			if (downloadProcess) {
				if (currentConfig && downloadProcess.state() === "pending") {
					waitClosed(currentConfig).done(function(state) {
						downloadProcess.resolve(state);
					}).fail(function(error) {
						downloadProcess.reject(error);
					});
				}
				return downloadProcess.promise();
			} else {
				var rejected = $.Deferred();
				rejected.reject("Editor was not created");
				return rejected.promise();
			}
		};

		/**
		 * Open Editor on current page using pre-initialized state (see init()).
		 * TODO deprecated
		 */
		this.open = function() {
			if (currentNode) {
				if (loadingProcess) {
					loadingProcess.reject("open");					
				}
				UI.open();
			} else {
				log("Current node not set");
			}
		};

		/**
		 * Close Editor on current page using pre-initialized state (see init() and open()).
		 * TODO deprecated
		 */
		this.close = function() {
			if (currentNode) {
				if (loadingProcess) {
					loadingProcess.reject("close");					
				}
				UI.close();
			} else {
				log("Current node not set");
			}
		};
		
		/**
     * Initialize current node for use within an editor.
     */
    this.initDocument = function() {
      if (currentNode) {
        return documentPost(currentNode.workspace, currentNode.path);
      } else {
        log("ERROR: Current node not set");
        var process = $.Deferred();
        process.reject("Document not initialized. Reload page and try again.");
        return process.promise();
      }
    };
    
    /**
     * Initialize an editor page in new browser window.
     */
    this.initEditor = function(config) {
      // TODO Establish a Comet/WebSocket channel from this point.
      // A new editor page will join the channel and notify when the doc will be saved
      // so we'll refresh this explorer view to reflect the edited content.
      currentNode = {
        workspace : config.workspace,
        path : config.path
      };
      UI.initEditor();
      create(config).done(function(localConfig) {
        $(function() {
          try {
            UI.create(localConfig);
          } catch(e) {
            log("Error initializing Onlyoffice client UI " + e, e);
          }
        });        
      }).fail(function(state, status, errorText) {
        log("ERROR: editor config failed : " + status + ". " + state.error);
        UI.showError("Error", "Failed to setup editor configuration");
      });
    };
    
    /**
     * Close WebUI editor (for proper menu state, when user clicked non-Onlyoffice item).
     * TODO Deprecated
     */
    this.closeUI = function() {
    	if (currentConfig) {
    		editorClose(currentConfig.editorConfig.user.id, currentConfig.document.key).done(function(res) {
    			if (res.closing) {
    				log("Editor UI closed successfully");
    			} 
    		}).fail(function(error) {
    			log("Error closing editor UI: " + JSON.stringify(error));
    		});
    	} else {
    		log("Current config not set to close UI");
    	}
    };

		this.showInfo = function(title, text) {
			UI.showInfo(title, text);
		};
		
		this.showError = function(title, text) {
      UI.showError(title, text);
    };
	}

	/**
	 * Online Editor WebUI integration.
	 */
	function UI() {
		var NOTICE_WIDTH = "380px";

		var docEditor;

		var hasDocumentChanged = false;
		
		/*
		var editorWindows = {};
		
    var openEditorWindow = function(name) {
      log(">> openEditorWindow: " + name);
      var w = editorWindows[name];
      if (!w || w.closed) {
        w = window.open("", name);
        editorWindows[name] = w;
      }
      w.focus();
    };
		
    var initEditorWindow = function(name, link) {
      log(">> initEditorWindow: " + name + " " + link);
      var w = editorWindows[name];
      if (w) {
        //delete editorWindows[name];        
        if (!w.location.href.endsWith(link)) {
          log("<< initEditorWindow: w.location = " + link);
          w.location.href = link;            
        } else {
          log("<< initEditorWindow: already " + link);
        }
        w.focus(); // this may not work for a tab
      }
    };
    */
		
		var contextId = function(currentNode) {
		  return "oeditor" + (currentNode.workspace + currentNode.path).hashCode();
		}
		
		var initAction = function() {
			var init = false;
			var $actionOpenIcon = $("#uiActionsBarContainer i.uiIconEcmsOnlyofficeOpen");
			if ($actionOpenIcon.length > 0 && !$actionOpenIcon.hasClass("uiIconEdit")) {
				$actionOpenIcon.addClass("uiIconEdit");
				// XXX get rid of WebUI action ajaxGet() which will update interaction and will not let to open 
				// the editor page next time on a new page (it will load current Document explorer instead).
				$actionOpenIcon.parent().parent().removeAttr("onclick");
				// Do own click handler to open a new page with the editor
				$actionOpenIcon.parent().click(function() {
				  if (currentNode) {
				    var wId = contextId(currentNode);
				    var w = window.open("", wId);
				    editor.initDocument().done(function(info) {
				      if (!w.location.href.endsWith(info.editorUrl)) {
			          w.location = info.editorUrl;        
			        }
			        w.focus();
				    }).fail(function(state, status) {
				      w.close();
              log("Error initializing document: " + state.error + " [" + status + "]");
              UI.showError("Error", "Failed to initialize document");
            });
				  } else {
				    log("Current node not set. Cannot open an editor page.");
				  }
				});
			}
			/*
			var $actionCloseIcon = $("#uiActionsBarContainer i.uiIconEcmsOnlyofficeClose");
			if ($actionCloseIcon.length > 0 && !$actionCloseIcon.hasClass("uiIconSave")) { // was uiIconClose
				$actionCloseIcon.addClass("uiIconSave");
				init = true; // Jan 18, 2019 - should not happen
			}
			
			// TODO not required
			// May 25, 2018: if click Version or Edit Properties, we need close the editor at WebUI side
			if (init) {
				var $implicitCloseActions = $("#uiActionsBarContainer").find("i.uiIconEcmsManageVersions, i.uiIconEcmsEditProperty").parent("a.actionIcon").parent("li");
				$implicitCloseActions.click(function() {
					var showLeavedInfo = hasDocumentChanged;
					try {
						saveAndDestroy();
						editor.closeUI(); // TODO don't need it 
					} finally {
						if (showLeavedInfo) {
							UI.showInfo("You leaved document editor", "Document will be saved when all users close it.");
						}
					}
				});				
			}
			*/
		};
		
		var saveAndDestroy = function() {
			if (docEditor) {
				try {
					docEditor.processSaveResult(true);
					docEditor.destroyEditor();
				} catch(e) {
					log("Error saving and destroying ONLYOFFICE editor", e);
				} finally {
					docEditor = null;					
				}				
			}
		};

		/**
		 * Init all UI (dialogs, menus, views etc).
		 */
		this.init = function() {
			// init action bar menu
			initAction();
		};
		
		/**
		 * Init editor page UI.
		 */
		this.initEditor = function() {
		  $("#LeftNavigation").parent(".LeftNavigationTDContainer").remove();
		  $("#NavigationPortlet").remove();
		  $("#SharedLayoutRightBody .RightBodyTDContainer").css("padding-top", "0px");
		  $("#UIWorkingWorkspace").css("height", "100%");
		  $("#RightBody").css("min-height", "100vh");
		  $(".onlyofficeContainer > #editor, .onlyofficeContainer > .editor").css("height", "100vh");
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
		 * TODO deprecated
		 */
		this.open = function(existingConfig) {
			var $fileContent = $("#UIDocumentWorkspace .fileContent");
			if ($fileContent.length > 0 && !docEditor) {
				var $title = $fileContent.find(".title");
				if ($title.length > 0) {
					// TODO add full-screen button to the title
				}

				// show loading while upload to editor - it is already added by WebUI side
				var $container = $fileContent.find(".onlyofficeContainer");
				
				// create and start editor
				editor.create(existingConfig).done(function(localConfig) {
					docEditor = new DocsAPI.DocEditor("onlyoffice", localConfig);
					hasDocumentChanged = false;
					// show editor
					var $editor = $container.find(".editor");
					var $loading = $container.find(".loading");
					$loading.hide("blind");
					$editor.show("blind");
				}).fail(function(error) {
					UI.showError("Error creating editor", error);
					$container.find(".loading>.onError").click();
				});
			}
		};

		/**
     * TODO deprecated
     */
		this.close = function() {
			var $fileContent = $("#UIDocumentWorkspace .fileContent");
			if ($fileContent.length > 0 && docEditor) {
				// show loading while download the document - it is already added by WebUI side
				var $container = $fileContent.find(".onlyofficeContainer");

				saveAndDestroy();
				
				var $editor = $container.find(".editor");
				var $loading = $container.find(".loading");
				// remove Onlyoffice iframe - this will let Onlyoffice to know the editing is done
				$editor.empty();
				// hide/remove editor
				$editor.hide("blind");
				$loading.show("blind");

				// save the doc and switch to viewer
				editor.download().done(function(state) {
					if (state.error) {
						UI.showError("Document not saved", state.error);
						// need show editor back: we need add part of DOM here as Onlyoffice JS clean it when creating the editor
						$editor.append($("<div id='onlyoffice'></div>"));
						editor.create().done(function(config) {
							docEditor = new DocsAPI.DocEditor("onlyoffice", config);
							hasDocumentChanged = false;
							$loading.hide("blind", {
								"direction" : "down"
							});
							$editor.show("blind", {
								"direction" : "down"
							});
						}).fail(function(error) {
							UI.showError("Error creating editor", error);
							$loading.find(".onError").click();
						});
					} else {
						if (!state.saved) {
							if (state.users.length > 0) {
								UI.showInfo("Document in use by others", "Document will be saved when all users close it.");
							}
						}
						// this will refresh the file view and action bar
						$loading.find(".onClose").click();
					}
				}).fail(function(error) {
					log(JSON.stringify(error));
					UI.showError("Download error", error.data.errorDescription);
				});
			}
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
          //editor.create(config).done(function(localConfig) {
          docEditor = new DocsAPI.DocEditor("onlyoffice", localConfig);
          hasDocumentChanged = false;
          // show editor
          $container.find(".editor").show("blind");
          $container.find(".loading").hide("blind");
          //}).fail(function(error) {
          //  UI.showError("Error creating editor", error);
          //});
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
				icon : "picon " + ( options ? options.icon : ""),
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
		} catch(e) {
			log("Error configuring Onlyoffice Editor style.", e);
		}
	});

	return editor;
})($);

