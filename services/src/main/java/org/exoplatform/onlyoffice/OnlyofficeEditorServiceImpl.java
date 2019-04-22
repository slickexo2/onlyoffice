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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.json.JSONObject;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.onlyoffice.Config.Editor;
import org.exoplatform.onlyoffice.jcr.NodeFinder;
import org.exoplatform.portal.Constants;
import org.exoplatform.services.cache.CacheListener;
import org.exoplatform.services.cache.CacheListenerContext;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.lock.LockService;
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
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

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
  protected static final Log    LOG                    = ExoLogger.getLogger(OnlyofficeEditorServiceImpl.class);

  /** The Constant RANDOM. */
  protected static final Random RANDOM                 = new Random();

  /** The Constant CONFIG_DS_HOST. */
  public static final String    CONFIG_DS_HOST         = "documentserver-host";

  /** The Constant CONFIG_DS_SCHEMA. */
  public static final String    CONFIG_DS_SCHEMA       = "documentserver-schema";

  /** The Constant CONFIG_DS_ACCESS_ONLY. */
  public static final String    CONFIG_DS_ACCESS_ONLY  = "documentserver-access-only";

  /** The Constant CONFIG_DS_SECRET. */
  public static final String    CONFIG_DS_SECRET       = "documentserver-secret";

  /**
   * Configuration key for Document Server's allowed hosts in requests from a DS
   * to eXo side.
   */
  public static final String    CONFIG_DS_ALLOWEDHOSTS = "documentserver-allowedhosts";

  /** The Constant HTTP_PORT_DELIMITER. */
  protected static final char   HTTP_PORT_DELIMITER    = ':';

  /** The Constant TYPE_TEXT. */
  protected static final String TYPE_TEXT              = "text";

  /** The Constant TYPE_SPREADSHEET. */
  protected static final String TYPE_SPREADSHEET       = "spreadsheet";

  /** The Constant TYPE_PRESENTATION. */
  protected static final String TYPE_PRESENTATION      = "presentation";

  /** The Constant LOCK_WAIT_ATTEMTS. */
  protected static final int    LOCK_WAIT_ATTEMTS      = 20;

  /** The Constant LOCK_WAIT_TIMEOUT. */
  protected static final long   LOCK_WAIT_TIMEOUT      = 250;

  /** The Constant EMPTY_TEXT. */
  protected static final String EMPTY_TEXT             = "".intern();

  /** The Constant CACHE_NAME. */
  public static final String    CACHE_NAME             = "onlyoffice.EditorCache".intern();

  /**
   * NewDocumentTypesConfig.
   */
  public static class DocumentTypesConfig {

    /** The mime types. */
    protected List<String> mimeTypes;

    /**
     * Gets the mime types.
     *
     * @return the mime types
     */
    public List<String> getMimeTypes() {
      return mimeTypes;
    }

    /**
     * Sets the mime types.
     *
     * @param mimeTypes the new mime types
     */
    public void setMimeTypes(List<String> mimeTypes) {
      this.mimeTypes = mimeTypes;
    }
  }

  /**
   * The Class LockState.
   */
  class LockState {

    /** The lock token. */
    final String lockToken;

    /** The lock. */
    final Lock   lock;

    /**
     * Instantiates a new lock state.
     *
     * @param lockToken the lock token
     */
    LockState(String lockToken) {
      super();
      this.lockToken = lockToken;
      this.lock = null;
    }

    /**
     * Instantiates a new lock state.
     *
     * @param lock the lock
     */
    LockState(Lock lock) {
      super();
      this.lockToken = null;
      this.lock = lock;
    }

    /**
     * Instantiates a new lock state.
     */
    LockState() {
      super();
      this.lockToken = null;
      this.lock = null;
    }

    /**
     * Check if was locked by this editor service.
     *
     * @return true, if successful
     */
    boolean wasLocked() {
      return lock != null;
    }

    /**
     * Check can edit a document associated with this lock.
     *
     * @return true, if successful
     */
    boolean canEdit() {
      return lock != null || lockToken != null;
    }
  }

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

  /** The document service. */
  protected final DocumentService                                 documentService;

  /** The lock service. */
  protected final LockService                                     lockService;

  /** Cache of Editing documents. */
  protected final ExoCache<String, ConcurrentMap<String, Config>> activeCache;

  /** Lock for updating Editing documents cache. */
  protected final ReentrantLock                                   activeLock   = new ReentrantLock();

  /** The config. */
  protected final Map<String, String>                             config;

  /** The upload url. */
  protected final String                                          uploadUrl;

  /** The documentserver host name. */
  protected final String                                          documentserverHostName;

  /** The documentserver url. */
  protected final String                                          documentserverUrl;

  /** The document command service url. */
  protected final String                                          commandServiceUrl;
  
  /** The document server secret. */
  protected final String                                          documentserverSecret;

  /** The documentserver access only. */
  protected final boolean                                         documentserverAccessOnly;

  /** The documentserver allowed hosts (can be empty if not configured). */
  protected final Set<String>                                     documentserverAllowedhosts;

  /** The file types. */
  protected final Map<String, String>                             fileTypes    = new ConcurrentHashMap<String, String>();

  /** The upload params. */
  protected final MessageFormat                                   uploadParams =
                                                                               new MessageFormat("?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}");

  /** The listeners. */
  protected final ConcurrentLinkedQueue<OnlyofficeEditorListener> listeners    =
                                                                            new ConcurrentLinkedQueue<OnlyofficeEditorListener>();

  /** The document type plugin. */
  protected DocumentTypePlugin                                    documentTypePlugin;

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
   * @param documentService the document service (ECMS)
   * @param lockService the lock service
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
                                     DocumentService documentService,
                                     LockService lockService,
                                     InitParams params)
      throws ConfigurationException {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.finder = finder;
    this.organization = organization;
    this.authenticator = authenticator;
    this.documentService = documentService;
    this.lockService = lockService;

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

    this.documentserverSecret = config.get(CONFIG_DS_SECRET);
    if(documentserverSecret == null || documentserverSecret.trim().isEmpty()) {
      throw new ConfigurationException("Configuration of " + CONFIG_DS_SECRET + " required");
    }
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
    this.commandServiceUrl = new StringBuilder(documentserverUrl).append("/coauthoring/CommandService.ashx").toString();
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
      DocumentStatus status = new DocumentStatus();
      status.setUsers(new String[] { userId });
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
            status.setConfig(config);
            status.setUrl(config.getEditorUrl());
            status.setKey(config.getDocument().getKey());
            fireGet(status);
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
        status.setConfig(config);
        status.setUrl(config.getEditorUrl());
        status.setKey(config.getDocument().getKey());
        fireGet(status);
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
                             int port,
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
    String nodePath = nodePath(workspace, path);

    // TODO other node types?
    if (!node.isNodeType("nt:file")) {
      throw new OnlyofficeEditorException("Document should be a nt:file node: " + nodePath);
    }
    if (!canEditDocument(node)) {
      throw new OnlyofficeEditorException("Cannot edit document: " + nodePath);
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

          StringBuilder platformUrl = platformUrl(schema, host, port);
          // REST URL for file and callback URLs fill be generated respectively
          // the platform URL and actual user
          builder.generateUrls(new StringBuilder(platformUrl).append('/')
                                                             .append(PortalContainer.getCurrentRestContextName())
                                                             .toString());
          // editor page URL
          builder.editorUrl(new StringBuilder(platformUrl).append(editorURLPath(docId)).toString());

          // ECMS explorer page URL
          String ecmsPageLink = explorerLink(path);
          builder.explorerUri(explorerUri(schema, host, port, ecmsPageLink));
          builder.secret(documentserverSecret);
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
      DocumentStatus status = new DocumentStatus();
      status.setConfig(config);
      status.setUsers(new String[] { userId });
      status.setUrl(config.getEditorUrl());
      status.setKey(config.getDocument().getKey());
      fireCreated(status);
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
        status.setConfig(config);
        validateUser(userId, config);

        String nodePath = nodePath(config.getWorkspace(), config.getPath());

        // status of the document. Can have the following values:
        // 0 - no document with the key identifier could be found,
        // 1 - document is being edited (user opened an editor),
        // 2 - document is ready for saving (last user closed it),
        // 3 - document saving error has occurred,
        // 4 - document is closed with no changes.
        // 6 - document is being edited, but the current document state is
        // saved,
        // 7 - error has occurred while force saving the document.
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
          LOG.warn("Received Onlyoffice status: no document with the key identifier could be found. Key: " + key + ". Document: "
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
          Editor.User lastUser = getUser(key, status.getLastUser());
          Editor.User lastModifier = getLastModifier(key);
          // We download if there were modifications after the last saving.
          if (lastModifier.getId().equals(lastUser.getId()) && lastUser.getLastModified() > lastUser.getLastSaved()) {
            downloadClosed(config, status);
          }
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
              // successful modification the same behaviour as for status (2)
              downloadClosed(config, status);
              activeCache.remove(key);
              activeCache.remove(nodePath);
              config.setError("Error in editor (" + status.getError() + "). Last change was successfully saved");
              fireError(status);
              LOG.warn("Received Onlyoffice error of saving document. Key: " + key + ". Users: "
                  + Arrays.toString(status.getUsers()) + ". Error: " + status.getError()
                  + ". Last change was successfully saved for " + nodePath);
            } else {
              // if error without content URL and last user: it's error state
              LOG.warn("Received Onlyoffice error of saving document without changes URL. Key: " + key + ". Users: "
                  + Arrays.toString(status.getUsers()) + ". Document: " + nodePath + ". Error: " + status.getError());
              config.setError("Error in editor (" + status.getError() + "). No changes saved");
              // Update cached (for replicated cache)
              activeCache.put(key, configs);
              activeCache.put(nodePath, configs);
              fireError(status);
              // TODO no sense to throw an ex here: it will be caught by the
              // caller (REST) and returned to
              // the Onlyoffice server as 500 response, but it doesn't deal with
              // it and will try send the status again.
            }
          } else {
            // otherwise we assume other user will save it later
            LOG.warn("Received Onlyoffice error of saving document with several editors. Key: " + key + ". Users: "
                + Arrays.toString(status.getUsers()) + ". Document: " + nodePath);
            config.setError("Error in editor. Document still in editing state");
            // Update cached (for replicated cache)
            activeCache.put(key, configs);
            activeCache.put(nodePath, configs);
            fireError(status);
          }
        } else if (statusCode == 4) {
          // user(s) haven't changed the document but closed it: sync users to
          // fire onLeaved event(s)
          syncUsers(configs, status.getUsers());
          // and remove this document from active configs
          activeCache.remove(key);
          activeCache.remove(nodePath);
        } else if (statusCode == 6) {
          // forcedsave done, save the version with its URL
          if (LOG.isDebugEnabled()) {
            LOG.debug("Received Onlyoffice forced saved document. Key: " + key + ". Users: " + Arrays.toString(status.getUsers())
                + ". Document " + nodePath + ". URL: " + status.getUrl() + ". Download: " + status.getUserdata().getDownload());
          }
          if (status.getUserdata().getDownload()) {
            downloadVersion(status.getUserdata(), status.getUrl());
          } else {
            saveLink(status.getUserdata(), status.getUrl());
          }

        } else if (statusCode == 7) {
          // forcedsave error, we may decide next step according
          // status.getError()
          // TODO more precise error handling:
          // 0 No errors.
          // 1 Document key is missing or no document with such key could be
          // found.
          // 2 Callback url not correct.
          // 3 Internal server error.
          // 4 No changes were applied to the document before the forcesave
          // command was received.
          // 5 Command not correct.
          // 6 Invalid token.
          LOG.error("Received Onlyoffice error of forced saving of document. Key: " + key + ". Users: "
              + Arrays.toString(status.getUsers()) + ". Document: " + nodePath + ". Error: " + status.getError() + ". URL: "
              + status.getUrl() + ". Download: " + status.getUserdata().getDownload());
        } else {
          // warn unexpected status, wait for next status
          LOG.warn("Received Onlyoffice unexpected status. Key: " + key + ". URL: " + status.getUrl() + ". Users: "
              + status.getUsers() + ". Document: " + nodePath);
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
    if (node.getPrimaryNodeType().getName().equals("exo:symlink")) {
      node = (Node) finder.findItem(node.getSession(), node.getPath());
    }
    if (node.canAddMixin("mix:referenceable") && canEditDocument(node)) {
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
  public String getDocumentId(Node node) throws OnlyofficeEditorException, RepositoryException {
    if (node.isNodeType("mix:referenceable")) {
      return node.getUUID();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEditorLink(Node node) throws RepositoryException, OnlyofficeEditorException {
    if (canEditDocument(node)) {
      String workspace = node.getSession().getWorkspace().getName();
      String docId = initDocument(node);
      PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
      String link = getEditorLink(pcontext.getRequest().getScheme(),
                                  pcontext.getRequest().getServerName(),
                                  pcontext.getRequest().getServerPort(),
                                  workspace,
                                  docId);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Editor link {}: {}", node.getPath(), link);
      }
      return link;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Editor link NOT FOUND for {}", node.getPath());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEditorLink(String schema, String host, int port, String workspace, String docId) {
    return platformUrl(schema, host, port).append(editorURLPath(docId)).toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getDocumentById(String workspace, String uuid) throws RepositoryException {
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
   * {@inheritDoc}
   */
  @Override
  public Node getDocument(String workspace, String path) throws RepositoryException, BadParameterException {
    if (workspace == null) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    try {
      return node(workspace, path);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTypePlugin(ComponentPlugin plugin) {
    Class<DocumentTypePlugin> pclass = DocumentTypePlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      documentTypePlugin = pclass.cast(plugin);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Set documentTypePlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The documentTypePlugin plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canEditDocument(Node node) throws RepositoryException {
    boolean res = false;
    if (node != null) {
      if (isDocumentMimeSupported(node)) {
        String remoteUser = WCMCoreUtils.getRemoteUser();
        String superUser = WCMCoreUtils.getSuperUser();
        boolean locked = node.isLocked();
        if (locked && (remoteUser.equalsIgnoreCase(superUser) || node.getLock().getLockOwner().equals(remoteUser))) {
          locked = false;
        }
        res = !locked && PermissionUtil.canSetProperty(node);
      }
    }
    if (!res && LOG.isDebugEnabled()) {
      LOG.debug("Cannot edit: {}", node != null ? node.getPath() : null);
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDocumentMimeSupported(Node node) throws RepositoryException {
    if (node != null) {
      String mimeType;
      if (node.isNodeType(Utils.NT_FILE)) {
        mimeType = node.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString();
      } else {
        mimeType = new MimeTypeResolver().getMimeType(node.getName());
      }
      return documentTypePlugin.getMimeTypes().contains(mimeType);
    }
    return false;
  }

  /**
   * Downloads document's content to the JCR node creating a new version.
   * 
   * @param userdata the userdata
   * @param contentUrl the contentUrl
   */
  @Override
  public void downloadVersion(Userdata userdata, String contentUrl) {
    String docId = null;
    try {
      Config config = getEditorByKey(userdata.getUserId(), userdata.getKey());
      // we set it sooner to let clients see the save
      config.getEditorConfig().getUser().setLastSaved(System.currentTimeMillis());
      docId = config.getDocId();
      DocumentStatus status = new DocumentStatus();
      status.setKey(userdata.getKey());
      status.setUrl(contentUrl);
      status.setUsers(new String[] { userdata.getUserId() });
      download(config, status);
    } catch (OnlyofficeEditorException | RepositoryException e) {
      LOG.error("Error occured while downloading document content [Version]. docId: " + docId, e);
    }
  }

  @Override
  public Editor.User getLastModifier(String key) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    Editor.User lastUser = null;
    if (configs != null) {
      long maxLastModified = 0;
      for (Entry<String, Config> entry : configs.entrySet()) {
        Editor.User user = entry.getValue().getEditorConfig().getUser();
        long lastModified = user.getLastModified();
        if (lastModified > maxLastModified) {
          maxLastModified = lastModified;
          lastUser = user;
        }
      }
    }
    return lastUser;
  }

  @Override
  public void setLastModifier(String key, String userId) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      config.getEditorConfig().getUser().setLastModified(System.currentTimeMillis());
      activeCache.put(key, configs);
      activeCache.put(nodePath(config), configs);
    }
  }

  @Override
  public Editor.User getUser(String key, String userId) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null && configs.containsKey(userId)) {
      return configs.get(userId).getEditorConfig().getUser();
    }
    return null;
  }

  @Override
  public void forceSave(Userdata userdata) {
    HttpURLConnection connection = null;
    try {
      String json = new JSONObject().put("c", "forcesave")
                                    .put("key", userdata.getKey())
                                    .put("userdata", userdata.toJSON())
                                    .toString();
      byte[] postDataBytes = json.toString().getBytes("UTF-8");

      URL url = new URL(commandServiceUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
      connection.setDoOutput(true);
      connection.setDoInput(true);
      try (OutputStream outputStream = connection.getOutputStream()) {
        outputStream.write(postDataBytes);
      }
      // read the response
      InputStream in = new BufferedInputStream(connection.getInputStream());
      String response = IOUtils.toString(in, "UTF-8");
      LOG.debug("Command service responded on forcesave command: " + response);
    } catch (Exception e) {
      LOG.error("Error in sending forcesave command. UserId: " + userdata.getUserId() + ". Key: " + userdata.getKey()
          + ". Download: " + userdata.getDownload(), e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  // *********************** implementation level ***************

  protected void saveLink(Userdata userdata, String url) {
    ConcurrentMap<String, Config> configs = activeCache.get(userdata.getKey());
    if (configs != null) {
      Config config = configs.get(userdata.getUserId());
      config.getEditorConfig().getUser().setDownloadLink(url);
      config.getEditorConfig().getUser().setLinkSaved(System.currentTimeMillis());
      activeCache.put(userdata.getKey(), configs);
      activeCache.put(nodePath(config), configs);
    }
  }

  /**
   * Downloads document's content to the JCR node when the editor is closed.
   * 
   * @param config the config
   * @param status the status
   */
  protected void downloadClosed(Config config, DocumentStatus status) {
    // First mark closing, then do actual download and save in storage
    config.closing();
    try {
      download(config, status);
      config.getEditorConfig().getUser().setLastSaved(System.currentTimeMillis());
      config.closed(); // reset transient closing state
    } catch (OnlyofficeEditorException | RepositoryException e) {
      LOG.error("Error occured while downloading document content [Closed]. docId: " + config.getDocId(), e);
    }
  }

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
   * @param port the port
   * @param workspace the workspace
   * @param path the path
   * @return the string
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  @Deprecated
  protected String webdavUrl(String schema,
                             String host,
                             int port,
                             String workspace,
                             String path) throws OnlyofficeEditorException, RepositoryException {
    StringBuilder filePath = new StringBuilder();
    try {
      URI baseWebdavUri = webdavUri(schema, host, port);

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
      DocumentStatus status = new DocumentStatus();
      status.setConfig(config);
      status.setKey(config.getDocument().getKey());
      status.setUrl(config.getEditorUrl());
      status.setUsers(users);
      if (editors.contains(user)) {
        if (config.isCreated() || config.isClosed()) {
          // editor was (re)opened by user
          config.open();
          fireJoined(status);
          updated = true;
        }
      } else {
        // editor was closed by user: it will be closing if closed via WebUI of
        // ECMS explorer, open in general case
        if (config.isClosing() || config.isOpen()) {
          // closed because user sync happens when someone else still editing or
          // nothing edited
          config.closed();
          fireLeaved(status);
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
   * Downloads document's content to the JCR node.
   * 
   * @param config the config
   * @param status the status
   * @throws OnlyofficeEditorException the OnlyofficeEditorException
   * @throws RepositoryException the RepositoryException
   */
  protected void download(Config config, DocumentStatus status) throws OnlyofficeEditorException, RepositoryException {
    String workspace = config.getWorkspace();
    String path = config.getPath();
    String nodePath = nodePath(workspace, path);

    if (LOG.isDebugEnabled()) {
      LOG.debug(">> download(" + nodePath + ", " + config.getDocument().getKey() + ")");
    }

    // Assuming a single user here (last modifier)
    String userId = status.getLastUser();
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
      final LockState lock = lock(node, config);
      if (lock.canEdit()) {
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

          status.setConfig(config);
          fireSaved(status);
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
            if (node.isLocked() && lock.wasLocked()) {
              unlock(node, lock);
            }
          } catch (Throwable e) {
            LOG.warn("Error unlocking edited document " + nodePath(workspace, path), e);
          }
        }
      } else {
        throw new OnlyofficeEditorException("Document locked " + nodePath);
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
   * Unlock given node.
   *
   * @param node the node
   * @param lock the lock
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected void unlock(Node node, LockState lock) throws OnlyofficeEditorException, RepositoryException {
    node.unlock();
    try {
      LockUtil.removeLock(node);
    } catch (Exception e) {
      if (RepositoryException.class.isAssignableFrom(e.getClass())) {
        throw RepositoryException.class.cast(e);
      } else {
        throw new OnlyofficeEditorException("Error removing document lock", e);
      }
    }
  }

  /**
   * Lock the node by current user. If lock attempts will succeed in predefined
   * time this method will throw {@link OnlyofficeEditorException}. If node
   * isn't mix:lockable it will be added first and node saved.
   *
   * @param node {@link Node}
   * @param config {@link Config}
   * @return {@link Lock} acquired by current user.
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected LockState lock(Node node, Config config) throws OnlyofficeEditorException, RepositoryException {
    if (!node.isNodeType("mix:lockable")) {
      if (!node.isCheckedOut() && node.isNodeType("mix:versionable")) {
        node.checkout();
      }
      node.addMixin("mix:lockable");
      node.save();
    }

    Config.Editor.User user = config.getEditorConfig().getUser();
    LockState lock;
    int attempts = 0;
    try {
      do {
        attempts++;
        if (node.isLocked()) {
          String lockToken;
          try {
            lockToken = LockUtil.getLockToken(node);
          } catch (Exception e) {
            if (RepositoryException.class.isAssignableFrom(e.getClass())) {
              throw RepositoryException.class.cast(e);
            } else {
              throw new OnlyofficeEditorException("Error reading document lock", e);
            }
          }
          if (node.getLock().getLockOwner().equals(user.getId()) && lockToken != null) {
            // already this user lock
            node.getSession().addLockToken(lockToken);
            lock = new LockState(lockToken);
          } else {
            // need wait for unlock
            Thread.sleep(LOCK_WAIT_TIMEOUT);
            lock = null;
          }
        } else {
          Lock jcrLock = node.lock(true, false); // TODO deep vs only file node
                                                 // lock?
          // keep lock token for other sessions of same user
          try {
            LockUtil.keepLock(jcrLock, user.getId(), jcrLock.getLockToken());
          } catch (Exception e) {
            if (RepositoryException.class.isAssignableFrom(e.getClass())) {
              throw RepositoryException.class.cast(e);
            } else {
              throw new OnlyofficeEditorException("Error saving document lock", e);
            }
          }
          lock = new LockState(jcrLock);
        }
      } while (lock == null && attempts <= LOCK_WAIT_ATTEMTS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OnlyofficeEditorException("Error waiting for lock of " + nodePath(config.getWorkspace(), config.getPath()), e);
    }
    return lock == null ? new LockState() : lock;
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
   * @param port the port
   * @return the string builder
   */
  protected StringBuilder platformUrl(String schema, String host, int port) {
    StringBuilder platformUrl = new StringBuilder();
    platformUrl.append(schema);
    platformUrl.append("://");
    platformUrl.append(host);
    if (port >= 0 && port != 80 && port != 443) {
      platformUrl.append(':');
      platformUrl.append(port);
    }
    platformUrl.append('/');
    platformUrl.append(PortalContainer.getCurrentPortalContainerName());

    return platformUrl;
  }

  /**
   * ECMS explorer page URL.
   *
   * @param schema the schema
   * @param host the host
   * @param port the port
   * @param ecmsURL the ECMS URL
   * @return the string builder
   */
  @Deprecated
  protected StringBuilder explorerUrl(String schema, String host, int port, String ecmsURL) {
    StringBuilder explorerUrl = new StringBuilder();
    explorerUrl.append(schema);
    explorerUrl.append("://");
    explorerUrl.append(host);
    if (port >= 0 && port != 80 && port != 443) {
      explorerUrl.append(':');
      explorerUrl.append(port);
    }
    explorerUrl.append(ecmsURL);
    return explorerUrl;
  }

  protected URI explorerUri(String schema, String host, int port, String ecmsLink) {
    URI uri;
    try {
      String[] linkParts = ecmsLink.split("\\?");
      if (linkParts.length >= 2) {
        uri = new URI(schema, null, host, port, linkParts[0], linkParts[1], null);
      } else {
        uri = new URI(schema, null, host, port, ecmsLink, null, null);
      }
    } catch (Exception e) {
      LOG.warn("Error creating document URI", e);
      try {
        uri = URI.create(ecmsLink);
      } catch (Exception e1) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Error creating document URI from ECMS link and after error: " + e.getMessage(), e1);
        }
        uri = null;
      }
    }
    return uri;
  }

  /**
   * ECMS explorer page relative URL (within the Platform).
   *
   * @param jcrPath the jcr path
   * @return the string
   */
  protected String explorerLink(String jcrPath) {
    try {
      return documentService.getLinkInDocumentsApp(jcrPath);
    } catch (Exception e) {
      LOG.warn("Error creating document link for " + jcrPath, e);
      return new StringBuilder().append('/').append(PortalContainer.getCurrentPortalContainerName()).toString();
    }
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
   * @param port the port
   * @return the string builder
   */
  @Deprecated
  protected StringBuilder platformRestUrl(String schema, String host, int port) {
    return platformRestUrl(platformUrl(schema, host, port));
  }

  /**
   * Webdav uri.
   *
   * @param schema the schema
   * @param host the host
   * @param port the port
   * @return the uri
   * @throws URISyntaxException the URI syntax exception
   */
  @Deprecated
  protected URI webdavUri(String schema, String host, int port) throws URISyntaxException {
    return new URI(platformRestUrl(schema, host, port).append("/jcr").toString());
  }

  /**
   * Fire created.
   *
   * @param status the status
   */
  protected void fireCreated(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onCreate(status);
      } catch (Throwable t) {
        LOG.warn("Creation listener error", t);
      }
    }
  }

  /**
   * Fire get.
   *
   * @param status the status
   */
  protected void fireGet(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onGet(status);
      } catch (Throwable t) {
        LOG.warn("Read (Get) listener error", t);
      }
    }
  }

  /**
   * Fire joined.
   *
   * @param status the status
   */
  protected void fireJoined(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onJoined(status);
      } catch (Throwable t) {
        LOG.warn("User joining listener error", t);
      }
    }
  }

  /**
   * Fire leaved.
   *
   * @param status the status
   */
  protected void fireLeaved(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onLeaved(status);
      } catch (Throwable t) {
        LOG.warn("User leaving listener error", t);
      }
    }
  }

  /**
   * Fire saved.
   *
   * @param status the status
   */
  protected void fireSaved(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onSaved(status);
      } catch (Throwable t) {
        LOG.warn("Saving listener error", t);
      }
    }
  }

  /**
   * Fire error.
   *
   * @param status the status
   */
  protected void fireError(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onError(status);
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
