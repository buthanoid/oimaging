/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oitools.model;

import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oitools.util.CombUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * This class visit the table for a given oifits file to process some computation on them. 
 */
public final class Analyzer implements ModelVisitor {

    /** Logger */
    private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Analyzer.class.getName());
    /* members */
    /** cached log debug flag */
    private final boolean isLogDebug = logger.isLoggable(Level.FINE);
    /** cached combinations for baselines */
    private final Map<Integer, List<int[]>> comb2TByLen = new HashMap<Integer, List<int[]>>(8);
    /** cached combinations for triplets */
    private final Map<Integer, List<int[]>> comb3TByLen = new HashMap<Integer, List<int[]>>(8);

    /**
     * Protected constructor
     */
    Analyzer() {
        super();
    }

    /**
     * Process the given OIFitsFile element with this visitor implementation :
     * fill the internal buffer with file information
     * @param oiFitsFile OIFitsFile element to visit
     */
    @Override
    public void visit(final OIFitsFile oiFitsFile) {
        final long start = System.nanoTime();

        // reset cached analyzed data:
        oiFitsFile.setChanged();

        // TODO: process keywords in primary HDU (image) ?
        // process OITarget table (mandatory but incorrect files can happen):
        if (oiFitsFile.hasOiTarget()) {
            process(oiFitsFile.getOiTarget());
        }

        // process OIWavelength tables:
        for (final OIWavelength oiWavelength : oiFitsFile.getOiWavelengths()) {
            process(oiWavelength);
        }

        // process OIArray tables:
        for (final OIArray oiArray : oiFitsFile.getOiArrays()) {
            process(oiArray);
        }

        // finally: process OIData tables:
        for (final OIData oiData : oiFitsFile.getOiDataList()) {
            process(oiData);
        }

        if (isLogDebug) {
            logger.fine("process: OIFitsFile[" + oiFitsFile.getAbsoluteFilePath() + "] oiDataPerTarget " + oiFitsFile.getOiDataPerTarget());
            logger.fine("process: OIFitsFile[" + oiFitsFile.getAbsoluteFilePath() + "] granules: " + oiFitsFile.getOiDataPerGranule().keySet());
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Analyzer.visit : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");
        }
    }

    /**
     * Process the given OITable element with this visitor implementation :
     * fill the internal buffer with table information
     * @param oiTable OITable element to visit
     */
    @Override
    public void visit(final OITable oiTable) {
        if (oiTable instanceof OIData) {
            process((OIData) oiTable);
        } else if (oiTable instanceof OIWavelength) {
            process((OIWavelength) oiTable);
        } else if (oiTable instanceof OIArray) {
            process((OIArray) oiTable);
        } else if (oiTable instanceof OITarget) {
            process((OITarget) oiTable);
        }
    }

    /* --- process OITable --- */
    /**
     * Process the given OIData table
     * @param oiData OIData table to process
     */
    private void process(final OIData oiData) {
        if (isLogDebug) {
            logger.fine("process: OIData[" + oiData + "] OIWavelength range: " + Arrays.toString(oiData.getEffWaveRange()));
        }

        // reset cached analyzed data:
        oiData.setChanged();

        final int nRows = oiData.getNbRows();
        final int nWaves = oiData.getNWave();

        // Distinct Target Id:
        final short[] targetIds = oiData.getTargetId();

        final OITarget oiTarget = oiData.getOiTarget();
        // TODO: no OITarget but have targetId => create a Target("Missing[<targetId>]"):
        final Map<Short, Target> targetIdToTarget = (oiTarget != null) ? oiTarget.getTargetIdToTarget() : null;

        final OIWavelength oiWavelength = oiData.getOiWavelength();
        // TODO: no OIWaveLength but have insname => create an InstrumentMode("Missing<insname>"):
        final InstrumentMode insMode = (oiWavelength != null) ? oiWavelength.getInstrumentMode() : InstrumentMode.UNDEFINED;

        // compute night ids:
        final double[] nightIds = oiData.getNightId();

        final Set<Short> distinctTargetId = oiData.getDistinctTargetId();
        final Map<Granule, Set<OIData>> oiDataPerGranule = oiData.getOIFitsFile().getOiDataPerGranule();

        // reused Granule:
        Granule g = new Granule();

        for (int i = 0; i < nRows; i++) {
            final Short targetId = Short.valueOf(targetIds[i]);
            distinctTargetId.add(targetId);

            // create Granules:
            final Target target = (targetIdToTarget != null) ? targetIdToTarget.get(targetId) : Target.UNDEFINED;

            g.set(target, insMode, NumberUtils.valueOf((int) nightIds[i]));

            Set<OIData> oiDataTables = oiDataPerGranule.get(g);
            if (oiDataTables == null) {
                oiDataTables = new LinkedHashSet<OIData>();
                oiDataPerGranule.put(g, oiDataTables);
                g = new Granule();
            }
            oiDataTables.add(oiData);
        }

        if (isLogDebug) {
            logger.fine("process: OIData[" + oiData + "] distinctTargetId " + distinctTargetId);
        }

        // Process station indexes:
        processStaIndex(oiData);

        // Count Flags:
        final boolean[][] flags = oiData.getFlag();

        int nFlagged = 0;
        boolean[] row;
        for (int i = 0, j; i < nRows; i++) {
            row = flags[i];
            for (j = 0; j < nWaves; j++) {
                if (row[j]) {
                    nFlagged++;
                }
            }
        }

        if (isLogDebug) {
            logger.fine("process: OIData[" + oiData + "] nFlagged: " + nFlagged);
        }
        oiData.setNFlagged(nFlagged);

        // FINALLY: Update list of tables per target:
        if (oiTarget != null) {
            final Map<String, List<OIData>> oiDataPerTarget = oiData.getOIFitsFile().getOiDataPerTarget();

            final Map<String, Short> targetToTargetId = oiTarget.getTargetToTargetId();

            String target;
            List<OIData> oiDataTables;
            for (Map.Entry<String, Short> entry : targetToTargetId.entrySet()) {
                if (distinctTargetId.contains(entry.getValue())) {
                    target = entry.getKey();

                    oiDataTables = oiDataPerTarget.get(target);
                    if (oiDataTables == null) {
                        oiDataTables = new ArrayList<OIData>();
                        oiDataPerTarget.put(target, oiDataTables);
                    }

                    oiDataTables.add(oiData);
                }
            }
        }
    }

    /**
     * Process the given OIArray table
     * @param oiArray OIArray table to process
     */
    private void process(final OIArray oiArray) {
        if (isLogDebug) {
            logger.fine("process: OIArray[" + oiArray + "]");
        }

        // reset cached analyzed data:
        oiArray.setChanged();

        final Map<Short, Integer> staIndexToRowIndex = oiArray.getStaIndexToRowIndex();

        final short[] staIndexes = oiArray.getStaIndex();

        Short staIndex;
        for (int i = 0, len = oiArray.getNbRows(); i < len; i++) {
            staIndex = Short.valueOf(staIndexes[i]);
            staIndexToRowIndex.put(staIndex, NumberUtils.valueOf(i));
        }

        // TODO: analyze the stations 
        // ie create value objects (comparable) to be able 
        // to merge OI_ARRAY tables (and use uniform Stations => staIndex)
        if (isLogDebug) {
            logger.fine("process: OIArray[" + oiArray + "] staIndexToRowIndex: " + staIndexToRowIndex);
        }
    }

    /**
     * Process the given OIWavelength table
     * @param oiWavelength OIWavelength table to process
     */
    private void process(final OIWavelength oiWavelength) {
        if (isLogDebug) {
            logger.fine("process: OIWavelength[" + oiWavelength + "]");
        }

        // reset cached analyzed data:
        oiWavelength.setChanged();

        final String insName = oiWavelength.getInsName();
        final int nbChannels = oiWavelength.getNWave();
        final float lambdaMin = oiWavelength.getEffWaveMin();
        final float lambdaMax = oiWavelength.getEffWaveMax();
        // Resolution = lambda / delta_lambda
        final float resPower = oiWavelength.getResolution();

        // TODO: extract only instrument Name ie (first alpha characters ?):
        final InstrumentMode insMode = new InstrumentMode(insName, nbChannels, lambdaMin, lambdaMax, resPower);
        oiWavelength.setInstrumentMode(insMode);

        if (isLogDebug) {
            logger.fine("process: OIWavelength[" + oiWavelength + "] range: [" + lambdaMin + ", " + lambdaMax + "]");
            logger.fine("process: OIWavelength[" + oiWavelength + "]\ninsMode: " + insMode);
        }
    }

    /**
     * Process the given OITarget table
     * @param oiTarget OITarget table to process
     */
    private void process(final OITarget oiTarget) {
        if (isLogDebug) {
            logger.fine("process: OITarget[" + oiTarget + "]");
        }

        // reset cached analyzed data:
        oiTarget.setChanged();

        // TargetId indexes:
        final Map<Short, Integer> targetIdToRowIndex = oiTarget.getTargetIdToRowIndex();
        final Map<String, Short> targetToTargetId = oiTarget.getTargetToTargetId();
        // Target indexes:
        final Map<Short, Target> targetIdToTarget = oiTarget.getTargetIdToTarget();
        final Map<Target, List<Short>> targetObjToTargetId = oiTarget.getTargetObjToTargetId();
        // Columns
        final short[] targetIds = oiTarget.getTargetId();
        final String[] targets = oiTarget.getTarget();

        for (int i = 0, len = oiTarget.getNbRows(); i < len; i++) {
            final Short targetId = Short.valueOf(targetIds[i]);

            targetIdToRowIndex.put(targetId, NumberUtils.valueOf(i));

            // may have name conflicts ie multiple ids for the same target name in OI_TARGET !
            targetToTargetId.put(targets[i], targetId);

            final Target target = oiTarget.createTarget(i);

            targetIdToTarget.put(targetId, target);

            List<Short> ids = targetObjToTargetId.get(target);
            if (ids == null) {
                ids = new ArrayList<Short>(1);
                targetObjToTargetId.put(target, ids);
            }
            ids.add(targetId);
        }

        if (isLogDebug) {
            logger.fine("process: OITarget[" + oiTarget + "] targetIdToRowIndex: " + targetIdToRowIndex);
            logger.fine("process: OITarget[" + oiTarget + "] targetToTargetId: " + targetToTargetId);
            logger.fine("process: OITarget[" + oiTarget + "] targetIdToTarget: " + targetIdToTarget);
            logger.fine("process: OITarget[" + oiTarget + "] targetObjToTargetId: " + targetObjToTargetId);
        }
    }

    // --- baseline / configuration processing
    /**
     * Process station indexes on the given OIData table
     * @param oiData OIData table to process
     */
    private void processStaIndex(final OIData oiData) {

        final int nRows = oiData.getNbRows();

        // StaIndex column:
        final short[][] staIndexes = oiData.getStaIndex();

        // distinct staIndex arrays:
        final Set<short[]> distinctStaIndex = oiData.getDistinctStaIndex();

        if (nRows != 0) {
            // Get size of StaIndex arrays once:
            final int staLen = staIndexes[0].length;

            final StationIndex staList = new StationIndex(staLen);

            final Map<StationIndex, short[]> mappingStaList = new HashMap<StationIndex, short[]>(128);

            short[] staIndex;
            short[] uniqueStaIndex;

            for (int i = 0, j; i < nRows; i++) {
                staIndex = staIndexes[i];

                // prepare Station index:
                staList.clear();

                for (j = 0; j < staLen; j++) {
                    staList.add(Short.valueOf(staIndex[j]));
                }

                // TODO: warning: StaIndex arrays are not sorted so 'AB' <> 'BA'
                // Find existing array:
                uniqueStaIndex = mappingStaList.get(staList);

                if (uniqueStaIndex == null) {
                    // not found:
                    // store same array instance present in row:
                    distinctStaIndex.add(staIndex);

                    // store mapping:
                    mappingStaList.put(new StationIndex(staList), staIndex);
                } else {
                    // store distinct instance (minimize array instances):
                    staIndexes[i] = uniqueStaIndex;
                }

                // TODO: add an extra (derived) column to store sorted StaIndex (equivalence)
                // and also have distinctSortedStaIndex (GUI ?)
            }
        }

        if (isLogDebug) {
            logger.fine("processStaIndex: OIData[" + oiData + "] distinctStaIndex:");

            for (short[] item : distinctStaIndex) {
                logger.fine("Baseline: " + Arrays.toString(item) + " = " + oiData.getStaNames(item));
            }
        }

        processStaConf(oiData);
    }

    /**
     * Process station configurations on the given OIData table
     * @param oiData OIData table to process
     */
    private void processStaConf(final OIData oiData) {

        final int nRows = oiData.getNbRows();

        if (nRows == 0) {
            return;
        }

        // local vars for performance:
        final boolean isDebug = isLogDebug;

        // Derived StaConf column:
        final short[][] staConfs = oiData.getStaConf();

        // distinct staIndex arrays:
        final Set<short[]> distinctStaIndex = oiData.getDistinctStaIndex();

        // distinct staConf arrays:
        final Set<short[]> distinctStaConf = oiData.getDistinctStaConf();

        // distinct Sorted staIndex arrays:
        final Set<List<Short>> sortedStaIndex = new HashSet<List<Short>>(distinctStaIndex.size()); // LinkedHashSet ??

        // Sorted sta index mapping (unique instances) to distinct station indexes (maybe more than 1 due to permutations):
        // note: allocate twice because of missing StaIndex arrays:
        final Map<List<Short>, List<short[]>> mappingSortedStaIndex = new HashMap<List<Short>, List<short[]>>(distinctStaIndex.size());

        // Get size of StaIndex arrays once:
        final int staLen = distinctStaIndex.iterator().next().length;

        // Use List<Short> for easier manipulation ie List.equals() use also Short.equals on all items (missing baselines) !
        List<short[]> equivStaIndexes;

        for (short[] staIndex : distinctStaIndex) {
            final List<Short> staList = new StationIndex(staLen);

            for (int i = 0; i < staLen; i++) {
                staList.add(Short.valueOf(staIndex[i]));
            }

            // sort station list:
            Collections.sort(staList);

            if (sortedStaIndex.add(staList)) {
                // Allocate 2 slots (for only 2 permutations mainly):
                equivStaIndexes = new ArrayList<short[]>(2);
                mappingSortedStaIndex.put(staList, equivStaIndexes);
            } else {
                equivStaIndexes = mappingSortedStaIndex.get(staList);

            }
            // add staIndex corresponding to sorted station list:
            equivStaIndexes.add(staIndex);
        }

        if (isDebug) {
            logger.fine("processStaConf: OIData[" + oiData + "] sortedStaIndex:");

            for (List<Short> item : sortedStaIndex) {
                logger.fine("StaIndex: " + item);
            }
        }

        if (sortedStaIndex.size() == 1) {
            final short[] staConf = toArray(sortedStaIndex.iterator().next());

            // single staIndex array = single configuration
            distinctStaConf.add(staConf);

            // Fill StaConf derived column:
            for (int i = 0; i < nRows; i++) {
                // store single station configuration:
                staConfs[i] = staConf;
            }

        } else {

            // Guess configurations:
            // simple algorithm works only on distinct values (Aspro2 for now)
            // but advanced one should in fact use baselines having same MJD (same time) !
            // mapping between staId (1 station) and its station node:
            final Map<Short, StationNode> staIndexNodes = new HashMap<Short, StationNode>(32);

            // loop on sorted StaxIndex Set:
            for (List<Short> staList : sortedStaIndex) {
                for (Short staId : staList) {
                    StationNode node = staIndexNodes.get(staId);

                    // create missing node:
                    if (node == null) {
                        node = new StationNode(staId);
                        staIndexNodes.put(staId, node);
                    }
                    // add present StaIndex array:
                    node.addStaList(staList);
                }
            }

            // convert map into list of nodes:
            final List<StationNode> nodes = new ArrayList<StationNode>(staIndexNodes.values());

            // sort station node on its related staIndex counts (smaller count first):
            Collections.sort(nodes);

            final int nodeLen = nodes.size();

            if (isDebug) {
                logger.fine("Initial StationNodes --------------------------------------");

                for (StationNode n : nodes) {
                    logger.fine("Station: " + n.staId + "\t(" + n.count + "):\t" + n.staLists);
                }
                logger.fine("--------------------------------------");
            }

            // StaIndex mapping (distinct present StaIndex arrays) to station configuration:
            final Map<short[], short[]> mappingStaConf = new HashMap<short[], short[]>(distinctStaIndex.size());

            // Missing StaIndex (removed baseline or triplets) ordered by natural StationIndex comparator:
            final Set<List<Short>> missingStaIndexes = new TreeSet<List<Short>>();

            // current guessed station configuration:
            final Set<Short> guessConf = new HashSet<Short>(8);

            final List<List<Short>> combStaLists = new ArrayList<List<Short>>();
            final StationIndex sortedConf = new StationIndex(10);
            // StaIndex from combination:
            final StationIndex cStaList = new StationIndex(staLen);

            final Map<Integer, List<int[]>> combByLen = (staLen == 3) ? comb3TByLen : comb2TByLen;
            Integer combKey;
            List<int[]> iCombs;

            int confLen;

            StationNode node, other;
            List<Short> item;

            int[] combination;
            StationIndex newStaIndex;
            Short staId;

            int j, k, n, len;
            Iterator<List<Short>> itStaList;

            boolean doProcess = true;
            int nPass = 0;

            // Grow pass: add missing baseline or triplets:
            final int maxStaIndex = CombUtils.comb(nodeLen, staLen);

            if (isDebug) {
                logger.fine("nodes= " + nodeLen + " - maxStaIndex= " + maxStaIndex);
            }

            // distinct Sorted staIndex arrays from combinations (present + all missing StaIndex):
            final Set<List<Short>> sortedCombStaIndex = new HashSet<List<Short>>(maxStaIndex);
            // add all present StaIndex:
            sortedCombStaIndex.addAll(sortedStaIndex);

            // distinct processed sorted staConf:
            final Set<List<Short>> distinctCombStaConf = new HashSet<List<Short>>(64);

            while (doProcess) {
                nPass++;
                doProcess = false;

                // Process first the last node (sorted so has top most relations):
                for (n = nodeLen - 1; n >= 0; n--) {
                    node = nodes.get(n);

                    // skip marked nodes:
                    if (!node.mark) {
                        // try another node ?
                        doProcess = true;

                        guessConf.add(node.staId);

                        // try using all stations to form a conf:
                        for (itStaList = node.staLists.iterator(); itStaList.hasNext();) {
                            item = itStaList.next();

                            for (k = 0; k < staLen; k++) {
                                guessConf.add(item.get(k));
                            }
                        }

                        // compute missing staIndexes:
                        sortedConf.clear();
                        sortedConf.addAll(guessConf);
                        guessConf.clear();
                        Collections.sort(sortedConf);

                        // check that this conf is different than other already processed:
                        if (!distinctCombStaConf.contains(sortedConf)) {
                            distinctCombStaConf.add(new StationIndex(sortedConf));

                            if (isDebug) {
                                logger.fine("Growing node: " + node.staId + " - sortedConf = " + sortedConf);
                            }

                            // see CombUtils for generics
                            confLen = sortedConf.size();

                            if (isDebug) {
                                logger.fine("Get iCombs with len = " + confLen);
                            }

                            // get permutations:
                            combKey = NumberUtils.valueOf(confLen);

                            iCombs = combByLen.get(combKey);

                            if (iCombs == null) {
                                iCombs = CombUtils.generateCombinations(combKey.intValue(), staLen); // 2T or 3T
                                combByLen.put(combKey, iCombs);
                            }

                            combStaLists.clear();

                            // iCombs is sorted so should keep array sorted as otherStaIds is also sorted !
                            for (j = 0, len = iCombs.size(); j < len; j++) {
                                combination = iCombs.get(j);

                                cStaList.clear();

                                for (k = 0; k < staLen; k++) {
                                    cStaList.add(sortedConf.get(combination[k]));
                                }

                                // only keep new StaIndex arrays (not present nor already handled):
                                if (!sortedCombStaIndex.contains(cStaList)) {
                                    newStaIndex = new StationIndex(cStaList);

                                    // add new StaIndex array in processed StaIndex:
                                    sortedCombStaIndex.add(newStaIndex);

                                    combStaLists.add(newStaIndex);
                                }
                            }

                            if (isDebug) {
                                logger.fine("node: " + node.staId + " - combStaLists = " + combStaLists);
                            }

                            // Test missing staIndex:
                            for (j = 0, len = combStaLists.size(); j < len; j++) {
                                item = combStaLists.get(j);

                                // test if present:
                                if (!sortedStaIndex.contains(item)) {
                                    missingStaIndexes.add(item);
                                }
                            }

                            // process all possible staList:
                            // note: node needs not to be modified as all its stations were used to form the combination (sortedconf)
                            // add missing baselines in other nodes:
                            for (j = 0, len = combStaLists.size(); j < len; j++) {
                                item = combStaLists.get(j);

                                for (k = 0; k < staLen; k++) {
                                    staId = item.get(k);

                                    // skip current node:
                                    if (staId != node.staId) {
                                        // add baseline:
                                        other = staIndexNodes.get(staId);

                                        // ensure other is not null:
                                        if (other.addStaList(item)) {
                                            // mark the other node to be processed (again):
                                            other.mark = false;
                                        }
                                    }
                                }
                            }
                        }

                        // mark this node as done:
                        node.mark = true;

                        // exit from this loop:
                        break;
                    }
                } // nodes

                if (doProcess) {
                    // sort station node on its related staIndex counts (smaller count first) after each pass:
                    Collections.sort(nodes);

                    if (isDebug) {
                        logger.fine("Current StationNodes --------------------------------------");
                        for (StationNode s : nodes) {
                            logger.fine("Station[" + s.mark + "]: " + s.staId + "\t(" + s.count + "):\t" + s.staLists);
                        }
                        logger.fine("--------------------------------------");
                    }
                }

            } // until doProcess

            if (isDebug) {
                logger.fine("grow " + nPass + " pass: done");
            }

            // Process clusters pass:
            doProcess = true;
            nPass = 0;

            while (doProcess) {
                nPass++;
                doProcess = false;

                // Process only the first node (sorted so less relations):
                for (n = 0; n < nodeLen; n++) {
                    node = nodes.get(n);

                    // skip empty nodes:
                    if (node.count > 0) {
                        // try another node ?
                        doProcess = true;

                        guessConf.add(node.staId);

                        // try using all stations to form a conf (even with missing baselines):
                        for (itStaList = node.staLists.iterator(); itStaList.hasNext();) {
                            item = itStaList.next();

                            for (k = 0; k < staLen; k++) {
                                guessConf.add(item.get(k));
                            }
                        }

                        // compute missing staIndexes:
                        sortedConf.clear();
                        sortedConf.addAll(guessConf);
                        guessConf.clear();
                        Collections.sort(sortedConf);

                        if (isDebug) {
                            logger.fine("Processing node: " + node.staId + " - sortedConf = " + sortedConf);
                        }

                        combStaLists.clear();

                        // see CombUtils for generics
                        confLen = sortedConf.size();

                        if (isDebug) {
                            logger.fine("Get iCombs with len = " + confLen);
                        }

                        // TODO: remove all that code as grow pass has completed everything so don't test combination anymore ?
                        // get permutations:
                        combKey = NumberUtils.valueOf(confLen);

                        iCombs = combByLen.get(combKey);

                        if (iCombs == null) {
                            iCombs = CombUtils.generateCombinations(combKey.intValue(), staLen);
                            combByLen.put(combKey, iCombs);
                        }

                        // iCombs is sorted so should keep array sorted as otherStaIds is also sorted !
                        for (j = 0, len = iCombs.size(); j < len; j++) {
                            combination = iCombs.get(j);

                            newStaIndex = new StationIndex(staLen);

                            for (k = 0; k < staLen; k++) {
                                newStaIndex.add(sortedConf.get(combination[k]));
                            }

                            combStaLists.add(newStaIndex);
                        }

                        if (isDebug) {
                            logger.fine("node: " + node.staId + " - combStaLists = " + combStaLists);
                        }

                        // consider conf always valid:
                        final short[] staConf = toArray(sortedConf);

                        // add this configuration:
                        distinctStaConf.add(staConf);

                        // add mappings:
                        for (j = 0, len = combStaLists.size(); j < len; j++) {
                            item = combStaLists.get(j);

                            equivStaIndexes = mappingSortedStaIndex.get(item);

                            if (equivStaIndexes != null) {
                                // only store staConf for present baselines / triplets (some may be missing):
                                for (short[] staIndex : equivStaIndexes) {
                                    mappingStaConf.put(staIndex, staConf);
                                }
                            }
                        }

                        // remove all staList in node:
                        node.clear();

                        // remove all possible staIndex in other nodes:
                        for (j = 0, len = combStaLists.size(); j < len; j++) {
                            item = combStaLists.get(j);

                            for (k = 0; k < staLen; k++) {
                                staId = item.get(k);

                                if (staId != node.staId) {
                                    // remove baseline:
                                    other = staIndexNodes.get(staId);

                                    if (other != null) {
                                        other.removeStaList(item);
                                    }
                                }
                            }
                        }

                        // exit from this loop:
                        break;
                    }
                } // nodes

                if (doProcess) {
                    // sort station node on its related staIndex counts (smaller count first) after each pass:
                    Collections.sort(nodes);

                    if (isDebug) {
                        logger.fine("Current StationNodes --------------------------------------");
                        for (StationNode s : nodes) {
                            logger.fine("Station: " + s.staId + "\t(" + s.count + "):\t" + s.staLists);
                        }
                        logger.fine("--------------------------------------");
                    }
                }

            } // until doProcess

            if (isDebug) {
                logger.fine("process " + nPass + " pass: done");
            }

            // Report missing StaIndex arrays:
            if (!missingStaIndexes.isEmpty() && logger.isLoggable(Level.FINE)) {
                len = missingStaIndexes.size();

                final int itLen = (staLen * 3 + 2);
                final int nPerLine = 120 / itLen;
                final StringBuilder sb = new StringBuilder(len * itLen + len / nPerLine + 100);

                sb.append("processStaConf: OIData[").append(oiData.toString()).append("]:\n Missing ");
                sb.append(len).append(" / ").append(sortedStaIndex.size()).append(" StaIndex arrays:\n");

                sb.append('[');

                for (n = 0, k = 0, itStaList = missingStaIndexes.iterator(); itStaList.hasNext(); n++) {
                    item = itStaList.next();

                    sb.append('[');
                    for (j = 0; j < staLen; j++) {
                        sb.append(item.get(j)).append(", ");
                    }
                    sb.setLength(sb.length() - 2);
                    sb.append(']');

                    if (n != len) {
                        sb.append(", ");
                    }

                    k++;
                    if (k == nPerLine) {
                        sb.append("\n");
                        k = 0;
                    }
                }
                sb.append(']');

                // TODO: sort missing StaIndex arrays (+ formatting ?)
                logger.info(sb.toString());
            }

            // FINALLY: Fill StaConf derived column:
            // StaIndex column:
            final short[][] staIndexes = oiData.getStaIndex();

            short[] staIndex;
            for (int i = 0; i < nRows; i++) {
                staIndex = staIndexes[i];

                // store station configuration according to mapping (should be not null):
                staConfs[i] = mappingStaConf.get(staIndex);

                if (staConfs[i] == null) {
                    logger.warning("MISSING station configuration for station index:" + oiData.getStaNames(staIndex) + " !");

                }
            }
        }

        if (isDebug) {
            logger.fine("processStaConf: OIData[" + oiData + "] distinctStaConf:");

            for (short[] item : distinctStaConf) {
                logger.fine("StaConf: " + Arrays.toString(item) + " = " + oiData.getStaNames(item));
            }
        }
    }

    /**
     * Convert a station list to array
     * @param staList station list to convert
     * @return station index array
     */
    private static short[] toArray(final List<Short> staList) {
        final short[] staIndex = new short[staList.size()];

        int i = 0;
        for (Short staId : staList) {
            staIndex[i++] = staId.shortValue();
        }

        return staIndex;

    }

    /**
     * StationNode represents a graph node (station id) with its relations (staIndex arrays)
     */
    private static final class StationNode implements Comparable<StationNode> {

        /* members */
        /** station id */
        final Short staId;
        /** relation count (in sync with list) */
        int count = 0;
        /** 
         * distinct station lists (relations)
         * note: it always contains StationIndex implementations for performance
         */
        final Set<List<Short>> staLists = new HashSet<List<Short>>();
        /** mark flag used by grow pass */
        boolean mark = false;

        /**
         * Default constructor
         * @param staId station id
         */
        StationNode(final Short staId) {
            this.staId = staId;
        }

        /**
         * Clear that station node (recycle)
         */
        void clear() {
            staLists.clear();
            count = 0;
        }

        /**
         * Add a station list to this station node (relation) if missing and update relation count
         * @param staIndex station list to add
         * @return true if added; false if already present
         */
        boolean addStaList(final List<Short> staIndex) {
            if (staLists.add(staIndex)) {
                count++;
                return true;
            }
            return false;
        }

        /**
         * Remove the given station list from this station node (relation) if present and update relation count
         * @param staIndex station list to remove
         */
        void removeStaList(final List<Short> staIndex) {
            if (staLists.remove(staIndex)) {
                // note: same baseline can be removed several times because it belongs to several configurations ?
                count--;
            }
        }

        /**
         * Compare this station node with another station node
         * @param other another station node
         * @return comparison result (based on count and station id)
         */
        @Override
        public int compareTo(final StationNode other) {
            int res = count - other.count;
            if (res == 0) {
                res = staId.compareTo(other.staId);
            }
            return res;
        }
    }

    /**
     * StationIndex represents a station list (baseline or triplets) as List<Short> to fix AbstractList performance issues on hashcode() and equals()
     * i.e. avoid allocating Iterator or ListIterator and cache hashcode
     */
    private static final class StationIndex extends ArrayList<Short> implements Comparable<StationIndex> {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        /* members */
        /** cached hashcode */
        int hashCode = -1;

        /**
         * Default constructor
         * @param initialCapacity initial capacity
         */
        StationIndex(final int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Copy constructor
         * @param stationIndex station index to copy
         */
        StationIndex(final StationIndex stationIndex) {
            super(stationIndex);
            // copy hashcode:
            this.hashCode = stationIndex.hashCode;
        }

        /**
         * Clear that station index (list) and hashcode
         */
        @Override
        public void clear() {
            super.clear();
            // reset hashcode:
            hashCode = -1;
        }

        /**
         * Optimized hash code value for this list.
         *
         * @return the hash code value for this list
         */
        @Override
        public int hashCode() {
            // cache the computed value
            if (hashCode == -1) {
                // from super hashcode()
                int hash = 1;
                for (int i = 0, len = size(); i < len; i++) {
                    // note: short is never null
                    hash = 31 * hash + get(i).intValue();
                }
                hashCode = hash;
            }
            return hashCode;
        }

        /**
         * Optimized equals implementation for station indexes
         * @param o another StationIndex instance
         * @return true if equals (same stations and same ordering)
         */
        @Override
        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof StationIndex)) {
                return false;
            }
            final StationIndex other = (StationIndex) o;

            final int len = size();
            /*
             // not necessary:
             final int len2 = other.size();
             if (len != len2) {
             return false;
             }
             */
            for (int i = 0; i < len; i++) {
                if (get(i).shortValue() != other.get(i).shortValue()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Compare this station index with another station index
         * @param other another station index
         * @return comparison result
         */
        @Override
        public int compareTo(final StationIndex other) {
            int res = 0;
            for (int i = 0, len = size(); i < len; i++) {
                res = get(i).intValue() - other.get(i).intValue();
                if (res != 0) {
                    break;
                }
            }
            return res;
        }
    }
}
