package ru.circumflex.core

import javax.servlet.http.HttpServletResponse
import org.apache.commons.io.IOUtils
import java.io.{FileInputStream, File, OutputStream}

/**
 * Represents an `HttpServletResponse` wrapper for committing responses.
 * Apply method sets character encoding, content type, status code and headers from
 * `CircumflexContext` and flushes any associated output.
 */
trait HttpResponse {
  /**
   * Applies character encoding, content type, status code and headers to
   * specified `HttpServletResponse` and flushes any associated output.
   * If specified response is committed, returns silently.
   */
  def apply(response: HttpServletResponse) {
    if (!response.isCommitted) {
      response.setCharacterEncoding("UTF-8")
      response.setContentType(context.contentType.getOrElse("text/html"))
      response.setStatus(context.statusCode)
      context.stringHeaders.foreach(p => { response.setHeader(p._1, p._2) })
      context.dateHeaders.foreach(p => { response.setDateHeader(p._1, p._2) })
    }
  }
}

class EmptyResponse extends HttpResponse

case class ErrorResponse(val errorCode: Int, val msg: String) extends HttpResponse {
  override def apply(response: HttpServletResponse) {
    context.statusCode = errorCode
    response.sendError(errorCode, msg)
  }
}

case class RedirectResponse(val url: String) extends HttpResponse {
  override def apply(response: HttpServletResponse) = response.sendRedirect(url)
}

case class TextResponse(val text: String) extends HttpResponse {
  override def apply(response: HttpServletResponse) {
    super.apply(response)
    response.getWriter.print(text)
  }
}

case class BinaryResponse(val data: Array[Byte]) extends HttpResponse {
  override def apply(response: HttpServletResponse) {
    super.apply(response)
    response.getOutputStream.write(data)
  }
}

case class FileResponse(val file: File) extends HttpResponse {
  override def apply(response: HttpServletResponse) {
    // determine mime type by extension
    if (context.contentType.isEmpty)
      context.contentType = Circumflex.mimeTypesMap.getContentType(file)

    super.apply(response)
    // transfer a file
    val is = new FileInputStream(file)
    try {
      IOUtils.copy(is, response.getOutputStream)
    } finally {
      is.close
    }
  }
}

case class DirectStreamResponse(val streamingFunc: OutputStream => Unit)
    extends HttpResponse {
  override def apply(response: HttpServletResponse) {
    super.apply(response)
    streamingFunc(response.getOutputStream)
  }
}

case class ContextualResponse(f: CircumflexContext => HttpResponse) extends HttpResponse {
  override def apply(response: HttpServletResponse) =
    try {
      f(context)(response)
    }
    catch {
      case e: MatchError => ErrorResponse(400, "" + e)(response) // TODO: log error
    }
}