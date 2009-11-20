
(ns net.licenser.ssh.shell
  (:use clojure.contrib.duck-streams))

(declare *jsch-shell* *jsch-in* *jsch-out*)

(def *jsch-shell-wait* 500)


(defmacro with-shell
  "This macro encapsulates a shell session by creating a binding.
shell-* functions can be used within this macro"
  [& body]
  `(binding [*jsch-shell* (doto
                            (.openChannel net.licenser.ssh/*jsch-session* "shell")
                            (.setInputStream nil)
                            (.setOutputStream nil))]
     (binding [*jsch-in* (reader (.getInputStream *jsch-shell*))
             *jsch-out* (writer (.getOutputStream *jsch-shell*))]
       (doto *jsch-shell*
         (.connect))
       (try
         (do
           (shell-wait-for-newline)
           ~@body)
         (finally
           (.disconnect *jsch-shell*))))))


(defn shell-readln
  ([]
    (loop [s (new StringBuilder)]
      (if (or (.ready *jsch-in*) (Thread/sleep *jsch-shell-wait*) (.ready *jsch-in*))
        (let [c (char (.read *jsch-in*))]
          (if (= \newline c)
            (.toString s)
            (recur (.append s (char (.read *jsch-in*))))))
        (.toString s))))
  ([wait]
    (binding [*jsch-shell-wait* wait]
      (shell-readln))))

(defn shell-read-all
  ([]
  (loop [s (new StringBuilder)]
    (if (or (.ready *jsch-in*) (Thread/sleep *jsch-shell-wait*) (.ready *jsch-in*))
      (recur (.append s (char (.read *jsch-in*))))
      (.toString s))))
 ([wait]
    (binding [*jsch-shell-wait* wait]
      (shell-read-all))))


(defn shell-wait-for-newline
  []
  (.mark *jsch-in* 1024)
  (.readLine *jsch-in*)
  (.reset *jsch-in*))

(defn shell-println
  [s]
  (binding
    [*out* *jsch-out*]
    (println s)
    (shell-wait-for-newline)))

(defn shell-wait-for
  [regexp]
  (if (.available *jsch-in*)
    :empty
    (first (line-seq *jsch-in*))))

(defn shell-exec
  ([s]
  (shell-println s)
  (shell-wait-for-newline)
  (shell-read-all))
([s wait]
    (binding [*jsch-shell-wait* wait]
      (shell-exec s))))


(comment

  (use 'net.licenser.ssh)
  (use 'net.licenser.ssh.shell)

  (with-session "user" "pass" "host45"
    (with-shell
      (println (shell-read-all))
      (println (shell-exec "exec bash"))
      (println (shell-exec "export PS1=\"\\h:\\w# \""))
      (println (shell-exec "ls"))))

  )