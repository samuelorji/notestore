package http

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.server.{ Directive1, Directives, Route, ValidationRejection }
import play.api.libs.json._

package object routes {

  import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
  trait BaseRoute extends Directives {

    protected def completeWith(directive: Directive1[ToResponseMarshallable]) = {
      directive { trm =>
        complete(trm)
      }
    }

    def route: Route

    private def formatError(errorsSeq: Seq[(JsPath, Seq[JsonValidationError])]): String = {
      val (path, errors) = errorsSeq.head

      s"${path.toJsonString} -> ${errors.head.message}}"
    }

    protected def validateJson[T](implicit r: Reads[T]): Directive1[T] = {
      entity(as[JsValue]).flatMap { jsValue =>
        jsValue.validate[T].fold(err => reject(ValidationRejection(formatError(err))), model => provide(model))
      }
    }
  }
}