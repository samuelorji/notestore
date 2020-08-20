package auth

import redis.RedisClient

import scala.concurrent.Future

import scala.concurrent.ExecutionContext
trait TokenStorage {

  protected def keyFor(token: String, tokenType: String) = s"$tokenType:$token"
  def addToken(token: String, tokenType: String, expiresInSec: Option[Long] = None): Future[Boolean]

  def removeToken(token: String, tokenType: String): Future[Either[IllegalStateException, Boolean]]

  def exists(token: String, tokenType: String): Future[Boolean]
}

class RedisTokenStorage(client: RedisClient)(implicit ec: ExecutionContext) extends TokenStorage {
  override def addToken(token: String, tokenType: String, expiresInSec: Option[Long] = None): Future[Boolean] = {
    client.set(keyFor(token, tokenType), "", expiresInSec, None)
  }

  override def removeToken(token: String, tokenType: String): Future[Either[IllegalStateException, Boolean]] =
    client.del(keyFor(token, tokenType)).map {
      case 1l    => Right(false)
      case 0l    => Right(true)
      case other => Left(new IllegalStateException((s"Expected 0 or 1 keys deleted, not $other")))
    }

  override def exists(token: String, tokenType: String): Future[Boolean] = {
    client.exists(keyFor(token, tokenType))
  }
}
