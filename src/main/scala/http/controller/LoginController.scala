package http.controller

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive1
import auth.{ AuthService, TokenParams }
import http.directives.{ ApiControllerDirectives, ApiResponse }
import http.model.Models.SignUpUserInput

import scala.concurrent.{ ExecutionContext, Future }

trait LoginController {
  def signUpUser(input: SignUpUserInput): Future[ToResponseMarshallable]
  def doStuff: Directive1[ToResponseMarshallable]
}
class LoginControllerImpl(val authService: AuthService)(implicit ec: ExecutionContext) extends ApiControllerDirectives with LoginController {
  import AuthService._
  override def signUpUser(input: SignUpUserInput): Future[ToResponseMarshallable] = {
    val tokenParams = TokenParams(userId = "samuel")
    authService.issueToken(tokenParams, Bearer).map {
      case Left(err) =>
        logger.error("Couldn't issue token", new Exception(err.errorMessage))
        ApiResponse.badRequest(err.errorMessage)
      case Right(token) =>
        val headers: Seq[RawHeader] = List(RawHeader("Authorization", s"Bearer ${token.serialized}"))
        ApiResponse.singleItemOk(token.userId, headers)
    }
  }

  override def doStuff: Directive1[ToResponseMarshallable] = {
    userIdAction { userId =>
      Future.successful(userId)
    }
  }
}
