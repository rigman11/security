/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.seam.security.external.virtualapplications;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;

import org.jboss.seam.security.external.virtualapplications.api.AfterVirtualApplicationCreation;
import org.jboss.seam.servlet.event.qualifier.Destroyed;
import org.jboss.seam.servlet.event.qualifier.Initialized;
import org.slf4j.Logger;

/**
 * @author Marcel Kolsteren
 * 
 */
@ApplicationScoped
public class VirtualApplicationManager
{
   @Inject
   private Logger log;

   @Inject
   private VirtualApplicationContextExtension virtualApplicationContextExtension;

   @Inject
   private Instance<VirtualApplicationBean> virtualApplication;

   @Inject
   private BeanManager beanManager;

   private Set<String> hostNames = new HashSet<String>();

   protected void servletInitialized(@Observes @Initialized final ServletContextEvent e)
   {
      log.trace("Servlet initialized with event {}", e);
      getVirtualApplicationContext().initialize(e.getServletContext());

      AfterVirtualApplicationManagerCreationEvent afterVirtualApplicationManagerCreation = new AfterVirtualApplicationManagerCreationEvent();
      beanManager.fireEvent(afterVirtualApplicationManagerCreation);

      for (String hostName : afterVirtualApplicationManagerCreation.getHostNames())
      {
         hostNames.add(hostName);
         getVirtualApplicationContext().create(hostName);
         virtualApplication.get().setHostName(hostName);
         beanManager.fireEvent(new AfterVirtualApplicationCreation());
         getVirtualApplicationContext().detach();
      }
   }

   protected void servletDestroyed(@Observes @Destroyed final ServletContextEvent e)
   {
      log.trace("Servlet destroyed with event {}", e);
      for (String hostName : hostNames)
      {
         if (getVirtualApplicationContext().isExistingVirtualApplication(hostName))
         {
            attach(hostName);
            getVirtualApplicationContext().destroy();
         }
      }
   }

   protected void requestInitialized(@Observes @Initialized final ServletRequestEvent e)
   {
      log.trace("Servlet request initialized with event {}", e);
      String hostName = e.getServletRequest().getServerName();
      if (getVirtualApplicationContext().isExistingVirtualApplication(hostName))
      {
         attach(hostName);
      }
   }

   protected void requestDestroyed(@Observes @Destroyed final ServletRequestEvent e)
   {
      log.trace("Servlet request destroyed with event {}", e);
      if (getVirtualApplicationContext().isActive())
      {
         detach();
      }
   }

   public void attach(String hostName)
   {
      getVirtualApplicationContext().attach(hostName);
      virtualApplication.get().setHostName(hostName);
   }

   public void detach()
   {
      getVirtualApplicationContext().detach();
   }

   public Set<String> getHostNames()
   {
      return hostNames;
   }

   private VirtualApplicationContext getVirtualApplicationContext()
   {
      return virtualApplicationContextExtension.getVirtualApplicationContext();
   }
}