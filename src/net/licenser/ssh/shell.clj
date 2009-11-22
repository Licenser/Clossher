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
  "Reads one line from the shell, a line is defined as either ending with a
\newline or no input for 500 mili seconds this time can be changed by either
binding *jsch-shell-wait* or passing the wait argument."
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
  "Equivalent to shell-readln, just that it continues to read untill the wait
time is exeeded. This is helpfull to read the output of a entire command."
  ([]
  (loop [s (new StringBuilder)]
    (if (or (.ready *jsch-in*) (Thread/sleep *jsch-shell-wait*) (.ready *jsch-in*))
      (recur (.append s (char (.read *jsch-in*))))
      (.toString s))))
 ([wait]
    (binding [*jsch-shell-wait* wait]
      (shell-read-all))))


(defn shell-wait-for-newline
  "Waits untill a newline is printed on the shell."
  []
  (.mark *jsch-in* 1024)
  (.readLine *jsch-in*)
  (.reset *jsch-in*))

(defn shell-println
  "Writes s to the shell, adding a newline in the end."
  [s]
  (binding
    [*out* *jsch-out*]
    (println s)
    (shell-wait-for-newline)))

(defn shell-wait-for
  "Not implemented yet!"
  [regexp]
  (if (.available *jsch-in*)
    :empty
    (first (line-seq *jsch-in*))))

(defn shell-exec
  "Executes a command on the shell and reads it's input by calling 
shell-read-all. If your command is particuallary slow you might want to increase
the wait time by passing the wait argument."
  ([s]
  (shell-println s)
  (shell-wait-for-newline)
  (shell-read-all))
([s wait]
    (binding [*jsch-shell-wait* wait]
      (shell-exec s))))