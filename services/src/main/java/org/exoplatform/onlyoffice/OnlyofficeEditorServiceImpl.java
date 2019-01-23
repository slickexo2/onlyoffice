/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.onlyoffice;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.onlyoffice.jcr.NodeFinder;
import org.exoplatform.portal.Constants;
import org.exoplatform.services.cache.CacheListener;
import org.exoplatform.services.cache.CacheListenerContext;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.service.LinkProvider;

/**
 * Service implementing {@link OnlyofficeEditorService} and {@link Startable}.
 * This component handles interactions with Onlyoffice Document Server and
 * related eXo user states.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorServiceImpl.java 00000 Jan 31, 2016 pnedonosko
 *          $
 */
public class OnlyofficeEditorServiceImpl implements OnlyofficeEditorService, Startable {

  /** The Constant LOG. */
  protected static final Log                                      LOG                    =
                                                                      ExoLogger.getLogger(OnlyofficeEditorServiceImpl.class);

  /** The Constant RANDOM. */
  protected static final Random                                   RANDOM                 = new Random();

  /** The Constant CONFIG_DS_HOST. */
  public static final String                                      CONFIG_DS_HOST         = "documentserver-host";

  /** The Constant CONFIG_DS_SCHEMA. */
  public static final String                                      CONFIG_DS_SCHEMA       = "documentserver-schema";

  /** The Constant CONFIG_DS_ACCESS_ONLY. */
  public static final String                                      CONFIG_DS_ACCESS_ONLY  = "documentserver-access-only";

  /**
   * Configuration key for Document Server's allowed hosts in requests from a DS
   * to eXo side.
   */
  public static final String                                      CONFIG_DS_ALLOWEDHOSTS = "documentserver-allowedhosts";

  /** The Constant HTTP_PORT_DELIMITER. */
  protected static final char                                     HTTP_PORT_DELIMITER    = ':';

  /** The Constant TYPE_TEXT. */
  protected static final String                                   TYPE_TEXT              = "text";

  /** The Constant TYPE_SPREADSHEET. */
  protected static final String                                   TYPE_SPREADSHEET       = "spreadsheet";

  /** The Constant TYPE_PRESENTATION. */
  protected static final String                                   TYPE_PRESENTATION      = "presentation";

  /** The Constant LOCK_WAIT_ATTEMTS. */
  protected static final int                                      LOCK_WAIT_ATTEMTS      = 20;

  /** The Constant LOCK_WAIT_TIMEOUT. */
  protected static final long                                     LOCK_WAIT_TIMEOUT      = 250;

  /** The Constant EMPTY_TEXT. */
  protected static final String                                   EMPTY_TEXT             = "".intern();

  /** The Constant CACHE_NAME. */
  public static final String                                      CACHE_NAME             = "onlyoffice.EditorCache".intern();

  /** The jcr service. */
  protected final RepositoryService                               jcrService;

  /** The session providers. */
  protected final SessionProviderService                          sessionProviders;

  /** The identity registry. */
  protected final IdentityRegistry                                identityRegistry;

  /** The finder. */
  protected final NodeFinder                                      finder;

  /** The organization. */
  protected final OrganizationService                             organization;

  /** The authenticator. */
  protected final Authenticator                                   authenticator;

  /** Cache of Editing documents. */
  protected final ExoCache<String, ConcurrentMap<String, Config>> activeCache;

  /** Lock for updating Editing documents cache. */
  protected final ReentrantLock                                   activeLock             = new ReentrantLock();

  /** The config. */
  protected final Map<String, String>                             config;

  /** The upload url. */
  protected final String                                          uploadUrl;

  /** The documentserver host name. */
  protected final String                                          documentserverHostName;

  /** The documentserver url. */
  protected final String                                          documentserverUrl;

  /** The documentserver access only. */
  protected final boolean                                         documentserverAccessOnly;

  /** The documentserver allowed hosts (can be empty if not configured). */
  protected final Set<String>                                     documentserverAllowedhosts;

  /** The file types. */
  protected final Map<String, String>                             fileTypes              =
                                                                            new ConcurrentHashMap<String, String>();

  /** The upload params. */
  protected final MessageFormat                                   uploadParams           =
                                                                               new MessageFormat("?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}");

  /** The listeners. */
  protected final ConcurrentLinkedQueue<OnlyofficeEditorListener> listeners              =
                                                                            new ConcurrentLinkedQueue<OnlyofficeEditorListener>();

