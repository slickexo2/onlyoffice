/**
 * Onlyoffice Editor client.
 */
(function($) {

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
			if ($("head").find("link[href='" + cssUrl + "']").size() == 0) {
				var headElems = document.getElementsByTagName("head");
				var style = document.createElement("link");
				style.type = "text/css";
				style.rel = "stylesheet";
				style.href = cssUrl;
				headElems[headElems.length - 1].appendChild(style);
				// $("head").append($("<link href='" + cssUrl + "' rel='stylesheet' type='text/css' />"));
			} // else, already added
		}
	};

	/** For debug logging. */
	var log = function(msg, e) {
		if ( typeof console != "undefined" && typeof console.log != "undefined") {
			console.log(msg);
			if (e && typeof e.stack != "undefined") {
				console.log(e.stack);
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

	/**
	 * NOT USED
	 */
	var statusPost = function(key, status) {
		var request = $.ajax({
			type : "POST",
			url : prefixUrl + "/portal/rest/onlyoffice/editor/status/" + key,
			dataType : "json",
			data : status,
			xhrFields : {
				withCredentials : true
			}
		});

		return initRequest(request);
	};

	var configGet = function(workspace, path) {
		var request = $.ajax({
			type : "GET",
			url : prefixUrl + "/portal/rest/onlyoffice/editor/config/" + workspace + path,
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

		var downloadProcess;

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
			} else {
				log("ONLYOFFICE Changes are collected on document editing service");
			}
		};

		/**
		 * Initialize context and UI.
		 */
		this.init = function(nodeWorkspace, nodePath) {

			// currently open node in ECMS explorer
			currentNode = {
				workspace : nodeWorkspace,
				path : nodePath
			};

			// and on-page-ready initialization of UI
			$(function() {
				try {
					UI.init();
				} catch(e) {
					log("Error initializing Onlyoffice Editor UI " + e, e);
				}
			});
		};

		this.create = function() {
			// TODO not used
			/*var sampleConfig_ = {
				width : "100%",
				height : "100%",
				type : "desktop",
				documentType : "text",
				document : {
					title : "fileName",
					url : "http://192.168.99.100/ResourceService.ashx?path=temp_531919286%2f531919286.tmp&nocache=true&deletepath=temp_531919286&filename=531919286.tmp",
					fileType : "fileType",
					key : "-1598705153",
					info : {
						author : "Me",
						created : "02/22/2016",
					},
					permissions : {
						edit : true,
						download : true,
					}
				},
				editorConfig : {
					mode : "edit",
					lang : "en",
					callbackUrl : "http://192.168.1.101:8080/onlysample/IndexServlet?type=track&userAddress=192.168.1.101&fileName=Simple_Document.docx",
					user : {
						id : "192.168.1.101",
						firstname : "John",
						lastname : "Smith",
					},
					embedded : {
						saveUrl : "http://192.168.99.100/ResourceService.ashx?path=temp_531919286%2f531919286.tmp&nocache=true&deletepath=temp_531919286&filename=531919286.tmp",
						embedUrl : "http://192.168.99.100/ResourceService.ashx?path=temp_531919286%2f531919286.tmp&nocache=true&deletepath=temp_531919286&filename=531919286.tmp",
						shareUrl : "http://192.168.99.100/ResourceService.ashx?path=temp_531919286%2f531919286.tmp&nocache=true&deletepath=temp_531919286&filename=531919286.tmp",
						toolbarDocked : "top",
					},
					customization : {
						about : true,
						feedback : true,
						goback : {
							url : "http://192.168.1.101:8080/IndexServlet",
						},
					},
				},
				events : {
					"onReady" : onReady,
					"onDocumentStateChange" : onDocumentStateChange,
					"onError" : onError
				}
			};*/

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

			currentConfig = null;
			var process = $.Deferred();
			if (currentNode) {
				if (currentConfig) {
					// create new deferred for future download
					downloadProcess = $.Deferred();
					process.resolve(currentConfig);
				} else {
					// request config
					var get = configGet(currentNode.workspace, currentNode.path);
					get.done(function(config) {
						log("Editor config successfully requested.");
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

						log("currentConfig = " + JSON.stringify(currentConfig));

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
								process.reject("Script load timeout " + config.documentserverJsUrl);
							}
						}

						if (jsReady()) {
							process.resolve(config);
						} else {
							waitReady();
						}
					});
					get.fail(function(state, status, errorText) {
						log("ERROR: config request failed : " + status + ". " + state.error);
						process.reject(state);
					});
				}
			} else {
				process.reject("Current node not initialized");
			}
			return process.promise();
		};

		this.download = function() {
			if (downloadProcess) {
				if (currentConfig && downloadProcess.state() === "pending") {
					// wait a bit (up to ~60sec) to let Onlyoffice post document status
					var attempts = 40;
					function checkSaved() {
						attempts--;
						var get = stateGet(currentConfig.editorConfig.user.id, currentConfig.document.key);
						get.done(function(state) {
							if (state.saved || state.error) {
								downloadProcess.resolve(state);
							} else {
								if (attempts >= 0 && state.users && state.users.length == 1 && state.users[0] == currentConfig.editorConfig.user.id) {
									setTimeout(function() {
										checkSaved();
									}, 1500);
								} else {
									// resolve as-is, this will cover co-editing when others still edit
									downloadProcess.resolve(state);
								}
							}
						});
						get.fail(function(error) {
							downloadProcess.reject(error);
						});
					}

					checkSaved();
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
		 */
		this.open = function() {
			if (currentNode) {
				UI.open();
			} else {
				log("Current node not set");
			}
		};

		/**
		 * Close Editor on current page using pre-initialized state (see init() and open()).
		 */
		this.close = function() {
			if (currentNode) {
				UI.close();
			} else {
				log("Current node not set");
			}
		};

		this.showInfo = function(title, text) {
			UI.showInfo(title, text);
		};
	}

	/**
	 * Online Editor WebUI integration.
	 */
	function UI() {
		var NOTICE_WIDTH = "380px";

		var docEditor;

		var refresh = function() {
			// refresh view w/o popup
			$("#ECMContextMenu a[exo\\:attr='RefreshView'] i").click();
		};

		/**
		 * TODO Deprecated. Not used. 
		 */
		var initDocument = function() {
			var $toolbarViewer = $("#UIDocumentWorkspace #toolbarViewerRight");
			if ($toolbarViewer.size() > 0) {
				var $editorAction = $toolbarViewer.find("#onlyofficeEditor");
				if ($editorAction.size() == 0) {
					$editorAction = $("<a id='onlyofficeEditor' class='actionIcon' data-placement='bottom' rel='tooltip' data-original-title='Edit' tabindex='11' data-l10n-id='edit'><i class='uiIconLightGray'></i></a>");
					$toolbarViewer.prepend($editorAction);
					var $icon = $editorAction.find("i");

					$editorAction.data("original-title", "Edit");
					$editorAction.data("l10n-id", "edit");
					$icon.addClass("uiIconEdit");

					var $container = $("#viewerContainer");
					var $viewer = $container.find("#viewer");

					function showProgress() {
						var $progress = $container.find("#progress");
						if ($progress.size() == 0) {
							// uiLoadingIconXLarge
							$progress = $("<div id='progress'><div class='waitThrobber'></div></div>");
							$container.append($progress);
						}

						$editorAction.attr("disabled", true);

						$container.find("#editor").remove();
						$viewer.hide("blind");
						$progress.show("blind");
					}

					function showEditor(config) {
						var $editor = $container.find("#editor");
						if ($editor.size() == 0) {
							// this will load Onlyoffice JS!
							$editor = $("<div id='editor'><div id='onlyoffice'></div></div>");
							$container.append($editor);
						}

						$editorAction.attr("disabled", false);
						$editorAction.data("original-title", "Save");
						$editorAction.data("l10n-id", "save");
						$icon.removeClass("uiIconEdit");
						$icon.addClass("uiIconSave");

						$container.find("#progress").hide("blind");
						$viewer.hide("blind");
						$editor.show("blind");
					}

					function showViewer() {
						$editorAction.attr("disabled", false);
						$editorAction.data("original-title", "Edit");
						$editorAction.data("l10n-id", "edit");
						$icon.removeClass("uiIconSave");
						$icon.addClass("uiIconEdit");

						$container.find("#progress").hide("blind");
						$container.find("#progress").remove();
						$container.find("#editor").remove();
						$viewer.show("blind");

						// refresh view w/o popup
						$("#ECMContextMenu a[exo\\:attr='RefreshView'] i").click();
					}


					$editorAction.click(function() {
						if ($icon.hasClass("uiIconEdit")) {
							// show loading while upload to editor
							showProgress();

							// create and start editor
							var create = editor.create();
							create.done(function(config) {
								// XXX timeout mandatory to let DocsAPI load successfully
								setTimeout(function() {
									$(function() {
										docEditor = new DocsAPI.DocEditor("onlyoffice", config);
									});
								}, 2000);
								// show editor
								showEditor(config);
							});
							create.fail(function(error) {
								log("ERROR: " + JSON.stringify(error));
								UI.showError("Error creating editor", error.error);
								showViewer();
							});
						} else {
							// show loading while downloading from editor
							showProgress();

							// TODO seems this not required
							docEditor.processSaveResult(true);

							// save the doc and switch to viewer
							var download = editor.download();
							download.done(function(state) {
								showViewer();
								if (!state.saved) {
									if (state.users && state.users.length > 1) {
										UI.showInfo("Document in use by others", "Document will be saved when all users will close it.");
									}
								}
							});
							download.fail(function(error) {
								log(JSON.stringify(error));
								UI.showError("Download error", error.data.errorDescription);
							});
						}
					});
				}
			}
		};

		var initAction = function() {
			var $actionOpenIcon = $("#uiActionsBarContainer i.uiIconEcmsOnlyofficeOpen");
			if (!$actionOpenIcon.hasClass("uiIconEdit")) {
				$actionOpenIcon.addClass("uiIconEdit");
			}

			var $actionCloseIcon = $("#uiActionsBarContainer i.uiIconEcmsOnlyofficeClose");
			if (!$actionCloseIcon.hasClass("uiIconClose")) {
				$actionCloseIcon.addClass("uiIconSave");
				// uiIconClose
			}

			// var $fileContent = $("#UIDocumentWorkspace .fileContent");
			// var $title = $fileContent.find(".title");
			// var $content = $fileContent.find(".content");
			// var $editor = $fileContent.find(".editor");
			// var $progress = $fileContent.find(".progress");
			//
			// function showProgress() {
			// //var $progress = $fileContent.find(".progress");
			// if ($progress.size() == 0) {
			// // uiLoadingIconXLarge
			// $progress = $("<div id='progress'><div class='waitThrobber'></div></div>");
			// $fileContent.append($progress);
			// }
			//
			// // need remove to let Onlyoffice see end of work
			// $fileContent.find(".editor").remove();
			// $content.hide("blind");
			// $progress.show("blind");
			// }
			//
			// function showEditor(config) {
			// //var $editor = $container.find(".editor");
			// if ($editor.size() == 0) {
			// // this will load Onlyoffice JS!
			// $editor = $("<div id='editor'><div id='onlyoffice'></div></div>");
			// $fileContent.append($editor);
			// }
			//
			// $fileContent.find(".progress").hide("blind");
			// $editor.show("blind");
			// }
			//
			// if ($actionOpenIcon.size() > 0) {
			// // at this point we have document in viewer
			// var $action = $actionOpenIcon.parent().parent();
			// //$action.removeAttr("onclick");
			//
			// if ($fileContent.size() > 0) {
			// if ($title.size() > 0) {
			// // TODO add full-screen button to the title
			// }
			//
			// $action.click(function() {
			// // show loading while upload to editor
			// showProgress();
			//
			// // create and start editor
			// var create = editor.create();
			// create.done(function(config) {
			// // XXX timeout mandatory to let DocsAPI load successfully
			// setTimeout(function() {
			// $(function() {
			// docEditor = new DocsAPI.DocEditor("onlyoffice", config);
			// });
			// }, 2000);
			// // show editor
			// showEditor(config);
			// });
			// create.fail(function(error) {
			// log("ERROR: " + JSON.stringify(error));
			// UI.showError("Error creating editor", error.error);
			// showViewer();
			// });
			// });
			// }
			// } else {
			// $actionOpenIcon.prop("disabled", true);
			// }
			//
			// if ($actionCloseIcon.size() > 0) {
			// var $action = $actionCloseIcon.parent().parent();
			// //var onclickScript = $action.attr("onclick");
			// var $origAction = $action.clone(true);
			// $origAction.empty();
			// $action.removeAttr("onclick");
			//
			// if ($fileContent.size() > 0) {
			// // show loading while downloading from editor
			// showProgress();
			//
			// // TODO seems this not required
			// if (docEditor) {
			// docEditor.processSaveResult(true);
			// docEditor = null;
			// }
			//
			// // save the doc and switch to viewer
			// var download = editor.download();
			// download.done(function(state) {
			// if (!state.saved) {
			// if (state.users && state.users.length > 1) {
			// UI.showInfo("Document in use by others", "Document will be saved when all users will close it.");
			// }
			// }
			// $origAction.click();
			// // invoke original (WebUI's) action
			// });
			// download.fail(function(error) {
			// log(JSON.stringify(error));
			// UI.showError("Download error", error.data.errorDescription);
			// });
			// }
			// } else {
			// $actionCloseIcon.prop("disabled", true);
			// }
		};

		/**
		 * Init all UI (dialogs, menus, views etc).
		 */
		this.init = function() {
			// init doc view
			//initDocument();
			// init action bar menu
			initAction();
		};

		this.open = function() {
			var $fileContent = $("#UIDocumentWorkspace .fileContent");
			if ($fileContent.size() > 0) {
				var $title = $fileContent.find(".title");
				if ($title.size() > 0) {
					// TODO add full-screen button to the title
				}

				// show loading while upload to editor - it is already by WebUI side
				// showProgress();

				// create and start editor
				var create = editor.create();
				create.done(function(config) {
					docEditor = new DocsAPI.DocEditor("onlyoffice", config);
					// show editor
					var $editor = $fileContent.find(".editor");
					var $loading = $fileContent.find(".loading");
					$loading.hide("blind");
					$editor.show("blind");
				});
				create.fail(function(error) {
					log("ERROR: " + JSON.stringify(error));
					UI.showError("Error creating editor", error.error);
					$fileContent.find(".loading>.onError").click();
				});
			}
		};

		this.close = function() {
			var $fileContent = $("#UIDocumentWorkspace .fileContent");
			if ($fileContent.size() > 0) {
				// show loading while download the document - it is already by WebUI side
				// showProgress();

				// TODO seems this not required
				if (docEditor) {
					docEditor.processSaveResult(true);
					docEditor = null;
				}

				var $editor = $fileContent.find(".editor");
				var $loading = $fileContent.find(".loading");
				// remove Onlyoffice iframe - this will let Onlyoffice to know the editing is done
				$editor.empty();
				// hide/remove editor
				$editor.hide("blind");
				$loading.show("blind");

				// save the doc and switch to viewer
				var download = editor.download();
				download.done(function(state) {
					if (state.error) {
						UI.showError("Document not saved", state.error);
						// need show editor back: we need add part of DOM here as Onlyoffice JS clean it when creating the editor
						$editor.append($("<div id='onlyoffice'></div>"));
						var create = editor.create();
						create.done(function(config) {
							docEditor = new DocsAPI.DocEditor("onlyoffice", config);
							$loading.hide("blind", {
								"direction" : "down"
							});
							$editor.show("blind", {
								"direction" : "down"
							});
						});
						create.fail(function(error) {
							log("ERROR: " + JSON.stringify(error));
							UI.showError("Error creating editor", error.error);
							$loading.find(".onError").click();
						});
					} else {
						if (!state.saved) {
							if (state.users && state.users.length > 0) {
								UI.showInfo("Document in use by others", "Document will be saved when all users will close it.");
							}
						}
						// this will refresh the file view and action bar
						$loading.find(".onClose").click();
					}
				});
				download.fail(function(error) {
					log(JSON.stringify(error));
					UI.showError("Download error", error.data.errorDescription);
				});
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

	// Load onlyoffice dependencies only in top window (not in iframes of gadgets).
	if (window == top) {
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
				// no history roller in the
				// right corner
			} catch(e) {
				log("Error configuring Onlyoffice Editor style.", e);
			}
		});
	}

	return editor;
})($);

