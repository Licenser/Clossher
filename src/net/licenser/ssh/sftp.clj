(ns net.licenser.ssh.sftp
  (:use [net.licenser.ssh :only (exec)])
  (:import java.io.File))

(declare *jsch-sftp*)

(defmacro with-sftp
  "This macro encapsulates a sftp session by creating a binding.
sftp-* functions can be used within this macro"
  [& body]
  `(binding [*jsch-sftp* (.openChannel net.licenser.ssh/*jsch-session* "sftp")]
     (.connect *jsch-sftp*)
     (try
      (do ~@body)
      (finally (.disconnect *jsch-sftp*)))))


(defn sftp-put
  "Puts the src file to the dest. If one argument is used it puts the file with
the same name in the remote pwd."
  ([src dest]
    (.put *jsch-sftp* src dest))
  ([src]
    (.put *jsch-sftp* src)))

(defn sftp-pwd []
  "Returns the remote pwd."
  (.pwd *jsch-sftp*))

(defn sftp-lpwd []
  "Returns the local pwd."
  (.lpwd *jsch-sftp*))


(defn sftp-exec [command]
  "Encapsulates exec to run in the sftp sessions pwd. by adding cd <pwd>;"
  (exec (str "cd " (sftp-pwd) ";" command)))

(defn sftp-get
  "Gets a src file and stores it at dest. If only one argument is proivided it
puts the file in the lpwd."
  ([src dest]
    (.get *jsch-sftp* src dest))
  ([src]
    (.get *jsch-sftp* src 
      (str (sftp-lpwd) "/"
        (.getName (new File
                    (if (= \/ (first src))
                      src
                      (str (sftp-pwd "/" src))) ))))))

(defn sftp-lcd [path]
  "Changes the local pwd."
  (.lcd *jsch-sftp* path))

(defn sftp-cd [path]
  "Changes the remote pwd."
  (.cd *jsch-sftp* path))

(defn sftp-mkdir [path]
  "Creates a directory on the remote host"
  (.mkdir *jsch-sftp* path))

(defn sftp-ls [path]
  "Lists files on the remote host."
  (.ls *jsch-sftp* path))

(defn sftp-rm []
  "Deletes files on the remote host."
  (.rm *jsch-sftp*))

(defn sftp-rmdir []
  "Deletes a directory on the remote host."
  (.rmdir *jsch-sftp*))

(defn sftp-rename []
  "Renames files on the remote host."
  (.rmdir *jsch-sftp*))

(defn sftp-chgrp [gid path]
  "Changes the group of a file on the remote host, gid is the group ID."
  (.chgrp *jsch-sftp* gid path))

(defn sftp-chmod [mod path]
  "Chans the accessrights on a remode file."
  (.chmod *jsch-sftp* mod path))

(defn sftp-chown [uid path]
  "Changes the owner on a remote file, uid is the new users user ID"
  (.chown *jsch-sftp* uid path))