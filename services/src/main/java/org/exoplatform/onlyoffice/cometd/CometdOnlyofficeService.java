/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.onlyoffice.cometd;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.cometd.annotation.Param;
import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.annotation.Subscription;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.eclipse.jetty.util.component.LifeCycle;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.onlyoffice.Config.Editor;
import org.exoplatform.onlyoffice.DocumentStatus;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorListener;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The CometdOnlyofficeService.
 */
public class CometdOnlyofficeService implements Startable {

  /** The Constant LOG. */
  private static final Log                LOG                        = ExoLogger.getLogger(CometdOnlyofficeService.class);

  /** The channel name. */
  public static final String              CHANNEL_NAME               = "/eXo/Application/Onlyoffice/editor/";

  /** The channel name. */
  public static final String              CHANNEL_NAME_PARAMS        = CHANNEL_NAME + "{docId}";

  /** The document saved event. */
  public static final String              DOCUMENT_SAVED_EVENT       = "DOCUMENT_SAVED";

  /** The document changed event. */
  public static final String              DOCUMENT_CHANGED_EVENT     = "DOCUMENT_CHANGED";

  /** The document version event. */
  public static final String              DOCUMENT_VERSION_EVENT     = "DOCUMENT_VERSION";

  /** The editor closed event. */
  public static final String              EDITOR_CLOSED_EVENT        = "EDITOR_CLOSED";


  /** The Constant SAME_USER_VERSION_LIFETIME. */
  public static final long                SAME_USER_VERSION_LIFETIME = 10 * 60 * 1000;

  /** The Constant SAME_USER_VERSION_SKIPTIME. */
  public static final long                SAME_USER_VERSION_SKIPTIME = 5 * 1000;

  /** The Onlyoffice editor service. */
  protected final OnlyofficeEditorService editors;

  /** The exo bayeux. */
  protected final EXoContinuationBayeux   exoBayeux;

  /** The service. */
  protected final CometdService           service;

  /**
   * Instantiates the CometdOnlyofficeService.
   *
   * @param exoBayeux the exoBayeux
   * @param onlyofficeEditorService the onlyoffice editor service
   */
  public CometdOnlyofficeService(EXoContinuationBayeux exoBayeux, OnlyofficeEditorService onlyofficeEditorService) {
    this.exoBayeux = exoBayeux;
    this.editors = onlyofficeEditorService;
    this.service = new CometdService();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // instantiate processor after the eXo container start, to let
    // start-dependent logic worked before us
    final AtomicReference<ServerAnnotationProcessor> processor = new AtomicReference<>();
    // need initiate process after Bayeux server starts
    exoBayeux.addLifeCycleListener(new LifeCycle.Listener() {
      @Override
      public void lifeCycleStarted(LifeCycle event) {
        ServerAnnotationProcessor p = new ServerAnnotationProcessor(exoBayeux);
        processor.set(p);
        p.process(service);
      }

      @Override
      public void lifeCycleStopped(LifeCycle event) {
        ServerAnnotationProcessor p = processor.get();
        if (p != null) {
          p.deprocess(service);
        }
      }

      @Override
      public void lifeCycleStarting(LifeCycle event) {
        // Nothing
      }

      @Override
      public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        // Nothing
      }

      @Override
      public void lifeCycleStopping(LifeCycle event) {
        // Nothing
      }
    });

