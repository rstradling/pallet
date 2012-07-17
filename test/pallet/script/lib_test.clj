(ns pallet.script.lib-test
  (:use pallet.script.lib)
  (:use clojure.test)
  (:require
   [pallet.script :as script]
   [pallet.test-utils :as test-utils]
   [pallet.stevedore :as stevedore]
   ))

(use-fixtures
 :once
 test-utils/with-ubuntu-script-template
 test-utils/with-bash-script-language)

(deftest exit-test
  (is (= "exit 1"
         (stevedore/script (~exit 1)))))

(deftest rm-test
  (is (= "rm --force file1"
         (stevedore/script (~rm "file1" :force true)))))

(deftest mv-test
  (is (= "mv --backup=\"numbered\" file1 file2"
         (stevedore/script (~mv "file1" "file2" :backup :numbered)))))

(deftest ln-test
  (is (= "ln -s file1 file2"
         (stevedore/script (~ln "file1" "file2" :symbolic true)))))

(deftest chown-test
  (is (= "chown user1 file1"
         (stevedore/script (~chown "user1" "file1")))))

(deftest chgrp-test
  (is (= "chgrp group1 file1"
         (stevedore/script (~chgrp "group1" "file1")))))

(deftest chmod-test
  (is (= "chmod 0666 file1"
         (stevedore/script (~chmod "0666" "file1")))))

(deftest tmpdir-test
  (is (= "${TMPDIR-/tmp}"
         (stevedore/script (~tmp-dir)))))

(deftest normalise-md5-test
  (is (= (str "if egrep '^[a-fA-F0-9]+$' abc.md5; then"
              " echo \"  $(basename abc.md5 | sed -e s/.md5//)\" >> abc.md5;fi")
         (stevedore/script (~normalise-md5 abc.md5)))))

(deftest md5sum-verify-test
  (is (= "( cd $(dirname abc.md5) && md5sum --quiet --check $(basename abc.md5) )"
         (stevedore/script (~md5sum-verify abc.md5)))))

(deftest heredoc-test
  (is (= "{ cat > somepath <<EOFpallet\nsomecontent\nEOFpallet\n }"
         (stevedore/script (~heredoc "somepath" "somecontent" {})))))

(deftest heredoc-literal-test
  (is (= "{ cat > somepath <<'EOFpallet'\nsomecontent\nEOFpallet\n }"
         (stevedore/script (~heredoc "somepath" "somecontent" {:literal true})))))

(deftest sed-file-test
  (testing "explicit separator"
    (is (= "sed -i -e \"s|a|b|\" path"
           (stevedore/script (~sed-file "path" {"a" "b"} {:seperator "|"})))))
  (testing "single quotings"
    (is (= "sed -i -e 's/a/b/' path"
           (stevedore/script (~sed-file "path" {"a" "b"} {:quote-with "'"})))))
  (testing "computed separator"
    (is (= "sed -i -e \"s/a/b/\" path"
           (stevedore/script (~sed-file "path" {"a" "b"} {}))))
    (is (= "sed -i -e \"s_a/_b_\" path"
           (stevedore/script (~sed-file "path" {"a/" "b"} {}))))
    (is (= "sed -i -e \"s_a_b/_\" path"
           (stevedore/script (~sed-file "path" {"a" "b/"} {}))))
    (is (= "sed -i -e \"s*/_|:%!@*b*\" path"
           (stevedore/script (~sed-file "path" {"/_|:%!@" "b"} {})))))
  (testing "restrictions"
    (is (= "sed -i -e \"1 s/a/b/\" path"
           (stevedore/script (~sed-file "path" {"a" "b"} {:restriction "1"}))))
    (is (= "sed -i -e \"/a/ s/a/b/\" path"
           (stevedore/script (~sed-file "path" {"a" "b"} {:restriction "/a/"})))))
  (testing "other commands"
    (is (= "sed -i -e \"1 a\" path"
           (stevedore/script (~sed-file "path" "a" {:restriction "1"}))))))

(deftest make-temp-file-test
  (is (= "$(mktemp \"prefixXXXXX\")"
         (stevedore/script (~make-temp-file "prefix")))))

(deftest download-file-test
  (is (stevedore/script (~download-file "http://server.com/" "/path")))
  (is (= "if hash curl 2>&-; then curl -o \"/path\" --retry 5 --silent --show-error --fail --location --proxy localhost:3812 \"http://server.com/\";else\nif hash wget 2>&-; then wget -O \"/path\" --tries 5 --no-verbose -e \"http_proxy = http://localhost:3812\" -e \"ftp_proxy = http://localhost:3812\" \"http://server.com/\";else\necho No download utility available\nexit 1\nfi\nfi"
         (stevedore/script
          (~download-file
           "http://server.com/" "/path" :proxy "http://localhost:3812"))))
  (is (= "if hash curl 2>&-; then curl -o \"/path\" --retry 5 --silent --show-error --fail --location --proxy localhost:3812 --insecure \"http://server.com/\";else\nif hash wget 2>&-; then wget -O \"/path\" --tries 5 --no-verbose -e \"http_proxy = http://localhost:3812\" -e \"ftp_proxy = http://localhost:3812\" --no-check-certificate \"http://server.com/\";else\necho No download utility available\nexit 1\nfi\nfi"
         (stevedore/script
          (~download-file
           "http://server.com/" "/path" :proxy "http://localhost:3812"
           :insecure true)))
      ":insecure should disable ssl checks"))

