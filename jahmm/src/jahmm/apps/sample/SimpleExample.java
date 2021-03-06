/*
 * Copyright (c) 2004-2009, Jean-Marc François. All Rights Reserved.
 * Licensed under the New BSD license.  See the LICENSE file.
 */
package jahmm.apps.sample;


/*
 * SimpleExample.java: A simple example file for the jahmm package.
 *
 * Written by Jean-Marc Francois.
 *
 * The content of this file is public-domain.
 *
 * Compile with the following command:
 *      javac SimpleExample.java
 * And run with:
 *      java SimpleExample
 *
 * This file (or a newer version) can be found at jahmm website:
 *      http://jahmm.googlecode.com/
 *
 *
 * Changelog:
 * 2004-03-01: Creation. (JMF)
 * 2004-04-27: Adapted to Jahmm 0.2.4. (JMF)
 * 2005-01-31: Minor adaption for release 0.3.0. (JMF)
 * 2005-11-25: Adapted to Jahmm 0.5.0. (JMF)
 * 2006-01-11: Now prints the initial/learnt HMMs. (JMF)
 * 2006-02-05: Small modification to avoid 'unchecked casting'. (JMF)
 * 2006-02-05: Renamed, adapted to v0.6.0. (JMF)
 * 2009-06-06: Updated comments with new website URL
 */
import jahmm.RegularHmmBase;
import jahmm.draw.HmmDotDrawer;
import jahmm.learn.RegularBaumWelchLearnerBase;
import jahmm.observables.Observation;
import jahmm.observables.ObservationEnum;
import jahmm.observables.OpdfEnum;
import jahmm.observables.OpdfEnumFactory;
import jahmm.toolbox.KullbackLeiblerDistanceCalculator;
import jahmm.toolbox.RegularMarkovGeneratorBase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jutils.collections.CollectionUtils;

/**
 * This class demonstrates how to build a HMM with known parameters, how to
 * generate a sequence of observations given a HMM, how to learn the parameters
 * of a HMM given observation sequences, how to compute the probability of an
 * observation sequence given a HMM and how to convert a HMM to a Postscript
 * drawing.
 * <p>
 * The example used is that of a wireless computer network that can experience
 * jamming. When the wireless medium is (resp. is not) jammed, a lot (resp. few)
 * packets are lost. Thus, the HMMs built here have two states (jammed/not
 * jammed).
 */
public class SimpleExample {

    public static final int OBSERVATION_COUNT = 200;
    public static final int OBSERVATION_LENGTH = 100;

    /**
     *
     * @param argv
     * @throws IOException
     */
    static public void main(String[] argv)
            throws java.io.IOException {
        /* Build a HMM and generate observation sequences using this HMM */
        RegularHmmBase<ObservationEnum<Packet>> hmm = buildHmm();

        List<List<ObservationEnum<Packet>>> sequences;
        sequences = generateSequences(hmm);

        System.out.println(CollectionUtils.deepToString(sequences));

        /* Baum-Welch learning */
        RegularBaumWelchLearnerBase<ObservationEnum<Packet>, RegularHmmBase<ObservationEnum<Packet>>> bwl = new RegularBaumWelchLearnerBase<>();

        RegularHmmBase<ObservationEnum<Packet>> learntHmm = buildInitHmm();

        // This object measures the distance between two HMMs
        KullbackLeiblerDistanceCalculator klc
                = new KullbackLeiblerDistanceCalculator();

        // Incrementally improve the solution
        for (int i = 0; i < 10; i++) {
            System.out.println(learntHmm);
            System.out.println("Distance at iteration " + i + ": "
                    + klc.distance(learntHmm, hmm));
            learntHmm = bwl.iterate(learntHmm, sequences);
        }

        System.out.println("Resulting HMM:\n" + learntHmm);

        /* Computing the probability of a sequence */
        ObservationEnum<Packet> packetOk = Packet.OK.observation();
        ObservationEnum<Packet> packetLoss = Packet.LOSS.observation();

        List<ObservationEnum<Packet>> testSequence
                = new ArrayList<>();
        testSequence.add(packetOk);
        testSequence.add(packetOk);
        testSequence.add(packetLoss);

        System.out.println("Sequence probability: "
                + learntHmm.probability(testSequence));

        /* Write the final result to a 'dot' (graphviz) file. */
        HmmDotDrawer.Instance.write(learntHmm, "learntHmm.dot");
    }

    /* The HMM this example is based on */
    static RegularHmmBase<ObservationEnum<Packet>> buildHmm() {
        RegularHmmBase<ObservationEnum<Packet>> hmm
                = new RegularHmmBase<>(2,
                        new OpdfEnumFactory<>(Packet.class));

        hmm.setPi(0, 0.95);
        hmm.setPi(1, 0.05);

        hmm.setOpdf(0, new OpdfEnum<>(Packet.class,
                new double[]{0.95, 0.05}));
        hmm.setOpdf(1, new OpdfEnum<>(Packet.class,
                new double[]{0.20, 0.80}));

        hmm.setAij(0, 1, 0.05);
        hmm.setAij(0, 0, 0.95);
        hmm.setAij(1, 0, 0.10);
        hmm.setAij(1, 1, 0.90);

        return hmm;
    }

    /* Initial guess for the Baum-Welch algorithm */
    static RegularHmmBase<ObservationEnum<Packet>> buildInitHmm() {
        RegularHmmBase<ObservationEnum<Packet>> hmm
                = new RegularHmmBase<>(2,
                        new OpdfEnumFactory<>(Packet.class));

        hmm.setPi(0, 0.50);
        hmm.setPi(1, 0.50);

        hmm.setOpdf(0, new OpdfEnum<>(Packet.class,
                new double[]{0.8, 0.2}));
        hmm.setOpdf(1, new OpdfEnum<>(Packet.class,
                new double[]{0.1, 0.9}));

        hmm.setAij(0, 1, 0.2);
        hmm.setAij(0, 0, 0.8);
        hmm.setAij(1, 0, 0.2);
        hmm.setAij(1, 1, 0.8);

        return hmm;
    }

    /* Generate several observation sequences using a HMM */
    static <O extends Observation> List<List<O>>
            generateSequences(RegularHmmBase<O> hmm) {
        RegularMarkovGeneratorBase<O, RegularHmmBase<O>> mg = new RegularMarkovGeneratorBase<>(hmm);

        List<List<O>> sequences = new ArrayList<>();
        for (int i = 0; i < OBSERVATION_COUNT; i++) {
            sequences.add(mg.observationSequence(OBSERVATION_LENGTH));
        }

        return sequences;
    }
    /* Possible packet reception status */

    /**
     *
     */
    public enum Packet {

        /**
         *
         */
        OK,
        /**
         *
         */
        LOSS;

        /**
         *
         * @return
         */
        public ObservationEnum<Packet> observation() {
            return new ObservationEnum<>(this);
        }
    };
}