    if (PropertyManager.isDevelopping()) {
      // This listener not required for work, just for info during development
      exoBayeux.addListener(new BayeuxServer.SessionListener() {
        @Override
        public void sessionRemoved(ServerSession session, boolean timedout) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("sessionRemoved: " + session.getId() + " timedout:" + timedout + " channels: "
                + channelsAsString(session.getSubscriptions()));
          }
        }

        @Override
        public void sessionAdded(ServerSession session, ServerMessage message) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("sessionAdded: " + session.getId() + " channels: " + channelsAsString(session.getSubscriptions()));
          }
        }
      });
    }
  }


  /**
   * The CometService is responsible for sending messages to Cometd channels
   * when a document is saved.
   */
  @Service("onlyoffice")
  public class CometdService {

    /** The bayeux. */
    @Inject
    private BayeuxServer  bayeux;

    /** The local session. */
    @Session
    private LocalSession  localSession;

    /** The server session. */
    @Session
    private ServerSession serverSession;

    /**
     * Post construct.
     */
    @PostConstruct
    public void postConstruct() {
      editors.addListener(new OnlyofficeEditorListener() {

        @Override
        public void onSaved(DocumentStatus status) {
          publishSavedEvent(status.getConfig().getDocId(), status.getLastUser());
        }

        @Override
        public void onLeaved(DocumentStatus status) {
          // Nothing
        }

        @Override
        public void onJoined(DocumentStatus status) {
          // Nothing
        }

        @Override
        public void onGet(DocumentStatus status) {
          // Nothing
        }

        @Override
        public void onError(DocumentStatus status) {
          // Nothing
        }

        @Override
        public void onCreate(DocumentStatus status) {
          // Nothing
        }
      });
    }

    /**
     * Subscribe document events.
     *
     * @param message the message.
     * @param docId the docId.
     * @throws OnlyofficeEditorException the onlyoffice editor exception
     * @throws RepositoryException the repository exception
     */
    @Subscription(CHANNEL_NAME_PARAMS)
    public void subscribeDocuments(Message message, @Param("docId") String docId) throws OnlyofficeEditorException,
                                                                                  RepositoryException {
      Object objData = message.getData();
      if (!Map.class.isInstance(objData)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Couldn't get data as a map from event");
        }
        return;
      }

      Map<String, Object> data = message.getDataAsMap();
      String type = (String) data.get("type");

      switch (type) {
      case DOCUMENT_CHANGED_EVENT:
        handleDocumentChangeEvent(data, docId);
        break;
      case DOCUMENT_VERSION_EVENT:
        handleDocumentVersionEvent(data, docId);
        break;
      case EDITOR_CLOSED_EVENT:
        handleEditorClosedEvent(data, docId);
        break;
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Event published in " + message.getChannel() + ", docId: " + docId + ", data: " + message.getJSON());
      }

    }


    /**
     * Handle editor closed event.
     *
     * @param data the data
     * @param docId the doc id
     */
    protected void handleEditorClosedEvent(Map<String, Object> data, String docId) {
      String userId = (String) data.get("userId");
      String key = (String) data.get("key");
      editors.forceDownload(userId, key);
    }

    /**
     * Handle document version event.
     *
     * @param data the data
     * @param docId the doc id
     */
    protected void handleDocumentVersionEvent(Map<String, Object> data, String docId) {
      String userId = (String) data.get("userId");
      String key = (String) data.get("key");
      Editor.User lastUser = editors.getLastModifier(key);
      if (LOG.isDebugEnabled()) {
        if (lastUser != null) {
          LOG.debug("Handle document version: {} for {}, lastUser: {}:{}",
                    userId,
                    docId,
                    lastUser.getId(),
                    lastUser.getLastSaved());
        } else {
          LOG.debug("Handle document version: {} for {}, lastUser: null", userId, docId);
        }
      }
      if (lastUser != null && lastUser.getId().equals(userId) && lastUser.getLastSaved() > 0
          && System.currentTimeMillis() - lastUser.getLastSaved() <= SAME_USER_VERSION_SKIPTIME) {
        // XXX We don't want save same user version too often (case for many
        // open editors by same user of same doc)
        if (LOG.isDebugEnabled()) {
          LOG.debug("Skipped same user version: {} for {}", userId, docId);
        }
        return;
      }
      
      editors.forceDownload(userId, key);
      
    }

    /**
     * Handle document change event.
     *
     * @param data the data
     * @param docId the doc id
     */
    protected void handleDocumentChangeEvent(Map<String, Object> data, String docId) {
      String userId = (String) data.get("userId");
      String key = (String) data.get("key");
      Editor.User lastUser = editors.getLastModifier(key);
      if (lastUser != null // TODO reconsider and cleanup
          && (!userId.equals(lastUser.getId()) /*
                                                * || (lastUser.getLastSaved() >
                                                * 0 &&
                                                * lastUser.getLastModified() > 0
                                                * && lastUser.getLastModified()
                                                * - lastUser.getLastSaved() >
                                                * SAME_USER_VERSION_LIFETIME &&
                                                * System.currentTimeMillis() -
                                                * lastUser.getLastModified() >
                                                * SAME_USER_VERSION_LIFETIME)
                                                */)) {
        // We download user version if another user started to change the
        // document or enough time passed since previous change by this user.
        editors.forceDownload(lastUser.getId(), key);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Download a new version of document: user " + lastUser.getId() + ", docId: " + docId);
          LOG.debug("Started collecting changes for: " + userId + ", docId: " + docId);
        }
      }
      editors.setLastModifier(key, userId);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Changes collected from: " + userId + ", docId: " + docId);
      }
    }


    /**
     * Publish saved event.
     *
     * @param docId the doc id
     * @param userId the user id
     */
    protected void publishSavedEvent(String docId, String userId) {
      ServerChannel channel = bayeux.getChannel(CHANNEL_NAME + docId);
      if (channel != null) {
        StringBuilder data = new StringBuilder();
        data.append('{');
        data.append("\"type\": \"");
        data.append(DOCUMENT_SAVED_EVENT);
        data.append("\", ");
        data.append("\"docId\": \"");
        data.append(docId);
        data.append("\", ");
        data.append("\"userId\": \"");
        data.append(userId);
        data.append("\"");
        data.append('}');
        channel.publish(localSession, data.toString());
      }

    }
  }

  /**
   * Channels as string.
   *
   * @param channels the channels
   * @return the string
   */
  protected String channelsAsString(Set<ServerChannel> channels) {
    return channels.stream().map(c -> c.getId()).collect(Collectors.joining(", "));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // Nothing
  }

  /**
   * Gets the cometd server path.
   *
   * @return the cometd server path
   */
  public String getCometdServerPath() {
    return new StringBuilder("/").append(exoBayeux.getCometdContextName()).append("/cometd").toString();
  }

  /**
   * Gets the user token.
   *
   * @param userId the userId
   * @return the token
   */
  public String getUserToken(String userId) {
    return exoBayeux.getUserToken(userId);
  }

}
