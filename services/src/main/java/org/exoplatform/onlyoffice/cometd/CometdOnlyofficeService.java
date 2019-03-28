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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
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

  /**
   * Command thread factory adapted from {@link Executors#DefaultThreadFactory}.
   */
  static class CommandThreadFactory implements ThreadFactory {

    /** The group. */
    final ThreadGroup   group;

    /** The thread number. */
    final AtomicInteger threadNumber = new AtomicInteger(1);

    /** The name prefix. */
    final String        namePrefix;

    /**
     * Instantiates a new command thread factory.
     *
     * @param namePrefix the name prefix
     */
    CommandThreadFactory(String namePrefix) {
      SecurityManager s = System.getSecurityManager();
      this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      this.namePrefix = namePrefix;
    }

    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0) {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void finalize() throws Throwable {
          super.finalize();
          threadNumber.decrementAndGet();
        }

      };
      if (t.isDaemon()) {
        t.setDaemon(false);
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }

  /**
   * The Class ContainerCommand.
   */
  abstract class ContainerCommand implements Runnable {

    /** The container name. */
    final String containerName;

    /**
     * Instantiates a new container command.
     *
     * @param containerName the container name
     */
    ContainerCommand(String containerName) {
      this.containerName = containerName;
    }

    /**
     * Execute actual work of the commend (in extending class).
     *
     * @param exoContainer the exo container
     */
    abstract void execute(ExoContainer exoContainer);

    /**
     * Callback to execute on container error.
     *
     * @param error the error
     */
    abstract void onContainerError(String error);

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      // Do the work under eXo container context (for proper work of eXo apps
      // and JPA storage)
      ExoContainer exoContainer = ExoContainerContext.getContainerByName(containerName);
      if (exoContainer != null) {
        ExoContainer contextContainer = ExoContainerContext.getCurrentContainerIfPresent();
        try {
          // Container context
          ExoContainerContext.setCurrentContainer(exoContainer);
          RequestLifeCycle.begin(exoContainer);
          // do the work here
          execute(exoContainer);
        } finally {
          // Restore context
          RequestLifeCycle.end();
          ExoContainerContext.setCurrentContainer(contextContainer);
        }
      } else {
        // LOG.warn("Container not found " + containerName + " for remote call "
        // + contextName);
        onContainerError("Container not found");
      }

    }
  }

  /** The Constant LOG. */
  private static final Log                LOG                     = ExoLogger.getLogger(CometdOnlyofficeService.class);

  /** The channel name. */
  public static final String              CHANNEL_NAME            = "/eXo/Application/Onlyoffice/editor/";

  /** The channel name. */
  public static final String              CHANNEL_NAME_PARAMS     = CHANNEL_NAME + "{docId}";

  /** The document saved event. */
  public static final String              DOCUMENT_SAVED_EVENT    = "DOCUMENT_SAVED";

  /** The document changed event. */
  public static final String              DOCUMENT_CHANGED_EVENT  = "DOCUMENT_CHANGED";

  /** The document download event. */
  public static final String              DOCUMENT_DOWNLOAD_EVENT = "DOCUMENT_DOWNLOAD";

  /** The document version event. */
  public static final String              DOCUMENT_VERSION_EVENT  = "DOCUMENT_VERSION";

  /**
   * Base minimum number of threads for remote calls' thread executors.
   */
  public static final int                 MIN_THREADS             = 2;

  /**
   * Minimal number of threads maximum possible for remote calls' thread executors.
   */
  public static final int                 MIN_MAX_THREADS         = 4;

  /** Thread idle time for thread executors (in seconds). */
  public static final int                 THREAD_IDLE_TIME        = 120;

  /**
   * Maximum threads per CPU for thread executors of user call channel.
   */
  public static final int                 CALL_MAX_FACTOR         = 20;

  /**
   * Queue size per CPU for thread executors of user call channel.
   */
  public static final int                 CALL_QUEUE_FACTOR       = CALL_MAX_FACTOR * 2;

  /**
   * Thread name used for calls executor.
   */
  public static final String              CALL_THREAD_PREFIX      = "onlyoffice-comet-thread-";

  /** The Onlyoffice editor service. */
  protected final OnlyofficeEditorService onlyofficeEditorService;

  /** The exo bayeux. */
  protected final EXoContinuationBayeux   exoBayeux;

  /** The service. */
  protected final CometdService           service;

  /** The call handlers. */
  protected final ExecutorService         eventsHandlers;

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
    // Thread executors
    this.eventsHandlers = createThreadExecutor(CALL_THREAD_PREFIX, CALL_MAX_FACTOR, CALL_QUEUE_FACTOR);
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
   * Create a new thread executor service.
   *
   * @param threadNamePrefix the thread name prefix
   * @param maxFactor - max processes per CPU core
   * @param queueFactor - queue size per CPU core
   * @return the executor service
   */
  protected ExecutorService createThreadExecutor(String threadNamePrefix, int maxFactor, int queueFactor) {
    // Executor will queue all commands and run them in maximum set of threads.
    // Minimum set of threads will be
    // maintained online even idle, other inactive will be stopped in two
    // minutes.
    final int cpus = Runtime.getRuntime().availableProcessors();
    int poolThreads = cpus / 4;
    poolThreads = poolThreads < MIN_THREADS ? MIN_THREADS : poolThreads;
    int maxThreads = Math.round(cpus * 1f * maxFactor);
    maxThreads = maxThreads > 0 ? maxThreads : 1;
    maxThreads = maxThreads < MIN_MAX_THREADS ? MIN_MAX_THREADS : maxThreads;
    int queueSize = cpus * queueFactor;
    queueSize = queueSize < queueFactor ? queueFactor : queueSize;
    if (LOG.isDebugEnabled()) {
      LOG.debug("Creating thread executor " + threadNamePrefix + "* for " + poolThreads + ".." + maxThreads
          + " threads, queue size " + queueSize);
    }
    return new ThreadPoolExecutor(poolThreads,
                                  maxThreads,
                                  THREAD_IDLE_TIME,
                                  TimeUnit.SECONDS,
                                  new LinkedBlockingQueue<Runnable>(queueSize),
                                  new CommandThreadFactory(threadNamePrefix),
                                  new ThreadPoolExecutor.CallerRunsPolicy());
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
     * @throws RepositoryException 
     * @throws OnlyofficeEditorException 
     */
    @Subscription(CHANNEL_NAME_PARAMS)
    public void subscribeDocuments(Message message, @Param("docId") String docId) throws OnlyofficeEditorException,
                                                                                  RepositoryException {
      Map<String, Object> data = message.getDataAsMap();
      String type = (String) data.get("type");
      if (DOCUMENT_CHANGED_EVENT.equals(type)) {
        handleDocumentChangeEvent(data, docId);
      } else if (DOCUMENT_VERSION_EVENT.equals(type)) {
        handleDocumentVersionEvent(data, docId);
      }
      LOG.info("Event published in " + message.getChannel() + ", docId: " + docId + ", data: " + message.getJSON());
    }

    protected void handleDocumentVersionEvent(Map<String, Object> data, String docId) {
      String userId = (String) data.get("userId");
      String key = (String) data.get("key");
      String link = (String) data.get("documentLink");
      eventsHandlers.submit(new ContainerCommand(PortalContainer.getCurrentPortalContainerName()) {
        @Override
        void onContainerError(String error) {
          LOG.error("An error has occured in container: {}", containerName);
        }

        @Override
        void execute(ExoContainer exoContainer) {
          try {
            LOG.info("Creating a new version for user: " + userId + ", docId: " + docId);
            onlyofficeEditorService.createVersion(key, userId, link);
          } catch (OnlyofficeEditorException | RepositoryException e) {
            LOG.error("Cannot create version :", e);
          }
        }
      });

    }

    protected void handleDocumentChangeEvent(Map<String, Object> data, String docId) {
      String userId = (String) data.get("userId");
      String lastUser = documentUpdates.computeIfAbsent(docId, id -> userId);
      if (!userId.equals(lastUser)) {
        LOG.info("Download a new version of document: user " + lastUser + ", docId: " + docId);
        publishDownloadEvent(docId, lastUser);
        documentUpdates.replace(docId, userId);
        LOG.info("Started collecting changes for: " + userId + ", docId: " + docId);
      }
      LOG.info("Changes collected from: " + userId + ", docId: " + docId);
    }

    protected void publishDownloadEvent(String docId, String userId) {
      ServerChannel channel = bayeux.getChannel(CHANNEL_NAME + docId);
      if (channel != null) {
        LOG.info("Document {} saved. Sending message to cometd channel", docId);
        StringBuilder data = new StringBuilder();
        data.append('{');
        data.append("\"type\": \"");
        data.append(DOCUMENT_DOWNLOAD_EVENT);
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
