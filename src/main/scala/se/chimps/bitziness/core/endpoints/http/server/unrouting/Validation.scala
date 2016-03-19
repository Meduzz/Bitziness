package se.chimps.bitziness.core.endpoints.http.server.unrouting

trait Validation {

  def validate[T](data:T, func:(T)=>Option[String]):Field[T] = Field(data).validate(func)

  implicit def enhanceMap(map:Map[String, String]):MapConverters = new MapConverters(map)

  implicit def intValidation(data:Int):IntValidators = new IntValidators(data)

  implicit def longValidation(data:Long):LongValidators = new LongValidators(data)

  implicit def stringValidation(data:String):StringValidators = new StringValidators(data)

  implicit def booleanValidators(data:Boolean):BooleanValidators = new BooleanValidators(data)

  implicit def bigDecimalValidators(data:BigDecimal):BigDecimalValidators = new BigDecimalValidators(data)

}

class MapConverters(val map:Map[String, String]) {
  def asInt(field:String):Int = map(field).toInt
  def asLong(field:String):Long = map(field).toLong
  def asBoolean(field:String):Boolean = map(field).toBoolean
  def asBigDecimal(field:String):BigDecimal = BigDecimal(map(field))
}

class IntValidators(val data:Int) {
  def between(low:Int, high:Int):Boolean = high > data && data > low
  def negative:Boolean = data < 0
  def positive:Boolean = data > 0
  def ==(other:Int):Boolean = data == other
  def max(max:Int):Boolean = data < max
  def min(min:Int):Boolean = data > min
}

class LongValidators(val data:Long) {
  def between(low:Long, high:Long):Boolean = high > data && data > low
  def negative:Boolean = data < 0
  def positive:Boolean = data > 0
  def ==(other:Long):Boolean = data == other
  def max(max:Long):Boolean = data < max
  def min(min:Long):Boolean = data > min
}

class StringValidators(val data:String) {
  def notNull:Boolean = data != null
  def lengthBetween(min:Int, max:Int):Boolean = data.length >= min && data.length <= max
}

class BooleanValidators(val data:Boolean) {
  def notNull:Boolean = data != null
}

class BigDecimalValidators(val data:BigDecimal) {
  def between(low:BigDecimal, high:BigDecimal):Boolean = high > data && data > low
  def negative:Boolean = data < BigDecimal(0)
  def positive:Boolean = data > BigDecimal(0)
  def max(max:BigDecimal):Boolean = data < max
  def min(min:BigDecimal):Boolean = data > min
  def notNull:Boolean = data != null
}

object Field {
  def apply[T](field:T) = Valid(field)
}

trait Field[T] {
  def validate(func:(T)=>Option[String]):Field[T]
  def map[K](f:(T)=>K):Field[K]
  def flatMap[K](f:(T)=>Field[K]):Field[K]
  def filter(f:(T)=>Boolean):Field[T]
  def forEach(f:(T)=>Unit):Unit

  private[unrouting] def data:T
}

case class Valid[T](data:T) extends Field[T] {
  override def validate(func:(T)=>Option[String]):Field[T] = {
    val pred = func(data)
    if (pred.isEmpty) {
      Valid(data)
    } else {
      Invalid(data, pred.get)
    }
  }

  override def map[K](f:(T) => K):Field[K] = Field(f(data))

  override def flatMap[K](f:(T) => Field[K]):Field[K] = f(data)

  override def forEach(f:(T) => Unit):Unit = map(f)

  override def filter(f:(T) => Boolean):Field[T] = {
    if (f(data)) {
      Valid(data)
    } else {
      Invalid(data, "")
    }
  }
}

case class Invalid[T](data:T, msg:String) extends Field[T] {
  override def validate(func:(T) => Option[String]):Field[T] = this

  override def map[K](f:(T) => K):Field[K] = Invalid(f(data), msg)

  override def flatMap[K](f:(T) => Field[K]):Field[K] = Invalid(f(data).data, msg)

  override def forEach(f:(T) => Unit):Unit = f(data)

  override def filter(f:(T) => Boolean):Field[T] = this
}