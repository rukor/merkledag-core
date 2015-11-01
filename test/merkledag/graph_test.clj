(ns merkledag.graph-test
  (:require
    [blobble.core :as blob]
    [blobble.store.memory :refer [memory-store]]
    [byte-streams :as bytes :refer [bytes=]]
    (clj-time
      [coerce :as coerce]
      [core :as time]
      [format :as format :refer [formatters]])
    [clojure.test :refer :all]
    [merkledag.graph :as merkle]
    [merkledag.types :refer [core-types]]
    [multihash.core :as multihash]
    [puget.dispatch :as dispatch]
    [puget.printer :as puget])
  (:import
    merkledag.graph.MerkleLink
    multihash.core.Multihash
    org.joda.time.DateTime))


(defn dprint
  [v]
  (puget/cprint
    v
    {:print-handlers
     (dispatch/chained-lookup
       {DateTime (puget/tagged-handler 'inst (partial format/unparse (formatters :date-time)))
        Multihash (puget/tagged-handler 'data/hash multihash/base58)
        MerkleLink (puget/tagged-handler 'data/link (juxt :name :target :tsize))}
       puget/common-handlers)}))


(def hash-1 (multihash/decode "Qmb2TGZBNWDuWsJVxX7MBQjvtB3cUc4aFQqrST32iASnEh"))
(def hash-2 (multihash/decode "Qmd8kgzaFLGYtTS1zfF37qKGgYQd5yKcQMyBeSa8UkUz4W"))


(deftest a-test
  (let [store (memory-store)
        repo {:types core-types, :store store}]
    (merkle/with-repo repo
      (testing "basic node properties"
        (let [node (merkle/node
                     [(merkle/link "@context" hash-1)]
                     {:type :finance/posting
                      :uuid "foo-bar"})]
          (is (instance? Multihash (:id node)))
          (is (instance? java.nio.ByteBuffer (:content node)))
          (is (vector? (:links node)))
          (is (= 1 (count (:links node))))
          (is (every? (partial instance? MerkleLink) (:links node)))
          (is (map? (:data node)))
          (is (empty? (blob/list store))
              "node creation should not store any blobs")))
      (testing "multi-node reference"
        (let [node-1 (merkle/node
                       {:type :finance/posting
                        :uuid "foo-bar"})
              node-2 (merkle/node
                       {:type :finance/posting
                        :uuid "frobblenitz omnibus"})
              node-3 (merkle/node
                       [(merkle/link "@context" hash-1)]
                       {:type :finance/transaction
                        :uuid #uuid "31f7dd72-c7f7-4a15-a98b-0f9248d3aaa6"
                        :title "SCHZ - Reinvest Dividend"
                        :description "Automatic dividend reinvestment."
                        :time #inst "2013-10-08T00:00:00Z"
                        :entries [(merkle/link "posting-1" node-1)
                                  (merkle/link "posting-2" node-2)]})]
          ;(bytes/print-bytes (:content node-3))
          (is (= 3 (count (:links node-3))))
          (is (every? (partial instance? MerkleLink) (:links node-3)))
          (merkle/put-node! repo node-1)
          (merkle/put-node! repo node-2)
          (merkle/put-node! repo node-3)
          (let [node' (merkle/get-node repo (:id node-3))]
            (is (= (:id node') (:id node-3)))
            (is (bytes= (:content node') (:content node-3)))
            (dprint node')
            (is (= (:links node') (:links node-3)))
            (is (= (:data node') (:data node-3))))

          ;(dprint node-1)
          ;(dprint @(first (:entries (:data node-3))))
          )))))


; TODO: test raw :data segments
