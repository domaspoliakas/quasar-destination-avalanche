package quasar.destination.avalanche

import scala.concurrent.duration.Duration

import cats.effect.{Concurrent, Timer, ContextShift, Resource}

import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

object Http4sEmberClient {
  
  def client[F[_]: Concurrent: Timer: ContextShift]: Resource[F, Client[F]] = 
    EmberClientBuilder
      .default[F]
      .withMaxTotal(400)
      .withMaxPerKey(_ => 200)
      .withTimeout(Duration.Inf)
      .build

}
