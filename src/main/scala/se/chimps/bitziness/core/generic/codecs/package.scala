package se.chimps.bitziness.core.generic

import akka.util.ByteString

package object codecs {
	trait Encoder[IN, OUT] {
		def encode(in:IN):OUT
	}

	trait Decoder[IN, OUT] {
		def decode(in:IN):OUT
	}

	trait Codec[BIN, CLS] extends Encoder[BIN, CLS] with Decoder[CLS, BIN] {}

	trait BinaryCodecBase[T] extends Codec[ByteString, T] {}
}
