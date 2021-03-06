(ns markov-bot.make-bot
  (:require [markov-bot.twitter :as twitter]
            [markov-bot.generator :refer :all]
            [cemerick.url :refer [url-encode]]))

(defn get-tweets-for-users [users]
  (->> users
       (map twitter/get-all-tweets-for-user)
       (apply concat)))

(defn gen-chain-from-tweets [tweets]
  (->> (map clojure.string/lower-case tweets)
       (map text-to-word-chain)
       (apply merge-with clojure.set/union)))

(defn gen-rand-start-phrase [tweets]
  (->> tweets
       shuffle
       first
       (#(clojure.string/split % #" "))
       (take 2)
       (clojure.string/join " ")
       clojure.string/lower-case))

(defn reject-tweet [tweet orig-tweets]
  (let [is-same (contains? (set orig-tweets) tweet)]
    (if (= is-same true) (println "Removing" tweet))
    is-same))

(defn make-bot [users]
  (let [tweets (->> users get-tweets-for-users)
        chain  (->> tweets gen-chain-from-tweets)
        make-text #(generate-text (gen-rand-start-phrase tweets) chain)]
    (fn [num]
      (loop [num-to-gen num
             result []]
        (let [new-tweets (repeatedly num make-text)
              should-reject #(reject-tweet % tweets)
              filtered (remove should-reject new-tweets)]
          (if (< (count filtered) num-to-gen)
            (recur (- num-to-gen (count filtered)) (concat result filtered))
            (concat result filtered)))))))
