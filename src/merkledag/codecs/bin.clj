(ns merkledag.codecs.bin
  "Enhanced binary multicodec which accepts a variety of raw bytes for encoding
  and always produces `PersistentBytes` values on decode."
  (:require
    [byte-streams :as bytes]
    [clojure.java.io :as io]
    (multicodec.codecs
      [bin :as bin]
      [filter :refer [filter-codec]]))
  (:import
    blocks.data.PersistentBytes
    java.nio.ByteBuffer))


;; ## Binary Protocol

(extend-protocol bin/BinaryData

  ByteBuffer

  (write-bytes!
    [buffer output]
    (let [length (.remaining buffer)]
      (bytes/transfer buffer output)
      length))


  PersistentBytes

  (write-bytes!
    [content output]
    (io/copy (.open content) output)
    (count content)))



;; ## Binary Codec

(defn bin-codec
  "Constructs a new enhanced binary codec. Decodes data into `PersistentBytes`
  values rather than byte arrays."
  []
  (filter-codec
    (bin/bin-codec)
    :decoding (fn wrap-bytes [data]
                (PersistentBytes/wrap ^bytes data))))
