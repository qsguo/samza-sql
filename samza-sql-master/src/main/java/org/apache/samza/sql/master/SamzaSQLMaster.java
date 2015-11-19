/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.samza.sql.master;

import org.apache.samza.sql.master.rest.QueryHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class SamzaSQLMaster {
  private static final Logger log = LoggerFactory.getLogger(SamzaSQLMaster.class);

  public static final String PROP_WEBAPP_BASE_DIR = "samza.sql.master.webapp.home";
  public static final String PROP_MODE = "samza.sql.mode";

  public static void main(String[] args) throws Exception {
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");

    Server jettyServer = new Server(8080);
    jettyServer.setHandler(context);

    context.addServlet(DefaultServlet.class, "/");

    String pwdPath = System.getProperty("user.dir");
    String webBaseDirRelativeToWorkingDir = System.getProperty(PROP_WEBAPP_BASE_DIR);
    String webBaseDir = pwdPath;
    if(webBaseDirRelativeToWorkingDir != null){
      webBaseDir = Paths.get(pwdPath, webBaseDirRelativeToWorkingDir).toString();
    } else {
      webBaseDir = Paths.get(pwdPath, "web").toString();
    }

    // Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
    // It is important that this is last.
    ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
    holderPwd.setInitParameter("resourceBase", webBaseDir);
    holderPwd.setInitParameter("dirAllowed", "true");
    context.addServlet(holderPwd, "/");

    ServletHolder jerseyServlet = context.addServlet(
        org.glassfish.jersey.servlet.ServletContainer.class, "/api/*");
    jerseyServlet.setInitOrder(0);

    // Tells the Jersey Servlet which REST service/class to load.
    jerseyServlet.setInitParameter(
        ServerProperties.PROVIDER_CLASSNAMES,
        QueryHandler.class.getCanonicalName());
    jerseyServlet.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
        "com.fasterxml.jackson.jaxrs.json;org.apache.samza.sql.master.rest.providers");
    jerseyServlet.setInitParameter(
        "com.sun.jersey.api.json.POJOMappingFeature",
        "true");

    try {
      jettyServer.start();

      String mode = System.getProperty(PROP_MODE);
      if(mode != null && mode.trim().equals("dev")) {
        jettyServer.dump(System.err);
      }

      jettyServer.join();
    } finally {
      jettyServer.destroy();
    }
  }
}
