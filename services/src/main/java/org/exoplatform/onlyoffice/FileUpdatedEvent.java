package org.exoplatform.onlyoffice;

import java.util.Timer;

import org.apache.commons.chain.Context;

import org.exoplatform.services.listener.Event;

/**
 * The Class FileUpdatedEvent.
 */
public class FileUpdatedEvent {

  /** The last modifier. */
  private String lastModifier;
  
  /** The timer. */
  private Timer timer;
  
  /** The event. */
  private Event<Context, String> event;
  
  
  /**
   * Instantiates a new file updated event.
   *
   * @param lastModifier the last modifier
   * @param timer the timer
   * @param event the event
   */
  public FileUpdatedEvent(String lastModifier, Timer timer, Event<Context, String> event) {
    this.lastModifier = lastModifier;
    this.timer = timer;
    this.event = event;
  }

  /**
   * Gets the last modifier.
   *
   * @return the last modifier
   */
  public String getLastModifier() {
    return lastModifier;
  }

  /**
   * Sets the last modifier.
   *
   * @param lastModifier the new last modifier
   */
  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

  /**
   * Gets the timer.
   *
   * @return the timer
   */
  public Timer getTimer() {
    return timer;
  }

  /**
   * Sets the timer.
   *
   * @param timer the new timer
   */
  public void setTimer(Timer timer) {
    this.timer = timer;
  }

  /**
   * Gets the event.
   *
   * @return the event
   */
  public Event<Context, String> getEvent() {
    return event;
  }

  /**
   * Sets the event.
   *
   * @param event the event
   */
  public void setEvent(Event<Context, String> event) {
    this.event = event;
  }


}
