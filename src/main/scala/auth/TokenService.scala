package auth

import com.nimbusds.jose.{ JWSAlgorithm, JWSHeader }
import com.nimbusds.jose.crypto.{ MACSigner, MACVerifier }
import com.nimbusds.jwt.{ JWTClaimsSet, SignedJWT }
import org.joda.time.{ DateTime, DateTimeZone }
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

trait TokenService {

  def issueToken(params: TokenParams, tokenType: String): Future[Either[GenericTokenError, Token]]

  def verifyToken(token: String, tokenType: String): Future[Either[TokenError, Token]]

  def removeToken(token: String, tokenType: String): Future[Either[IllegalStateException, Boolean]]
}

class TokenServiceImpl(signingKey: Array[Byte], storage: TokenStorage)(implicit ec: ExecutionContext) extends TokenService {
  private val log = LoggerFactory.getLogger(getClass)
  private val signer = new MACSigner(signingKey)
  private val verifier = new MACVerifier(signingKey)

  override def issueToken(params: TokenParams, tokenType: String): Future[Either[GenericTokenError, Token]] = {
    try {
      val now: DateTime = DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0)
      val id = java.util.UUID.randomUUID().toString.replaceAll("-", "")
      val expiresAt = params.lifetime.map(seconds => now.plus(seconds))

      val claims = new JWTClaimsSet {
        setIssueTime(now.toDate)
        setJWTID(id)
        setSubject(params.userId)
        expiresAt.map(date => setExpirationTime(date.toDate))

      }

      val signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims)
      signed.sign(signer)

      val serialized = signed.serialize()
      val token = Token(
        id = id,
        userId = params.userId,
        issuedAt = now.toString(),
        expiresAt = expiresAt.map(_.toString()),
        serialized = serialized
      )

      storage.addToken(token.serialized, tokenType).map {
        case true  => Right(token)
        case false => Left(GenericTokenError(s"Could not insert token into storage: storage : $this"))
      }
    } catch {
      case NonFatal(e) => Future.successful(Left(GenericTokenError(e.getMessage)))
    }
  }

  override def verifyToken(serializedToken: String, tokenType: String): Future[Either[TokenError, Token]] = {
    try {
      val parsed = SignedJWT.parse(serializedToken)
      val result = if (parsed.verify(verifier)) {
        Option(parsed.getJWTClaimsSet.getIssueTime) match {
          case None => Left(InvalidToken(serializedToken, "No issued time set on token"))
          case Some(iat) =>
            Option(parsed.getJWTClaimsSet.getSubject) match {
              case None =>
                Left(InvalidToken(serializedToken, "No subject set on token"))
              case Some(userId) =>
                Option(parsed.getJWTClaimsSet.getJWTID) match {
                  case None =>
                    Left(InvalidToken(serializedToken, "No jwt id set on token"))
                  case Some(id) =>
                    Option(parsed.getJWTClaimsSet.getExpirationTime).flatMap { exp =>
                      val now = DateTime.now(DateTimeZone.UTC)
                      if (now.isAfter(new DateTime(exp.getTime, DateTimeZone.UTC))) {
                        Some(TokenExpired(serializedToken, "Token is expired"))
                      } else {
                        None
                      }
                    } match {
                      case Some(err) =>
                        Left(err)
                      case None =>
                        Right {
                          Token(
                            id = id,
                            userId = userId,
                            issuedAt = iat.toString,
                            expiresAt = None,
                            serialized = serializedToken
                          )
                        }
                    }
                }
            }
        }
      } else {
        //not verified
        Left(InvalidToken(serializedToken, "Invalid token"))
      }

      result match {
        case p @ Left(_) =>
          Future.successful(p)
        case Right(parsedToken) =>
          storage.exists(parsedToken.serialized, tokenType).map {
            case true =>
              Right(parsedToken)
            case false =>
              Left(TokenNotExistent(parsedToken.serialized, s"Token is not in storage : storage :$this"))
          }
      }

    } catch {
      case NonFatal(e) =>
        log.error("error occured while verifying token", e)
        Future.successful(Left(InvalidToken(serializedToken, "Invalid token")))
    }
  }

  override def removeToken(token: String, tokenType: String): Future[Either[IllegalStateException, Boolean]] =
    storage.removeToken(token, tokenType)
}