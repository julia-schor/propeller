(ns propeller.problems.software.number-io
  (:require [propeller.genome :as genome]
            [propeller.push.interpreter :as interpreter]
            [propeller.push.state :as state]
            [propeller.push.utils :refer [get-stack-instructions]]
            [propeller.utils :as utils]
            [propeller.push.state :as state]
            [propeller.tools.math :as math]))

;; =============================================================================
;; Tom Helmuth, thelmuth@cs.umass.edu
;;
;; NUMBER IO PROBLEM
;;
;; This problem file defines the following problem:
;; There are two inputs, a float and an int. The program must read them in, find
;; their sum as a float, and print the result as a float.
;;
;; Problem Source: iJava (http://ijava.cs.umass.edu/)
;;
;; NOTE: input stack: in1 (float),
;;                    in2 (int)
;;       output stack: printed output
;; =============================================================================

;; =============================================================================
;; DATA DOMAINS
;;
;; A list of data domains. Each domain is a map containing a "set" of inputs
;; and two integers representing how many cases from the set should be used as
;; training and testing cases respectively. Each "set" of inputs is either a
;; list or a function that, when called, will create a random element of the set
;; =============================================================================

;; Random float between -100.0 and 100.0
(defn random-float [] (- (* (rand) 200) 100.0))

; Random integer between -100 and 100
(defn random-int [] (- (rand-int 201) 100.0))

(def instructions
  (utils/not-lazy
    (concat
      ;; stack-specific instructions
      (get-stack-instructions #{:float :integer :print})
      ;; input instructions
      (list :in1 :in2)
      ;; ERCs (constants)
      (list random-float random-int))))

(def train-and-test-data
  (let [inputs (vec (repeatedly 1025 #(vector (random-int) (random-float))))
        outputs (mapv #(apply + %) inputs)
        train-set {:inputs  (take 25 inputs)
                   :outputs (take 25 outputs)}
        test-set {:inputs  (drop 25 inputs)
                  :outputs (drop 25 outputs)}]
    {:train train-set
     :test  test-set}))

(defn error-function
  ([argmap individual]
   (error-function argmap individual :train))
  ([argmap individual subset]
   (let [program (genome/plushy->push (:plushy individual))
         data (get train-and-test-data subset)
         inputs (:inputs data)
         correct-outputs (:outputs data)
         outputs (map (fn [input]
                        (state/peek-stack
                          (interpreter/interpret-program
                            program
                            (assoc state/empty-state :input {:in1 (first input)
                                                             :in2 (last input)}
                                                     :output '(""))
                            (:step-limit argmap))
                          :output))
                      inputs)
         errors (map (fn [correct-output output]
                       (let [parsed-output (try (read-string output)
                                                (catch Exception e 1000.0))]
                         (min 1000.0 (math/abs (- correct-output parsed-output)))))
                     correct-outputs
                     outputs)]
     (assoc individual
       :behaviors outputs
       :errors errors
       :total-error (apply +' errors)))))
