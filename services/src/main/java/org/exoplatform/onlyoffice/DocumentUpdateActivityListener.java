package org.exoplatform.onlyoffice;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;

import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.ext.component.activity.listener.FileUpdateActivityListener;

// TODO: Auto-generated Javadoc
/**
 * The listener interface for receiving documentUpdateActivity events.
 * The class that is interested in processing a documentUpdateActivity
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addDocumentUpdateActivityListener<code> method. When
 * the documentUpdateActivity event occurs, that object's appropriate
 * method is invoked.
 *
 * @see DocumentUpdateActivityEvent
 */
public class DocumentUpdateActivityListener extends FileUpdateActivityListener {

  /** The Constant LOG. */
  private static final Log              LOG           = ExoLogger.getLogger(DocumentUpdateActivityListener.class);

  /** The pending events. */
  protected Map<Node, FileUpdatedEvent> pendingEvents = new ConcurrentHashMap<>();

 
  /** The Constant EVENT_DELAY in ms. */
  protected static final long           EVENT_DELAY   = 120000;

  /**
   * Event handler.
   *
   * @param event the event
   * @throws Exception the exception
   */
  @Override
  public void onEvent(Event<Context, String> event) throws Exception {
    Context context = event.getSource();
    Property currentProperty = (Property) context.get(InvocationContext.CURRENT_ITEM);
    Node currentNode = currentProperty.getParent().getParent();
    String lastModifier = currentNode.getProperty("exo:lastModifier").getString();

    // Schedule task for this event
    Timer timer = new Timer();
    timer.schedule(createTimerTask(event, currentNode), EVENT_DELAY);

    FileUpdatedEvent pendingEvent = pendingEvents.get(currentNode);
    if (pendingEvent != null) {
      // Reset timer for new event
      pendingEvent.getTimer().cancel();
      pendingEvent.setTimer(timer);

      if (!lastModifier.equals(pendingEvent.getLastModifier())) {
        // Send event and change lastModifier for pending one.
        super.onEvent(pendingEvent.getEvent());
        pendingEvent.setLastModifier(lastModifier);
        pendingEvent.setEvent(event);
      }
    } else {
      pendingEvents.put(currentNode, new FileUpdatedEvent(lastModifier, timer, event));
    }
  }

  /**
   * Creates the timer task.
   *
   * @param event the event
   * @param node the node
   * @return the timer task
   */
  public TimerTask createTimerTask(Event<Context, String> event, Node node) {
    // TODO: wrap to Container Commant to obtain portal context
    return new TimerTask() {
      @Override
      public void run() {
        try {
          LOG.info("Sending event...");
          DocumentUpdateActivityListener.super.onEvent(event);
          pendingEvents.remove(node);
        } catch (Exception e) {
          LOG.error("Couldn't send the event: ", e);
        }
      }
    };
  }
}
