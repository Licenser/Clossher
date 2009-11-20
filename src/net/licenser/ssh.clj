(ns net.licenser.ssh
  (:import com.jcraft.jsch.JSch)
  (:import java.util.Properties)
  (:use clojure.contrib.duck-streams))

(declare *jsch* *jsch-config* *jsch-session*)

(def *jsch* (new com.jcraft.jsch.JSch))

(def *jsch-config* (doto (new java.util.Properties)
                     (.put "StrictHostKeyChecking" "no")))

(defmacro with-session-with-port [user password host port & body]
  "This macro creates a ssh session that is valid within it's scope."
  `(binding [*jsch-session* (doto (.getSession *jsch* ~user ~host ~port)
                         (.setConfig *jsch-config*)
                         (.setPassword ~password)
                         (.connect))]
     (try
       (do ~@body)
       (finally (.disconnect *jsch-session*)))))



(defmacro with-session [user password host & body]
  "This macro creates a ssh session that is valid within it's scope, using the
default port 22 to connect."
  `(with-session-with-port ~user ~password ~host 22 ~@body))

(defn exec
  "Executes a command on the remote host and returns a seq of the lines the
command retured."
  [command]
  (let
    [channel (.openChannel *jsch-session* "exec")]
    (doto channel
      (.setCommand command)
      (.setInputStream nil)
      (.setErrStream System/err))
    (with-open
      [stream (.getInputStream channel)]
      (.connect channel)
      (doall (line-seq (clojure.contrib.duck-streams/reader stream))))))