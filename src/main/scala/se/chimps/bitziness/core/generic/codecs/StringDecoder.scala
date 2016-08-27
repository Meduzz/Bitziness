package se.chimps.bitziness.core.generic.codecs

import akka.util.ByteString

/**
	* Created by meduzz on 18/08/16.
	*/
class StringDecoder extends Decoder[ByteString, String] {
  override def decode(in:ByteString):String = in.utf8String
}
