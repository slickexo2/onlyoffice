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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import org.exoplatform.onlyoffice.DocumentStatus;
import org.exoplatform.onlyoffice.OnlyofficeEditorListener;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The CometdOnlyofficeService.
 */
public class CometdOnlyofficeService implements Startable {

  /** The Constant LOG. */
  private static final Log                LOG                    = ExoLogger.getLogger(CometdOnlyofficeService.class);

  /** The channel name. */
  public static final String              CHANNEL_NAME           = "/eXo/Application/Onlyoffice/editor/";

  /** The channel name. */
  public static final String              CHANNEL_NAME_PARAMS    = CHANNEL_NAME + "{docId}";

  /** The document saved event. */
  public static final String              DOCUMENT_SAVED_EVENT   = "DOCUMENT_SAVED";

  /** The document changed event. */
  public static final String              DOCUMENT_CHANGED_EVENT = "DOCUMENT_CHANGED";

  /** The Onlyoffice editor service. */
  protected final OnlyofficeEditorService onlyofficeEditorService;

  /** The exo bayeux. */
  protected final EXoContinuationBayeux   exoBayeux;

  /** The service. */
  protected final CometdService           service;

  /**
   * Instantiates the CometdOnlyofficeService.
   *
   * @param exoBayeux the exoBayeux
   * @param onlyofficeEditorService the OnlyofficeEditorService
   */
  public CometdOnlyofficeService(EXoContinuationBayeux exoBayeux, OnlyofficeEditorService onlyofficeEditorService) {
    this.exoBayeux = exoBayeux;
    this.onlyofficeEditorService = onlyofficeEditorService;
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
    private BayeuxServer        bayeux;

    /** The local session. */
    @Session
    private LocalSession        localSession;

    /** The server session. */
    @Session
    private ServerSession       serverSession;

    /** The documentUpdates map contains data about document updates. K - docId, V - last userId. */
    private Map<String, String> documentUpdates = new ConcurrentHashMap<>();

    /**
     * Post construct.
     */
    @PostConstruct
    public void postConstruct() {
      onlyofficeEditorService.addListener(new OnlyofficeEditorListener() {

        @Override
        public void onSaved(DocumentStatus status) {
          String docId = status.getConfig().getDocId();
          ServerChannel channel = bayeux.getChannel(CHANNEL_NAME + docId);
          if (channel != null) {
            LOG.info("Document {} saved. Sending message to cometd channel", docId);
            StringBuilder data = new StringBuilder();
            data.append('{');
            data.append("\"type\": \"");
            data.append(DOCUMENT_SAVED_EVENT);
            data.append("\", ");
            data.append("\"docId\": \"");
            data.append(docId);
            data.append("\", ");
            data.append("\"userId\": \"");
            data.append(status.getLastUser());
            data.append("\"");
            data.append('}');
            channel.publish(localSession, data.toString());
          }
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
     */
    @Subscription(CHANNEL_NAME_PARAMS)
    public void subscribeDocuments(Message message, @Param("docId") String docId) {
      Map<String, Object> data = message.getDataAsMap();
      String type = (String) data.get("type");
      String userId = (String) data.get("userId");
      if (DOCUMENT_CHANGED_EVENT.equals(type)) {
        String lastUser = documentUpdates.computeIfAbsent(docId, id -> userId);
        if (!userId.equals(lastUser)) {
          LOG.info("Create document version for " + lastUser + ", docId: " + docId);
          // TODO create new version for lastUser
          documentUpdates.replace(docId, userId);
          LOG.info("Started collecting changes for: " + userId + ", docId: " + docId);
        }
        LOG.info("Changes collected from: " + userId + ", docId: " + docId);
      }
      LOG.info("Event published in " + message.getChannel() + ", docId: " + docId + ", data: " + message.getJSON());
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
