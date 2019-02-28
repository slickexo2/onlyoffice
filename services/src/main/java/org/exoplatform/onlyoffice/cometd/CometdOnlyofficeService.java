package org.exoplatform.onlyoffice.cometd;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.eclipse.jetty.util.component.LifeCycle;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorListener;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CometdOnlyofficeService implements Startable {

  /** The Constant LOG. */
  private static final Log                LOG                    = ExoLogger.getLogger(CometdOnlyofficeService.class);

  public static final String              CHANNEL_NAME           = "/eXo/Application/Onlyoffice/editor/";

  public static final String              DOCUMENT_UPDATED_EVENT = "document_updated";

  /** The exo bayeux. */
  protected final EXoContinuationBayeux   exoBayeux;

  /** The exo bayeux. */
  protected final OnlyofficeEditorService onlyofficeService;

  /** The service. */
  protected final CometdService           service;

  public CometdOnlyofficeService(EXoContinuationBayeux exoBayeux, OnlyofficeEditorService onlyofficeService) {
    this.exoBayeux = exoBayeux;
    this.onlyofficeService = onlyofficeService;
    this.service = new CometdService();
  }

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
   * The CometService is responsible for sending messages to Cometd channels when a document is saved
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

    @PostConstruct
    public void postConstruct() {
      onlyofficeService.addListener(new OnlyofficeEditorListener() {
        @Override
        public void onSaved(Config config) {
          String docId = config.getDocId();
          ServerChannel channel = bayeux.getChannel(CHANNEL_NAME + docId);

          if (channel != null) {
            LOG.info("Document {} saved. Sending message to cometd channel", config.getDocId());
            StringBuilder data = new StringBuilder();
            data.append('{');
            data.append("\"eventType\": \"");
            data.append(DOCUMENT_UPDATED_EVENT);
            data.append("\", ");
            data.append("\"documentId\": \"");
            data.append(docId);
            data.append("\"");
            data.append('}');
            channel.publish(localSession, data.toString());
          }
        }

        @Override
        public void onLeaved(Config config) {
          // Nothing
        }

        @Override
        public void onJoined(Config config) {
          // Nothing
        }

        @Override
        public void onGet(Config config) {
          // Nothing
        }

        @Override
        public void onError(Config config) {
          // Nothing
        }

        @Override
        public void onCreate(Config config) {
          // Nothing
        }
      });
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

  @Override
  public void stop() {
    // TODO Auto-generated method stub

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
   * Gets the cometd server path.
   *
   * @return the cometd server path
   */
  public String getUserToken(String userId) {
    return exoBayeux.getUserToken(userId);
  }

}
