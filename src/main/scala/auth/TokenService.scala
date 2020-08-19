package auth

import com.nimbusds.jose.{ JWSAlgorithm, JWSHeader }
import com.nimbusds.jose.crypto.{ MACSigner, MACVerifier }
import com.nimbusds.jwt.{ JWTClaimsSet, SignedJWT }
import org.joda.time.{ DateTime, DateTimeZone }
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

trait TokenService {

  def issueToken(params: TokenParams): Token

  def verifyToken(token: String): Either[TokenError, Token]
}

class TokenServiceImpl(signingKey: Array[Byte]) extends TokenService {
  private val log = LoggerFactory.getLogger(getClass)
  private val signer = new MACSigner(signingKey)
  private val verifier = new MACVerifier(signingKey)

  override def issueToken(params: TokenParams): Token = {
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

    Token(
      id = id,
      userId = params.userId,
      issuedAt = now.toString(),
      expiresAt = expiresAt.map(_.toString()),
      serialized = serialized
    )
  }

  override def verifyToken(serializedToken: String): Either[TokenError, Token] = {
    try {
      val parsed = SignedJWT.parse(serializedToken)
      if (parsed.verify(verifier)) {
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

    } catch {
      case NonFatal(e) =>
        log.error("error occured while verifying token", e)
        Left(InvalidToken(serializedToken, "Invalid token"))
    }

  }
}