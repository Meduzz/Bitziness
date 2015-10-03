package se.chimps.bitziness.core.endpoints.io

import java.net.InetSocketAddress

import akka.io.Tcp.CommandFailed

trait ServerIOEndpoint {

}

object Server {
  def apply(callbacks: ServerCallbacks):Server = new ServerImpl
}

trait Server {
  def bind(port:Int)
  def unbind()
}

private class ServerImpl  extends Server {
  override def bind(port: Int): Unit = println()

  override def unbind(): Unit = println()
}

trait ServerCallbacks {
  def allowConnection(remote:InetSocketAddress):Boolean = true
  def onConnection(pipeline:Pipeline):Pipeline
  def onCommandFail(cmd:CommandFailed):Unit
}

trait Pipeline {

}