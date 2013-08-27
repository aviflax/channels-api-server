(ns channels.server.api.util-test
  (:use clojure.test)
  (:require [channels.server.api.util :refer [resource]]
            [compojure.core :refer [GET HEAD OPTIONS POST]]
            [clojure.java.io :refer [file input-stream]]))


(deftest test-resource
  (testing "a resource with just a GET route should handle HEAD"
    (let [handler (resource "foo" "/"
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain"}
                             :body "bar"}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "text/plain"
                             "Content-Length" "3"}))))


  (testing "a resource with a String response body should handle HEAD properly"
    (let [handler (resource "foo" "/"
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain"}
                             :body "bar"}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "text/plain"
                             "Content-Length" "3"}))))


  (testing "a resource with a InputStream response body should handle HEAD properly"
    (let [handler (resource "foo" "/"
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain"}
                             :body (input-stream (.getBytes "bar"))}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "text/plain"
                             "Content-Length" "3"}))))


  (testing "a resource with a File response body should handle HEAD properly"
    (let [handler (resource "foo" "/"
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain"}
                             :body (file "resources/test/fixed_length_3.txt")}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "text/plain"
                             "Content-Length" "3"}))))


  (testing "a resource with a Seq response body should handle HEAD properly"
    (let [handler (resource "foo" "/"
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain"}
                             :body (seq ["f" "o" "o"])}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "text/plain"
                             "Content-Length" "3"}))))


  (testing "a resource wherein GET sets Content-Length should return that value on HEAD"
    (let [handler (resource "foo" "/"
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain", "Content-Length" "613"}
                             :body "WHATEVER"}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "text/plain"
                             "Content-Length" "613"}))))


  (testing "when a resource supplies its own HEAD handler, it should be used"
    (let [handler (resource "foo" "/"
                    (HEAD [] {:status 200
                              :headers {"Content-Type" "application/yourmom"
                                        "Content-Length" "1337"
                                        "Cheese" "Moldy"}})
                    (GET [] {:status 200
                             :headers {"Content-Type" "text/plain", "Content-Length" "613"}
                             :body "WHATEVER"}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 200))
      (is (empty? (:body res)))
      (is (= (:headers res) {"Content-Type" "application/yourmom"
                             "Content-Length" "1337"
                             "Cheese" "Moldy"}))))


  (testing "when a resource doesn’t supply a GET route, it shouldn’t support HEAD"
    (let [handler (resource "foo" "/"
                    (POST [] {:status 200
                              :headers {"Content-Type" "text/plain", "Content-Length" "8"}
                              :body "Success!"}))
          req {:uri "/"
               :request-method :head
               :headers {"accept" "*/*"}}
          res (handler req)]
      (is (= (:status res) 405)))))
