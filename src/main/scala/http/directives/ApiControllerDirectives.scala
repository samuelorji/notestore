package http.directives

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.headers.{ BasicHttpCredentials, HttpChallenge, OAuth2BearerToken }
import auth.{ AuthService, Token }
import org.slf4j.LoggerFactory

trait ApiControllerDirectives {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Directive1 }
  protected val authService: AuthService

  private def rejectAuth(cause: AuthenticationFailedRejection.Cause) = {
    val challenge = HttpChallenge("Basic", "NoteStore API")
    reject(AuthenticationFailedRejection(cause, challenge))
  }

  def userIdAction(fun: String => ToResponseMarshallable): Directive1[ToResponseMarshallable] = {
    authenticationAction().flatMap {
      case Some(token) => provide(fun(token.userId))
      case None        => provide(ApiResponse.UnAuthenticated)
    }
  }

  private def credentialsAction(): Directive1[Option[(String, String)]] = {
    extractCredentials.flatMap {
      case p @ Some(OAuth2BearerToken(token)) =>
        provide(Some("Bearer" -> token))

      case Some(BasicHttpCredentials(username, password)) =>
        provide(Some(username -> password))

      case None => provide(None)

      case _ => extractLog.flatMap { log =>
        log.error(s"Could not extract valid authorization header credentials")
        provide(None)
      }
    }
  }

  private def authenticationAction(): Directive1[Option[Token]] = {
    credentialsAction().flatMap {
      case None => provide(None)
      //credentialName could be bearer , or application
      case Some((credentialName, token)) =>
        onSuccess(authService.authenticateToken(token, credentialName)).flatMap {
          case Left(_) =>
            rejectAuth(AuthenticationFailedRejection.CredentialsRejected)
          case Right(authenticatedToken) =>
            provide(Some(authenticatedToken))
        }
    }
  }

}