  /**
   * Cloud Drive service with storage in JCR and with managed features.
   *
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   * @param identityRegistry the identity registry
   * @param finder the finder
   * @param organization the organization
   * @param authenticator the authenticator
   * @param cacheService the cache service
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  public OnlyofficeEditorServiceImpl(RepositoryService jcrService,
                                     SessionProviderService sessionProviders,
                                     IdentityRegistry identityRegistry,
                                     NodeFinder finder,
                                     OrganizationService organization,
                                     Authenticator authenticator,
                                     CacheService cacheService,
                                     InitParams params)
      throws ConfigurationException {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.finder = finder;
    this.organization = organization;
    this.authenticator = authenticator;

    this.activeCache = cacheService.getCacheInstance(CACHE_NAME);
    if (LOG.isDebugEnabled()) {
      activeCache.addCacheListener(new CacheListener<String, ConcurrentMap<String, Config>>() {

        @Override
        public void onExpire(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
          LOG.debug(CACHE_NAME + " onExpire > " + key + ": " + obj);
        }

        @Override
        public void onRemove(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
          LOG.debug(CACHE_NAME + " onRemove > " + key + ": " + obj);
        }

        @Override
        public void onPut(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
          LOG.debug(CACHE_NAME + " onPut > " + key + ": " + obj);
        }

        @Override
        public void onGet(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
          LOG.debug(CACHE_NAME + " onGet > " + key + ": " + obj);
        }

        @Override
        public void onClearCache(CacheListenerContext context) throws Exception {
          LOG.debug(CACHE_NAME + " onClearCache");
        }
      });
    }

    // predefined file types
    // TODO keep map of type configurations with need of conversion to modern
    // format and back
    // FYI we enable editor for only modern office formats (e.g. docx or odt)
    // Text formats
    fileTypes.put("docx", TYPE_TEXT);
    fileTypes.put("doc", TYPE_TEXT);
    fileTypes.put("odt", TYPE_TEXT);
    fileTypes.put("txt", TYPE_TEXT);
    fileTypes.put("rtf", TYPE_TEXT);
    fileTypes.put("mht", TYPE_TEXT);
    fileTypes.put("html", TYPE_TEXT);
    fileTypes.put("htm", TYPE_TEXT);
    fileTypes.put("epub", TYPE_TEXT);
    fileTypes.put("pdf", TYPE_TEXT);
    fileTypes.put("djvu", TYPE_TEXT);
    fileTypes.put("xps", TYPE_TEXT);
    // Speadsheet formats
    fileTypes.put("xlsx", TYPE_SPREADSHEET);
    fileTypes.put("xls", TYPE_SPREADSHEET);
    fileTypes.put("ods", TYPE_SPREADSHEET);
    // Presentation formats
    fileTypes.put("pptx", TYPE_PRESENTATION);
    fileTypes.put("ppt", TYPE_PRESENTATION);
    fileTypes.put("ppsx", TYPE_PRESENTATION);
    fileTypes.put("pps", TYPE_PRESENTATION);
    fileTypes.put("odp", TYPE_PRESENTATION);

    // configuration
    PropertiesParam param = params.getPropertiesParam("editor-configuration");

    if (param != null) {
      config = Collections.unmodifiableMap(param.getProperties());
    } else {
      throw new ConfigurationException("Property parameters editor-configuration required.");
    }

    String dsSchema = config.get(CONFIG_DS_SCHEMA);
    if (dsSchema == null || (dsSchema = dsSchema.trim()).length() == 0) {
      dsSchema = "http";
    }

    String dsHost = config.get(CONFIG_DS_HOST);
    if (dsHost == null || (dsHost = dsHost.trim()).length() == 0) {
      throw new ConfigurationException("Configuration of " + CONFIG_DS_HOST + " required");
    }
    int portIndex = dsHost.indexOf(HTTP_PORT_DELIMITER);
    if (portIndex > 0) {
      // cut port from DS host to use in canDownloadBy() method
      this.documentserverHostName = dsHost.substring(0, portIndex);
    } else {
      this.documentserverHostName = dsHost;
    }

    this.documentserverAccessOnly = Boolean.parseBoolean(config.get(CONFIG_DS_ACCESS_ONLY));

    String dsAllowedHost = config.get(CONFIG_DS_ALLOWEDHOSTS);
    if (dsAllowedHost != null && !dsAllowedHost.isEmpty()) {
      Set<String> allowedhosts = new HashSet<>();
      for (String ahost : dsAllowedHost.split(",")) {
        ahost = ahost.trim();
        if (!ahost.isEmpty()) {
          allowedhosts.add(lowerCase(ahost));
        }
      }
      this.documentserverAllowedhosts = Collections.unmodifiableSet(allowedhosts);
    } else {
      this.documentserverAllowedhosts = Collections.emptySet();
    }

    // base parameters for API

    StringBuilder documentserverUrl = new StringBuilder();
    documentserverUrl.append(dsSchema);
    documentserverUrl.append("://");
    documentserverUrl.append(dsHost);

    this.uploadUrl = new StringBuilder(documentserverUrl).append("/FileUploader.ashx").toString();
    this.documentserverUrl = new StringBuilder(documentserverUrl).append("/OfficeWeb/").toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(OnlyofficeEditorListener listener) {
    this.listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(OnlyofficeEditorListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Config getEditor(String userId, String workspace, String path) throws OnlyofficeEditorException, RepositoryException {
    return getEditor(userId, nodePath(workspace, path), false);
  }

  /**
   * {@inheritDoc}
   */
  public Config getEditorByKey(String userId, String key) throws OnlyofficeEditorException, RepositoryException {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);
        return config;
      }
    }
    return null;
  }

  /**
   * Gets the editor.
   *
   * @param userId the user id
   * @param nodePath the node path
   * @param createCoEditing if <code>true</code> and has no editor for given
   *          user, create a copy for co-editing if document already editing by
   *          other users
   * @return the editor
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected Config getEditor(String userId, String nodePath, boolean createCoEditing) throws OnlyofficeEditorException,
                                                                                      RepositoryException {
    ConcurrentMap<String, Config> configs = activeCache.get(nodePath);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config == null && createCoEditing) {
        // copy editor for this user from another entry in the configs map
        try {
          Config another = configs.values().iterator().next();
          User user = getUser(userId); // and use this user language
          if (user != null) {
            config = another.forUser(user.getUserName(), user.getFirstName(), user.getLastName(), getUserLang(userId));
            Config existing = configs.putIfAbsent(userId, config);
            if (existing == null) {
              // need update the configs in the cache (for replicated cache)
              activeCache.put(nodePath, configs);
              activeCache.put(config.getDocument().getKey(), configs);
            } else {
              config = existing;
            }
            fireGet(config);
          } else {
            LOG.warn("Attempt to obtain document editor (" + nodePath(another) + ") under not existing user " + userId);
            throw new BadParameterException("User not found for " + another.getDocument().getTitle());
          }
        } catch (NoSuchElementException e) { // if configs was cleaned by
                                             // closing all active editors
          config = null;
        }
      } else if (createCoEditing) {
        // otherwise: config already obtained
        fireGet(config);
      }
      return config; // can be null
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Config createEditor(String schema,
                             String host,
                             String userId,
                             String workspace,
                             String docId) throws OnlyofficeEditorException, RepositoryException {
    if (workspace == null) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    // XXX to let REST EditorService work with paths
    if (docId.startsWith("/")) {
      // it's path as docId
      docId = initDocument(workspace, docId);
    }
    Node node = nodeByUUID(workspace, docId);
    String path = node.getPath();
    // TODO replace with workspace+docId, but this will be less informative in
    // logs/errors
    String nodePath = nodePath(workspace, path);

    if (!node.isNodeType("nt:file")) {
      // TODO other types?
      throw new OnlyofficeEditorException("Only nt:file supported " + nodePath);
    }

    Config config = getEditor(userId, nodePath, true);
    if (config == null) {
      // we should care about concurrent calls here
      activeLock.lock();
      try {
        ConcurrentMap<String, Config> configs = activeCache.get(nodePath);
        if (configs != null) {
          config = getEditor(userId, nodePath, true);
          if (config == null) {
            // it's unexpected state as existing map SHOULD contain a config and
            // it must be copied for given user in getEditor(): client will need
            // to retry the operation
            throw new ConflictException("Cannot obtain configuration for already existing editor");
          }
          // FYI mapping by unique file key should be done by the thread that
          // created this existing map
        } else {
          // Build a new editor config and document key
          User user = getUser(userId);

          String fileType = fileType(node);
          String docType = documentType(fileType);

          Config.Builder builder = Config.editor(documentserverUrl, docType, workspace, path, docId);
          builder.author(userId);
          builder.fileType(fileType);
          builder.created(nodeCreated(node));
          try {
            builder.folder(node.getParent().getName());
          } catch (AccessDeniedException e) {
            // TODO Current user has no permissions to read the document parent
            // - it can be an usecase of
            // shared file.
            // As folder is a text used for "Location" in document info in
            // Onlyoffice, we could guess
            // something like "John Smith's document" or "Product Team document"
            // for sharing from personal
            // docs and a space respectively.
            String owner;
            try {
              owner = node.getProperty("exo:owner").getString();
            } catch (PathNotFoundException oe) {
              owner = "?";
            }
            LOG.warn("Cannot read document parent node: "
                + nodePath(workspace, node.getPath() + ". Owner: " + owner + ". Error: " + e.getMessage()));
            builder.folder(EMPTY_TEXT); // can be empty for Onlyoffice, will
                                        // mean a root folder
          }
          builder.lang(getUserLang(userId));
          builder.mode("edit");
          builder.title(nodeTitle(node));
          builder.userId(user.getUserName());
          builder.userFirstName(user.getFirstName());
          builder.userLastName(user.getLastName());

          String key = generateId(workspace, path).toString();

          builder.key(key);

          StringBuilder platformUrl = platformUrl(schema, host);
          // REST URL for file and callback URLs fill be generated respectively
          // the platform URL and actual user
          builder.generateUrls(new StringBuilder(platformUrl).append('/')
                                                             .append(PortalContainer.getCurrentRestContextName())
                                                             .toString());
          // editor page URL
          builder.editorUrl(platformUrl.append(editorURLPath(docId)).toString());

          config = builder.build();

          // Create users' config map and add first user
          configs = new ConcurrentHashMap<String, Config>();
          configs.put(userId, config);

          // mapping by node path for getEditor()
          activeCache.put(nodePath, configs);
          // mapping by unique file key for updateDocument()
          activeCache.put(key, configs);
        }
      } finally {
        activeLock.unlock();
      }
      fireCreated(config);
    }
    return config;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("deprecation")
  @Override
  public DocumentContent getContent(String userId, String key) throws OnlyofficeEditorException, RepositoryException {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);

        // use user session here:
        // remember real context state and session provider to restore them at
        // the end
        ConversationState contextState = ConversationState.getCurrent();
        SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
        try {
          // XXX we want do all the job under actual (requester) user here
          Identity userIdentity = userIdentity(userId);
          if (userIdentity != null) {
            ConversationState state = new ConversationState(userIdentity);
            // Keep subject as attribute in ConversationState.
            state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
            ConversationState.setCurrent(state);
            SessionProvider userProvider = new SessionProvider(state);
            sessionProviders.setSessionProvider(null, userProvider);
          } else {
            LOG.warn("User identity not found " + userId + " for content of " + config.getDocument().getKey() + " "
                + config.getPath());
            throw new OnlyofficeEditorException("User identity not found " + userId);
          }

          // work in user session
          Node node = node(config.getWorkspace(), config.getPath());
          Node content = nodeContent(node);

          final String mimeType = content.getProperty("jcr:mimeType").getString();
          // data stream will be closed when EoF will be reached
          final InputStream data = new AutoCloseInputStream(content.getProperty("jcr:data").getStream());
          return new DocumentContent() {
            @Override
            public String getType() {
              return mimeType;
            }

            @Override
            public InputStream getData() {
              return data;
            }
          };
        } finally {
          // restore context env
          ConversationState.setCurrent(contextState);
          sessionProviders.setSessionProvider(null, contextProvider);
        }
      } else {
        throw new BadParameterException("User editor not found or already closed " + userId);
      }
    } else {
      throw new BadParameterException("File key not found " + key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canDownloadBy(String hostName) {
    if (documentserverAccessOnly) {
      // #19 support advanced configuration of DS's allowed hosts
      return documentserverHostName.equalsIgnoreCase(hostName) || documentserverAllowedhosts.contains(lowerCase(hostName));
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ChangeState getState(String userId, String key) throws OnlyofficeEditorException {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);
        String[] users = getActiveUsers(configs);
        return new ChangeState(false, config.getError(), users);
      } else {
        throw new BadParameterException("User editor not found " + userId);
      }
    } else {
      return new ChangeState(true, null, new String[0]); // not found - thus
                                                         // already saved
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateDocument(String userId, DocumentStatus status) throws OnlyofficeEditorException, RepositoryException {
    String key = status.getKey();
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);

        String nodePath = nodePath(config.getWorkspace(), config.getPath());

        // status of the document. Can have the following values: 0 - no
        // document with the key identifier
        // could be found, 1 - document is being edited (user opened an editor),
        // 2 - document is ready for
        // saving (last user closed it), 3 - document saving error has occurred,
        // 4 - document is closed with
        // no changes.
        long statusCode = status.getStatus();

        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Onlyoffice status " + statusCode + " for " + key + ". URL: " + status.getUrl() + ". Users: "
              + Arrays.toString(status.getUsers()) + " << Local file: " + nodePath);
        }

        if (statusCode == 0) {
          // Onlyoffice doesn't know about such document: we clean our records
          // and raise an error
          activeCache.remove(key);
          activeCache.remove(nodePath);
          LOG.warn("Received Onlyoffice status: no document with the key identifier could be found. Key: " + key + ". Document "
              + nodePath);
          throw new OnlyofficeEditorException("Error editing document: document ID not found");
        } else if (statusCode == 1) {
          // while "document is being edited" (1) will come just before
          // "document is ready for saving"
          // (2) we could do nothing at this point, indeed need study how
          // Onlyoffice behave in different
          // situations when user leave page open or browser
          // hangs/crashes/killed - it still could be useful
          // here to make a cleanup
          // Sync users from the status to active config: this should close
          // configs of gone users
          String[] users = status.getUsers();
          if (syncUsers(configs, users)) {
            // Update cached (for replicated cache)
            activeCache.put(key, configs);
            activeCache.put(nodePath, configs);
          }
        } else if (statusCode == 2) {
          // save as "document is ready for saving" (2)
          download(config, status);
          activeCache.remove(key);
          activeCache.remove(nodePath);
        } else if (statusCode == 3) {
          // it's an error of saving in Onlyoffice
          // we sync to remote editors list first
          syncUsers(configs, status.getUsers());
          if (configs.size() <= 1) {
            // if one or zero users we can save it
            String url = status.getUrl();
            if (url != null && url.length() > 0) {
              // if URL available then we can download it assuming it's last
              // successful modification
              // the same behaviour as for status (2)
              download(config, status);
              activeCache.remove(key);
              activeCache.remove(nodePath);
              config.setError("Error in editor. Last change was successfully saved");
              // XXX even having it saved we don't known exactly what is it,
              // thus user should see the editor
              // again and decide about content (e.g. it can download it
              // manually from Onlyoffice)
              // config.close(); // close for use in listeners
              // fireSaved(config);
              fireError(config);
              LOG.warn("Received Onlyoffice error of saving document. Key: " + key + ". Users: "
                  + Arrays.toString(status.getUsers()) + ". Last change was successfully saved for " + nodePath);
            } else {
              // if error without content URL and last user: it's error state
              LOG.warn("Received Onlyoffice error of saving document without changes URL. Key: " + key + ". Users: "
                  + Arrays.toString(status.getUsers()) + ". Document " + nodePath);
              config.setError("Error in editor. No changes saved");
              // Update cached (for replicated cache)
              activeCache.put(key, configs);
              activeCache.put(nodePath, configs);
              fireError(config);
              // TODO no sense to throw an ex here: it will be caught by the
              // caller (REST) and returned to
              // the Onlyoffice server as 500 response, but it doesn't deal with
              // it and will try send the
              // status again.
            }
          } else {
            // otherwise we assume other user will save it later
            LOG.warn("Received Onlyoffice error of saving document with several editors. Key: " + key + ". Users: "
                + Arrays.toString(status.getUsers()) + ". Document " + nodePath);
            config.setError("Error in editor. Document still in editing state");
            // Update cached (for replicated cache)
            activeCache.put(key, configs);
            activeCache.put(nodePath, configs);
            fireError(config);
          }
        } else if (statusCode == 4) {
          // user(s) haven't changed the document but closed it: sync users to
          // fire onLeaved event(s)
          syncUsers(configs, status.getUsers());
          // and remove this document from active configs
          activeCache.remove(key);
          activeCache.remove(nodePath);
        } else {
          // warn unexpected status, wait for next status
          LOG.warn("Received Onlyoffice unexpected status. Key: " + key + ". URL: " + status.getUrl() + ". Users: "
              + status.getUsers() + ". Document " + nodePath);
        }
      } else {
        throw new BadParameterException("User editor not found " + userId);
      }
    } else {
      throw new BadParameterException("File key not found " + key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String initDocument(Node node) throws OnlyofficeEditorException, RepositoryException {
    if (node.canAddMixin("mix:referenceable")) {
      node.addMixin("mix:referenceable");
      node.save();
    }
    return node.getUUID();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String initDocument(String workspace, String path) throws OnlyofficeEditorException, RepositoryException {
    Node node = node(workspace, path);
    return initDocument(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEditorLink(String schema, String host, String workspace, String docId) {
    return platformUrl(schema, host).append(editorURLPath(docId)).toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getDocument(String workspace, String uuid) throws RepositoryException {
    if (workspace == null) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    try {
      return nodeByUUID(workspace, uuid);
    } catch (ItemNotFoundException e) {
      return null;
    }
  }

  /**
   * On-start initializer.
   */
  @Override
  public void start() {
    LOG.info("Onlyoffice Editor service successfuly started");
  }

  /**
   * On-stop finalizer.
   */
  @Override
  public void stop() {
    LOG.info("Onlyoffice  Editor service successfuly stopped");
  }

  // *********************** implementation level ***************

  /**
   * Node title.
   *
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String nodeTitle(Node node) throws RepositoryException {
    String title = null;
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getString();
    } else if (node.hasProperty("jcr:content/dc:title")) {
      Property dcTitle = node.getProperty("jcr:content/dc:title");
      if (dcTitle.getDefinition().isMultiple()) {
        Value[] dctValues = dcTitle.getValues();
        if (dctValues.length > 0) {
          title = dctValues[0].getString();
        }
      } else {
        title = dcTitle.getString();
      }
    } else if (node.hasProperty("exo:name")) {
      // FYI exo:name seems the same as node name
      title = node.getProperty("exo:name").getString();
    }
    if (title == null) {
      title = node.getName();
    }
    return title;
  }

  /**
   * File type.
   *
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String fileType(Node node) throws RepositoryException {
    String title = nodeTitle(node);
    int dotIndex = title.lastIndexOf('.');
    if (dotIndex >= 0 && dotIndex < title.length()) {
      String fileExt = title.substring(dotIndex + 1).trim();
      if (fileTypes.containsKey(fileExt)) {
        return fileExt;
      }
    }
    // TODO should we find a type from MIME-type?
    // String mimeType =
    // node.getProperty("jcr:content/jcr:mimeType").getString();
    return null;
  }

  /**
   * Document type.
   *
   * @param fileType the file type
   * @return the string
   */
  protected String documentType(String fileType) {
    String docType = fileTypes.get(fileType);
    if (docType != null) {
      return docType;
    }
    return TYPE_TEXT; // we assume text document by default
  }

  /**
   * Node content.
   *
   * @param node the node
   * @return the node
   * @throws RepositoryException the repository exception
   */
  protected Node nodeContent(Node node) throws RepositoryException {
    return node.getNode("jcr:content");
  }

  /**
   * Node created.
   *
   * @param node the node
   * @return the calendar
   * @throws RepositoryException the repository exception
   */
  protected Calendar nodeCreated(Node node) throws RepositoryException {
    return node.getProperty("jcr:created").getDate();
  }

  /**
   * Mime type.
   *
   * @param content the content
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String mimeType(Node content) throws RepositoryException {
    return content.getProperty("jcr:mimeType").getString();
  }

  /**
   * Data.
   *
   * @param content the content
   * @return the property
   * @throws RepositoryException the repository exception
   */
  protected Property data(Node content) throws RepositoryException {
    return content.getProperty("jcr:data");
  }

  /**
   * Generate id.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the uuid
   */
  protected UUID generateId(String workspace, String path) {
    StringBuilder s = new StringBuilder();
    s.append(workspace);
    s.append(path);
    s.append(System.currentTimeMillis());
    s.append(String.valueOf(RANDOM.nextLong()));

    return UUID.nameUUIDFromBytes(s.toString().getBytes());
  }

  /**
   * Node path.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the string
   */
  protected String nodePath(String workspace, String path) {
    return new StringBuilder().append(workspace).append(":").append(path).toString();
  }

  /**
   * Node path.
   *
   * @param config the config
   * @return the string
   */
  protected String nodePath(Config config) {
    return nodePath(config.getWorkspace(), config.getPath());
  }

  /**
   * Gets the user.
   *
   * @param username the username
   * @return the user
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  protected User getUser(String username) throws OnlyofficeEditorException {
    try {
      return organization.getUserHandler().findUserByName(username);
    } catch (Exception e) {
      throw new OnlyofficeEditorException("Error searching user " + username, e);
    }
  }

  /**
   * Webdav url.
   *
   * @param schema the schema
   * @param host the host
   * @param workspace the workspace
   * @param path the path
   * @return the string
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected String webdavUrl(String schema, String host, String workspace, String path) throws OnlyofficeEditorException,
                                                                                        RepositoryException {
    StringBuilder filePath = new StringBuilder();
    try {
      URI baseWebdavUri = webdavUri(schema, host);

      filePath.append(baseWebdavUri.getPath());
      filePath.append('/');
      filePath.append(jcrService.getCurrentRepository().getConfiguration().getName());
      filePath.append('/');
      filePath.append(workspace);
      filePath.append(path);

      URI uri = new URI(baseWebdavUri.getScheme(),
                        null,
                        baseWebdavUri.getHost(),
                        baseWebdavUri.getPort(),
                        filePath.toString(),
                        null,
                        null);
      return uri.toASCIIString();
    } catch (URISyntaxException e) {
      throw new OnlyofficeEditorException("Error creating content link (WebDAV) for " + path + " in " + workspace, e);
    }
  }

  /**
   * Sync users.
   *
   * @param configs the configs
   * @param users the users
   * @return true, if actually changed editor config user(s)
   */
  protected boolean syncUsers(ConcurrentMap<String, Config> configs, String[] users) {
    Set<String> editors = new HashSet<String>(Arrays.asList(users));
    // remove gone editors
    boolean updated = false;
    for (Iterator<Map.Entry<String, Config>> ceiter = configs.entrySet().iterator(); ceiter.hasNext();) {
      Map.Entry<String, Config> ce = ceiter.next();
      String user = ce.getKey();
      Config config = ce.getValue();
      if (editors.contains(user)) {
        if (config.isCreated() || config.isClosed()) {
          // editor was (re)opened by user
          config.open();
          fireJoined(config);
          updated = true;
        }
      } else {
        // editor was closed by user: it will be closing if closed via WebUI of
        // ECMS explorer, open in general case
        if (config.isClosing() || config.isOpen()) {
          // closed because user sync happens when someone else still editing or
          // nothing edited
          config.closed();
          fireLeaved(config);
          updated = true;
        }
      }
    }
    return updated;
  }

  /**
   * Gets the current users.
   *
   * @param configs the configs
   * @return the current users
   */
  protected String[] getActiveUsers(ConcurrentMap<String, Config> configs) {
    // copy key set to avoid confuses w/ concurrency
    Set<String> userIds = new LinkedHashSet<String>(configs.keySet());
    // remove not existing locally (just removed), not yet open (created) or
    // already closed
    for (Iterator<String> uiter = userIds.iterator(); uiter.hasNext();) {
      String userId = uiter.next();
      Config config = configs.get(userId);
      if (config == null || config.isCreated() || config.isClosed()) {
        uiter.remove();
      }
    }
    return userIds.toArray(new String[userIds.size()]);
  }

  /**
   * Download.
   *
   * @param config the config
   * @param status the status
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  @SuppressWarnings("deprecation")
  protected void download(Config config, DocumentStatus status) throws OnlyofficeEditorException, RepositoryException {

    // First mark closing, then do actual download and save in storage. Note:
    // closing state may be already set
    // by UI layer (OnlyofficeEditorUIService).
    config.closing();

    String workspace = config.getWorkspace();
    String path = config.getPath();
    String nodePath = nodePath(workspace, path);

    if (LOG.isDebugEnabled()) {
      LOG.debug(">> download(" + nodePath + ", " + config.getDocument().getKey() + ")");
    }

    String userId = status.getUsers()[0]; // assuming a single user here (last
                                          // editor)
    validateUser(userId, config);

    String contentUrl = status.getUrl();
    Calendar editedTime = Calendar.getInstance();

    HttpURLConnection connection;
    InputStream data;
    try {
      URL url = new URL(contentUrl);
      connection = (HttpURLConnection) url.openConnection();
      data = connection.getInputStream();
      if (data == null) {
        throw new OnlyofficeEditorException("Content stream is null");
      }
    } catch (MalformedURLException e) {
      throw new OnlyofficeEditorException("Error parsing content URL " + contentUrl + " for " + nodePath, e);
    } catch (IOException e) {
      throw new OnlyofficeEditorException("Error reading content stream " + contentUrl + " for " + nodePath, e);
    }

    // remember real context state and session provider to restore them at the
    // end
    ConversationState contextState = ConversationState.getCurrent();
    SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
    try {
      // We want do all the job under actual (last editor) user here
      // Notable that some WCM actions (FileUpdateActivityListener) will fail if
      // user will be anonymous
      // TODO but it seems looks as nasty thing for security, it should be
      // carefully reviewed for production
      Identity userIdentity = userIdentity(userId);
      if (userIdentity != null) {
        ConversationState state = new ConversationState(userIdentity);
        // Keep subject as attribute in ConversationState.
        state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
        ConversationState.setCurrent(state);
        SessionProvider userProvider = new SessionProvider(state);
        sessionProviders.setSessionProvider(null, userProvider);
        if (LOG.isDebugEnabled()) {
          LOG.debug(">>> download under user " + userIdentity.getUserId() + " (" + nodePath + ", " + config.getDocument().getKey()
              + ")");
        }
      } else {
        LOG.warn("User identity not found " + userId + " for downloading " + config.getDocument().getKey() + " " + nodePath);
        throw new OnlyofficeEditorException("User identity not found " + userId);
      }

      // work in user session
      Node node = node(workspace, path);
      Node content = nodeContent(node);

      // lock node first, this also will check if node isn't locked by another
      // user (will throw exception)
      Lock lock = lock(node, config);
      if (lock == null) {
        throw new OnlyofficeEditorException("Document locked " + nodePath);
      }

      // manage version only if node already mix:versionable
      boolean checkIn = checkout(node);

      try {
        // update document
        content.setProperty("jcr:data", data);
        // update modified date (this will force PDFViewer to regenerate its
        // images)
        content.setProperty("jcr:lastModified", editedTime);
        if (content.hasProperty("exo:dateModified")) {
          content.setProperty("exo:dateModified", editedTime);
        }
        if (content.hasProperty("exo:lastModifiedDate")) {
          content.setProperty("exo:lastModifiedDate", editedTime);
        }
        if (node.hasProperty("exo:lastModifiedDate")) {
          node.setProperty("exo:lastModifiedDate", editedTime);
        }
        if (node.hasProperty("exo:dateModified")) {
          node.setProperty("exo:dateModified", editedTime);
        }
        if (node.hasProperty("exo:lastModifier")) {
          node.setProperty("exo:lastModifier", userId);
        }

        node.save();
        if (checkIn) {
          // Make a new version from the downloaded state
          node.checkin();
          // Since 1.2.0-RC01 we check-out the document to let (more) other
          // actions in ECMS appear on it
          node.checkout();
        }

        config.closed(); // reset transient closing state

        fireSaved(config);
      } catch (RepositoryException e) {
        try {
          node.refresh(false); // rollback JCR modifications
        } catch (Throwable re) {
          LOG.warn("Error rolling back failed change for " + nodePath, re);
        }
        throw e; // let the caller handle it further
      } finally {
        try {
          data.close();
        } catch (Throwable e) {
          LOG.warn("Error closing exported content stream for " + nodePath, e);
        }
        try {
          connection.disconnect();
        } catch (Throwable e) {
          LOG.warn("Error closing export connection for " + nodePath, e);
        }
        try {
          if (lock != null && node.isLocked()) {
            node.unlock();
          }
        } catch (Throwable e) {
          LOG.warn("Error unlocking edited document " + nodePath(workspace, path), e);
        }
      }
    } finally {
      // restore context env
      ConversationState.setCurrent(contextState);
      sessionProviders.setSessionProvider(null, contextProvider);
    }
  }

  /**
   * Node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
  protected Node node(String workspace, String path) throws BadParameterException, RepositoryException {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());

    Item item = finder.findItem(userSession, path);
    if (item.isNode()) {
      return (Node) item;
    } else {
      throw new BadParameterException("Not a node " + path);
    }
  }

  /**
   * System node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
  protected Node systemNode(String workspace, String path) throws BadParameterException, RepositoryException {
    SessionProvider sp = sessionProviders.getSystemSessionProvider(null);
    Session sysSession = sp.getSession(workspace, jcrService.getCurrentRepository());

    Item item = finder.findItem(sysSession, path);
    if (item.isNode()) {
      return (Node) item;
    } else {
      throw new BadParameterException("Not a node " + path);
    }
  }

  /**
   * Node by UUID.
   *
   * @param workspace the workspace
   * @param uuid the UUID
   * @return the node
   * @throws RepositoryException the repository exception
   */
  protected Node nodeByUUID(String workspace, String uuid) throws RepositoryException {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());
    return userSession.getNodeByUUID(uuid);
  }

  /**
   * Checkout.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  protected boolean checkout(Node node) throws RepositoryException {
    if (node.isNodeType("mix:versionable")) {
      if (!node.isCheckedOut()) {
        node.checkout();
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Lock by current user of the node. If lock attempts will succeed in
   * predefined time this method will throw {@link OnlyofficeEditorException}.
   * If node isn't mix:versionable it will be added first and node saved.
   *
   * @param node {@link Node}
   * @param config {@link Config}
   * @return {@link Lock} acquired by current user.
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected Lock lock(Node node, Config config) throws OnlyofficeEditorException, RepositoryException {
    if (!node.isNodeType("mix:lockable")) {
      if (!node.isCheckedOut() && node.isNodeType("mix:versionable")) {
        node.checkout();
      }
      node.addMixin("mix:lockable");
      node.save();
    }

    Config.Editor.User user = config.getEditorConfig().getUser();
    Lock lock;
    int attempts = 0;
    try {
      do {
        attempts++;
        if (node.isLocked()) {
          String lockToken = user.getLockToken();
          if (node.getLock().getLockOwner().equals(user.getId()) && lockToken != null) {
            // already this user lock
            node.getSession().addLockToken(lockToken);
            lock = node.getLock();
          } else {
            // need wait for unlock
            Thread.sleep(LOCK_WAIT_TIMEOUT);
            lock = null;
          }
        } else {
          lock = node.lock(true, false); // TODO deep vs only file node lock?
          user.setLockToken(lock.getLockToken()); // keep own token for crossing
                                                  // sessions
        }
      } while (lock == null && attempts <= LOCK_WAIT_ATTEMTS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OnlyofficeEditorException("Error waiting for lock of " + nodePath(config.getWorkspace(), config.getPath()), e);
    }
    return lock;
  }

  /**
   * Validate user.
   *
   * @param userId the user id
   * @param config the config
   * @throws BadParameterException if user not found
   * @throws OnlyofficeEditorException if error searching user in organization
   *           service
   */
  protected void validateUser(String userId, Config config) throws BadParameterException, OnlyofficeEditorException {
    User user = getUser(userId);
    if (user == null) {
      LOG.warn("Attempt to access editor document (" + nodePath(config) + ") under not existing user " + userId);
      throw new BadParameterException("User not found for " + config.getDocument().getTitle());
    }
  }

  /**
   * Gets the user lang.
   *
   * @param userId the user id
   * @return the lang can be <code>null</code> if user has no profile or
   *         language in it or user profile error
   */
  protected String getUserLang(String userId) {
    UserProfileHandler hanlder = organization.getUserProfileHandler();
    try {
      UserProfile userProfile = hanlder.findUserProfileByName(userId);
      if (userProfile != null) {
        String lang = userProfile.getAttribute(Constants.USER_LANGUAGE);
        if (lang != null) {
          // XXX Onlyoffice doesn't support country codes (as of Apr 6, 2016)
          // All supported langauges here
          // http://helpcenter.onlyoffice.com/tipstricks/available-languages.aspx
          int cci = lang.indexOf("_");
          if (cci > 0) {
            lang = lang.substring(0, cci);
          }
        } else {
          lang = null;
        }
        return lang;
      } else {
        return null;
      }
    } catch (Exception e) {
      LOG.warn("Error searching user profile " + userId, e);
      return null;
    }
  }

  /**
   * Platform url.
   *
   * @param schema the schema
   * @param host the host
   * @return the string builder
   */
  protected StringBuilder platformUrl(String schema, String host) {
    StringBuilder platformUrl = new StringBuilder();
    platformUrl.append(schema);
    platformUrl.append("://");
    platformUrl.append(host);
    platformUrl.append('/');
    platformUrl.append(PortalContainer.getCurrentPortalContainerName());

    return platformUrl;
  }

  /**
   * Platform REST URL.
   *
   * @param platformUrl the platform URL
   * @return the string builder
   */
  protected StringBuilder platformRestUrl(CharSequence platformUrl) {
    StringBuilder restUrl = new StringBuilder(platformUrl);
    restUrl.append('/');
    restUrl.append(PortalContainer.getCurrentRestContextName());

    return restUrl;
  }

  /**
   * Platform REST URL.
   *
   * @param schema the schema
   * @param host the host
   * @return the string builder
   */
  @Deprecated
  protected StringBuilder platformRestUrl(String schema, String host) {
    return platformRestUrl(platformUrl(schema, host));
  }

  /**
   * Webdav uri.
   *
   * @param schema the schema
   * @param host the host
   * @return the uri
   * @throws URISyntaxException the URI syntax exception
   */
  @Deprecated
  protected URI webdavUri(String schema, String host) throws URISyntaxException {
    return new URI(platformRestUrl(schema, host).append("/jcr").toString());
  }

  /**
   * Fire created.
   *
   * @param config the config
   */
  protected void fireCreated(Config config) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onCreate(config);
      } catch (Throwable t) {
        LOG.warn("Creation listener error", t);
      }
    }
  }

  /**
   * Fire get.
   *
   * @param config the config
   */
  protected void fireGet(Config config) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onGet(config);
      } catch (Throwable t) {
        LOG.warn("Read (Get) listener error", t);
      }
    }
  }

  /**
   * Fire joined.
   *
   * @param config the config
   */
  protected void fireJoined(Config config) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onJoined(config);
      } catch (Throwable t) {
        LOG.warn("User joining listener error", t);
      }
    }
  }

  /**
   * Fire leaved.
   *
   * @param config the config
   */
  protected void fireLeaved(Config config) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onLeaved(config);
      } catch (Throwable t) {
        LOG.warn("User leaving listener error", t);
      }
    }
  }

  /**
   * Fire saved.
   *
   * @param config the config
   */
  protected void fireSaved(Config config) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onSaved(config);
      } catch (Throwable t) {
        LOG.warn("Saving listener error", t);
      }
    }
  }

  /**
   * Fire error.
   *
   * @param config the config
   */
  protected void fireError(Config config) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onError(config);
      } catch (Throwable t) {
        LOG.warn("Error listener error", t);
      }
    }
  }

  /**
   * Find or create user identity.
   *
   * @param userId the user id
   * @return the identity can be null if not found and cannot be created via
   *         current authenticator
   */
  protected Identity userIdentity(String userId) {
    Identity userIdentity = identityRegistry.getIdentity(userId);
    if (userIdentity == null) {
      // We create user identity by authenticator, but not register it in the
      // registry
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("User identity not registered, trying to create it for: " + userId);
        }
        userIdentity = authenticator.createIdentity(userId);
      } catch (Exception e) {
        LOG.warn("Failed to create user identity: " + userId, e);
      }
    }
    return userIdentity;
  }

  /**
   * Get lower case copy of the given string.
   *
   * @param str the str
   * @return the string
   */
  protected String lowerCase(String str) {
    return str.toUpperCase().toLowerCase();
  }

  /**
   * Editor URL path.
   *
   * @param docId the doc id
   * @return the string
   */
  protected String editorURLPath(String docId) {
    return new StringBuilder().append('/')
                              .append(CommonsUtils.getCurrentPortalOwner())
                              .append("/oeditor?docId=")
                              .append(docId)
                              .toString();
  }
}
