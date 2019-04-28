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
package org.exoplatform.onlyoffice.webui;

import java.util.List;

import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.filter.Filter;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AbstractOnlyofficeWebFilter.java 00000 Apr 27, 2019 pnedonosko
 *          $
 */
public abstract class AbstractOnlyofficeWebFilter implements Filter {

  /**
   * 
   */
  protected AbstractOnlyofficeWebFilter() {
  }

  /**
   * Consult if we can add a new lifecycle of given class to the list. This
   * method is not blocking and thread safe, but as result of working over a
   * {@link List} of lifecycles, weakly consistent regarding its answer.
   *
   * @param lifecycles the lifecycles list
   * @param lifecycleClass the lifecycle class to add
   * @return <code>true</code>, if can add, <code>false</code> otherwise
   */
  @SuppressWarnings("rawtypes")
  protected <C extends ApplicationLifecycle> boolean canAddLifecycle(List<ApplicationLifecycle> lifecycles,
                                                                     Class<C> lifecycleClass) {
    return getLifecycle(lifecycles, lifecycleClass) == null;
  }

  /**
   * Returns a lifecycle instance of given class from the list. This method is
   * not blocking and thread safe, but as result of working over a {@link List}
   * of lifecycles, weakly consistent regarding its result.
   *
   * @param <C> the generic type
   * @param lifecycles the lifecycles list
   * @param lifecycleClass the lifecycle class
   * @return the lifecycle instance or <code>null</code> if nothing found in the
   *         given list
   */
  @SuppressWarnings("rawtypes")
  protected <C extends ApplicationLifecycle> C getLifecycle(List<ApplicationLifecycle> lifecycles, Class<C> lifecycleClass) {
    if (lifecycles.size() > 0) {
      // We want iterate from end of the list and don't be bothered by
      // ConcurrentModificationException for a case if someone else will modify
      // the list
      int index = lifecycles.size() - 1;
      do {
        ApplicationLifecycle lc = lifecycles.get(index);
        if (lc != null && lifecycleClass.isAssignableFrom(lc.getClass())) {
          return lifecycleClass.cast(lc);
        } else {
          index--;
        }
      } while (index >= 0);
    }
    return null;
  }

}
