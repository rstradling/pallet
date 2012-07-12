(ns pallet.actions.direct.rsync-test
  (:use
   clojure.test
   [pallet.actions :only [rsync rsync-directory]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [group-spec lift with-admin-user]]
   [pallet.core.user :only [*admin-user*]]
   [pallet.common.logging.logutils :only [logging-threshold-fixture]]
   [pallet.stevedore :only [script]]
   [pallet.test-utils :only [make-localhost-compute test-username]])
  (:require
   pallet.actions.direct.rsync
   [pallet.action :as action]
   [pallet.phase :as phase]
   [pallet.stevedore :as stevedore]
   [pallet.utils :as utils]
   [clojure.java.io :as io]))

(use-fixtures :once (logging-threshold-fixture))


