(ns merkledag.codec.edn-test
  (:require
    [clojure.test :refer :all]
    [merkledag.codec.edn :as edn]
    [multicodec.core :as codec])
  (:import
    (java.io
      ByteArrayInputStream
      ByteArrayOutputStream)))


(deftest edn-codec
  (let [edn (edn/edn-codec {'test/foo {:description "Test type"
                                       :reader #(vector :test/foo %)
                                       :writers {clojure.lang.Ratio str}}})
        test-encode #(let [baos (ByteArrayOutputStream.)]
                       (codec/encode! edn baos %)
                       (String. (.toByteArray baos)))
        test-decode #(codec/decode edn (.getBytes %))]
    (testing "encoding"
      (is (= "" (test-encode nil))
          "nil value should encode to nil")
      (is (= "false\n" (test-encode false)))
      (is (= "123\nfoo\n:bar\n" (test-encode '(123 foo :bar)))))
    (testing "decoding"
      (is (= {:alpha true, :beta 'bar, "foo" 123} (test-decode "{:alpha true :beta bar \"foo\" 123}")))
      (is (= [:foo 'bar 456] (test-decode ":foo\nbar\n456\n"))))))