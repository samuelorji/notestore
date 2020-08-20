package http

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ HttpResponse, _ }
import play.api.libs.json.{ JsObject, JsValue, Json, Writes }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

package object directives {
  case class ApiResponse(statusCode: StatusCode, body: JsObject, headers: Seq[HttpHeader] = Nil)

  object ApiResponse {
    implicit def toResponseMarshallable(response: ApiResponse): ToResponseMarshallable = {
      val jsonEntity = HttpEntity(ContentType(MediaTypes.`application/json`), Json.stringify(response.body))
      HttpResponse(status = response.statusCode, headers = response.headers.toList, entity = jsonEntity)
    }
    implicit def toResponseMarshallable(fut: Future[ApiResponse]): ToResponseMarshallable = {
      fut.map { response =>
        val jsonEntity = HttpEntity(ContentType(MediaTypes.`application/json`), Json.stringify(response.body))
        HttpResponse(status = response.statusCode, headers = response.headers.toList, entity = jsonEntity)
      }
    }

    private def errorBody(errors: Seq[String]) = Json.obj(
      "errors" -> errors
    )
    def badRequest(messages: String*) = ApiResponse(StatusCodes.BadRequest, errorBody(messages))

    def Ok(data: JsValue, headers: Seq[HttpHeader] = Nil): ApiResponse = {
      val obj = Json.obj(
        "data" -> data
      )
      ApiResponse(StatusCodes.OK, obj, headers)
    }

    def singleItemOk[A](item: A, headers: Seq[HttpHeader] = Nil)(implicit write: Writes[A]) = {
      val json = Json.toJson(item)
      ApiResponse.Ok(
        data = json,
        headers = headers
      )
    }

    lazy val UnAuthenticated = ApiResponse(StatusCodes.Unauthorized, errorBody(List("Credentials missing or invalid")))
    lazy val serverError = ApiResponse(StatusCodes.InternalServerError, errorBody(List("Oops: Internal Server Error")))
  }

}
