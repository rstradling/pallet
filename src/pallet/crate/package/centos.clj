(ns pallet.crate.package.centos
  "Actions for working with the centos repositories"
  (:use
   [pallet.actions :only [package package-source]]
   [pallet.crate :only [is-64bit?]]
   [pallet.monad :only [let-s]]
   [pallet.crate :only [def-plan-fn]]))

(def ^{:private true} centos-repo
  "http://mirror.centos.org/centos/%s/%s/%s/repodata/repomd.xml")

(def ^{:private true} centos-repo-key
  "http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-%s")

(def ^{:doc "Return the centos package architecture for the target node."}
  arch
  (let-s
    [is64bit is-64bit?]
    (if is64bit "x86_64" "i386")))

(def-plan-fn add-repository
  "Add a centos repository. By default, ensure that it has a lower than default
  priority."
  [& {:keys [version repository enabled priority]
      :or {version "5.5" repository "os" enabled 0 priority 50}}]
  [arch-str arch]
  (package "yum-priorities")
  (package-source
   (format "Centos %s %s %s" version repository arch-str)
   :yum {:url (format centos-repo version repository arch-str)
         :gpgkey (format centos-repo-key (str (first version)))
         :priority priority
         :enabled enabled}))
