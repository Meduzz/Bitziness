package se.chimps.bitziness.core.endpoints.net.http.server.unrouting

import java.net.InetSocketAddress

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import se.chimps.bitziness.core.endpoints.net.http.HttpServerEndpoint
import se.chimps.bitziness.core.generic.ErrorMapping

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

/**
 *
 */
trait Controller {
  private[unrouting] var definitions:List[ActionDefinition] = List()

  def get(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("GET", path, action, paramex)
  def post(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("POST", path, action, paramex)
  def put(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("PUT", path, action, paramex)
  def delete(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("DELETE", path, action, paramex)
  def option(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("OPTION", path, action, paramex)

  protected def addAction(method:String, path:String, action:Action, paramex:Map[String, String]) = {
    definitions = definitions ++ List(ControllerUtil.definition(method, path, action, paramex))
  }
}

object ControllerUtil {
	def definition(method:String, path:String, action:Action, paramex:Map[String, String]):ActionDefinition = {
		val findPathParamsRegex = ":([a-zA-Z0-9]+)".r
		val pathParamsRegex = "(:[a-zA-Z0-9]+)".r
		val pathNames = findPathParamsRegex.findAllIn(path).map(m => m.substring(1)).toList
		val pathRegex = pathNames.foldLeft(path)((url, name) => s"(:$name)".r.replaceAllIn(url, paramex.getOrElse(name, "([a-zA-Z0-9]+)"))).r

		ActionDefinition(method, pathRegex, action, pathNames)
	}
}

object Action {

  def apply(func:(UnroutingRequest)=>Future[HttpResponse]):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = func(req)
  }

  def apply(func:() => Future[HttpResponse]):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = func()
  }

  def sync(func:(UnroutingRequest)=>HttpResponse)(implicit ec:ExecutionContext):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = Future(func(req))
  }

  def sync(func:() => HttpResponse)(implicit ec:ExecutionContext):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = Future(func())
  }
}

trait Action extends (UnroutingRequest => Future[HttpResponse]) {
  def apply() = this
}

case class ActionDefinition(method:String, pathRegex:Regex, action:Action, paramNames:List[String])
case class UnroutingRequest(inet:InetSocketAddress, raw:HttpRequest, params:Map[String, String], mat: Materializer, ec:ExecutionContext) extends SugarCoating {
  implicit val materializer = mat
  implicit val executor = ec
}

/**
	* This trait are depending on that the akka http server has been instantiated.
	* Using it from Actor.preStart are late enough.
	*/
trait UnroutingDsl extends ErrorMapping[HttpResponse] { self:HttpServerEndpoint =>

	implicit def ec:ExecutionContext

	def get(path:String, paramex:Map[String, String] = Map()):ModelStep = ModelStep("GET", path, paramex)
	def post(path:String, paramex:Map[String, String] = Map()):ModelStep = ModelStep("POST", path, paramex)
	def put(path:String, paramex:Map[String, String] = Map()):ModelStep = ModelStep("PUT", path, paramex)
	def delete(path:String, paramex:Map[String, String] = Map()):ModelStep = ModelStep("DELETE", path, paramex)
	def option(path:String, paramex:Map[String, String] = Map()):ModelStep = ModelStep("OPTION", path, paramex)

	def noModel:(UnroutingRequest)=>Future[Unit] = req => Future(Unit)
	def noAction[T]:(T)=>Future[Unit] = any => Future(Unit)

	case class ModelStep(method:String, path:String, paramex:Map[String, String]) {
		def toModel[T](func:(UnroutingRequest)=>Future[T]):ActionStep[T] = ActionStep[T](this, func)
	}

	case class ActionStep[T](modelStep: ModelStep, model:(UnroutingRequest)=>Future[T]) {
		def withAction[K](func:(T)=>Future[K]):ResponseStep[T,K] = ResponseStep[T,K](this, func)
	}

	case class ResponseStep[T,K](actionStep: ActionStep[T], model:(T)=>Future[K]) {
		def finalize(func:(K)=>Future[HttpResponse]):Unit = {
			val action = Action { req =>
				val res = for {
					m <- actionStep.model(req)
					a <- model(m)
					res <- func(a)
				} yield res

				res.recover(errorMapping)
			}

			val definition = ControllerUtil.definition(actionStep.modelStep.method, actionStep.modelStep.path, action, actionStep.modelStep.paramex)
			server.foreach(_ ! definition)
		}
	}
}
