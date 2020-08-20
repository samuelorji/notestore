package auth

import akka.Done

import scala.concurrent.Future

class AuthService(tokenService: TokenService) {
  import AuthService._
  def issueToken(params: TokenParams, tokenType: String): Future[Either[GenericTokenError, Token]] = {
    tokenService.issueToken(params, tokenType)
  }

  def authenticateToken(token: String, tokenType: String): Future[Either[TokenError, Token]] = {
    tokenType match {
      case Bearer      => authenticateBearerToken(token)
      case Application => authenticateApplicationToken(token)
      case _           => Future.successful(Left(GenericTokenError("Invalid credential type")))
    }
  }
  private def authenticateBearerToken(token: String): Future[Either[TokenError, Token]] = {
    tokenService.verifyToken(token, Bearer)
  }

  private def authenticateApplicationToken(token: String): Future[Either[TokenError, Token]] = {
    tokenService.verifyToken(token, Application)
  }
  def removeToken(token: String, tokenType: String): Future[Either[IllegalStateException, Boolean]] = {
    tokenService.removeToken(token, tokenType)
  }

  def verifyUserNameAndPassword(userName: String, password: String): Future[Either[ValidationError, Done]] = ???
}

object AuthService {
  val Bearer = "Bearer"
  val Application = "Application"
}
