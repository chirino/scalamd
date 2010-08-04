/**
 * Copyright (C) 2010 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.circumflex.scalate

import ru.circumflex.core._
import java.lang.String
import org.fusesource.scalate.servlet.{ServletRenderContext, Config, ServletTemplateEngine}
import org.fusesource.scalate.Binding
import java.io.PrintWriter

/**
 * Provides Scalate integration with Circumflex.
 */
object Scalate {

  private lazy val templateEngine:ServletTemplateEngine = {
    val servletContext = ctx.request.getSession.getServletContext
    val rc = new ServletTemplateEngine(new Config{
      def getName = servletContext.getServletContextName
      def getServletContext = servletContext
      def getInitParameterNames = servletContext.getInitParameterNames
      def getInitParameter(name: String) = servletContext.getInitParameter(name)
    })
    rc.bindings ::= Binding("ctx", "ru.circumflex.core.CircumflexContext", true, None, "val", true)
    rc
  }

  def render(template: String, status: Int=200, locals:List[Pair[String, Any]]=List(), layout:Boolean=true ): Nothing = {
    ctx.statusCode = status
    throw new RouteMatchedException(new WriterResponse(out => {
        val circumflexContext = ctx
        val renderContext = new ServletRenderContext(templateEngine, new PrintWriter(out), circumflexContext.request, circumflexContext.response, circumflexContext.request.getSession.getServletContext)
        circumflexContext.params.foreach{ x=>
          renderContext.attributes(x._1) = x._2
        }
        locals.foreach{ x=>
          renderContext.attributes(x._1) = x._2
        }
        renderContext.attributes("ctx") = circumflexContext
        renderContext.include(template, layout)
      }
    ))
  }

  def view(it: AnyRef, view: String): Nothing = {
    throw new RouteMatchedException(new WriterResponse(out => {
        val circumflexContext = ctx
        val renderContext = new ServletRenderContext(templateEngine, new PrintWriter(out), circumflexContext.request, circumflexContext.response, circumflexContext.request.getSession.getServletContext)
        circumflexContext.params.foreach{ x=>
          renderContext.attributes(x._1) = x._2
        }
        renderContext.attributes("ctx") = circumflexContext
        renderContext.view(it, view)
      }
    ))
  }

}

