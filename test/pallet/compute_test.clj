(ns pallet.compute-test
  (:use
   pallet.compute
   clojure.test)
  (:require
   slingshot.test)
  (:import slingshot.ExceptionInfo))

(deftest packager-test
  (is (= :aptitude (packager {:os-family :ubuntu})))
  (is (= :yum (packager {:os-family :centos})))
  (is (= :pkgin (packager {:os-family :system-v})))
  (is (= :portage (packager {:os-family :gentoo}))))

(deftest base-distribution-test
  (is (= :debian (base-distribution {:os-family :ubuntu})))
  (is (= :rh (base-distribution {:os-family :centos})))
  (is (= :gentoo (base-distribution {:os-family :gentoo})))
  (is (= :arch (base-distribution {:os-family :arch})))
  (is (= :suse (base-distribution {:os-family :suse})))
  (is (= :system-v (base-distribution {:os-family :sunos}))))


(defmulti-os testos [session])
(defmethod testos :linux [session] :linux)
(defmethod testos :debian [session] :debian)
(defmethod testos :rh-base [session] :rh-base)
(defmethod testos :system-v [session] :system-v)

(deftest defmulti-os-test
  (is (= :linux (testos {:server {:image {:os-family :arch}}})))
  (is (= :rh-base (testos {:server {:image {:os-family :centos}}})))
  (is (= :debian (testos {:server {:image {:os-family :debian}}})))
  (is (= :system-v (testos {:server {:image {:os-family :sunos}}})))
  (is (thrown+? map? (testos {:server {:image {:os-family :unspecified}}}))))