(deftest download-request-test
  (is (= "curl -o \"p\" --retry 3 --silent --show-error --fail --location -H \"n: v\" \"http://server.com\""
         (let [request {:headers {"n" "v"}
                        :endpoint (java.net.URI. "http://server.com")}]
           (stevedore/script (~download-request "p" ~request))))))

(deftest mkdir-test
  (is (= "mkdir -p dir"
         (stevedore/script (~mkdir "dir" :path ~true)))))

;;; user management

(deftest create-user-test
  (is (= "/usr/sbin/useradd --create-home user1"
         (stevedore/script (~create-user "user1"  ~{:create-home true}))))
  (is (= "/usr/sbin/useradd --system user1"
         (stevedore/script (~create-user "user1"  ~{:system true}))))
  (testing "system on rh"
    (script/with-script-context [:centos]
      (is (= "/usr/sbin/useradd -r user1"
             (stevedore/script (~create-user "user1"  ~{:system true})))))))

(deftest modify-user-test
  (is (= "/usr/sbin/usermod --home \"/home2/user1\" --shell \"/bin/bash\" user1"
         (stevedore/script
          (~modify-user
           "user1"  ~{:home "/home2/user1" :shell "/bin/bash"})))))


;;; package management

(deftest update-package-list-test
  (is (= "aptitude update || true"
         (script/with-script-context [:aptitude]
           (stevedore/script (~update-package-list)))))
  (is (= "yum makecache -q"
         (script/with-script-context [:yum]
           (stevedore/script (~update-package-list)))))
  (is (= "zypper refresh"
         (script/with-script-context [:zypper]
           (stevedore/script (~update-package-list)))))
  (is (= "pacman -Sy --noconfirm --noprogressbar"
         (script/with-script-context [:pacman]
           (stevedore/script (~update-package-list)))))
  (is (= "pkgin -y update"
        (script/with-script-context [:pkgin]
          (stevedore/script (~update-package-list))))))

(deftest upgrade-all-packages-test
  (is (= "aptitude upgrade -q -y"
         (script/with-script-context [:aptitude]
           (stevedore/script (~upgrade-all-packages)))))
  (is (= "yum update -y -q"
         (script/with-script-context [:yum]
           (stevedore/script (~upgrade-all-packages)))))
  (is (= "zypper update -y"
         (script/with-script-context [:zypper]
           (stevedore/script (~upgrade-all-packages)))))
  (is (= "pacman -Su --noconfirm --noprogressbar"
         (script/with-script-context [:pacman]
           (stevedore/script (~upgrade-all-packages)))))
  (is (= "pkgin -y full-upgrade"
      (script/with-script-context [:pkgin]
        (stevedore/script (~upgrade-all-packages))))))

(deftest install-package-test
  (is (= "aptitude install -q -y java && aptitude show java"
         (script/with-script-context [:aptitude]
           (stevedore/script (~install-package "java")))))
  (is (= "yum install -y -q java"
         (script/with-script-context [:yum]
           (stevedore/script (~install-package "java")))))
  (is (= "pkgin -y install java"
        (script/with-script-context [:pkgin]
          (stevedore/script (~install-package "java"))))))

(deftest list-installed-packages-test
  (is (= "aptitude search \"~i\""
         (script/with-script-context [:aptitude]
           (stevedore/script (~list-installed-packages)))))
  (is (= "yum list installed"
         (script/with-script-context [:yum]
           (stevedore/script (~list-installed-packages)))))
  (is (= "pkgin list"
         (script/with-script-context [:pkgin]
          (stevedore/script (~list-installed-packages))))))




;;; test hostinfo

(deftest dnsdomainname-test
  (is (= "$(dnsdomainname)"
         (stevedore/script (~dnsdomainname)))))

(deftest dnsdomainname-test
  (is (= "$(hostname --fqdn)"
         (stevedore/script (~hostname :fqdn true)))))

(deftest nameservers-test
  (is (= "$(grep nameserver /etc/resolv.conf | cut -f2)"
         (stevedore/script (~nameservers)))))



;;; test filesystem paths

(defmacro mktest
  [os-family f path]
  `(is (= ~path
          (script/with-script-context [~os-family]
            (stevedore/script
             ( ~(list `unquote f)))))))

(deftest etc-default-test
  (mktest :ubuntu etc-default "/etc/default")
  (mktest :debian etc-default "/etc/default")
  (mktest :centos etc-default "/etc/sysconfig")
  (mktest :fedora etc-default "/etc/sysconfig")
  (mktest :os-x etc-default "/etc/defaults")
  (mktest :system-v etc-default "/etc/defaults")
  (mktest :sunos etc-default "/etc/defaults"))

