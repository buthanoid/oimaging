/**
 * *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 * ****************************************************************************
 */
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.oiexplorer.core.export.DocumentExportable;
import fr.jmmc.oiexplorer.core.export.DocumentOptions;
import fr.jmmc.oiexplorer.core.gui.action.ExportDocumentAction;
import fr.jmmc.oiexplorer.core.gui.chart.ChartUtils;
import fr.jmmc.oiexplorer.core.util.Constants;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.ui.Drawable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plot view implementation
 *
 * @author Jean-Philippe GROS.
 */
public final class GlobalView extends javax.swing.JPanel implements DocumentExportable {

    /**
     * default serial UID for Serializable interface
     */
    private static final long serialVersionUID = 1;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(GlobalView.class.getName());
    
    /* members */
    /** List of charts */
    private final List<ChartPanel> chartPanelList;

    /**
     * Creates new form PlotView
     */
    public GlobalView() {
        chartPanelList = new ArrayList<ChartPanel>();

        // Build GUI
        initComponents();
    }

    public int getChartCount() {
        return chartPanelList.size();
    }

    public void addChart(final JFreeChart chart, final Overlay overlay) {
        logger.debug("addChart: {}", chart);

        final ChartPanel chartPanel = ChartUtils.createChartPanel(chart, false);

        // enable mouse wheel:
        chartPanel.setMouseWheelEnabled(true);

        if (overlay != null) {
            chartPanel.addOverlay(overlay);
        }
        // TODO: support mouse listener:
        // chartPanel.addChartMouseListener(this);

        chartPanelList.add(chartPanel);
        refreshJPanel();
    }

    public void removeChart(JFreeChart chart) {
        logger.debug("removeChart: {}", chart);

        for (int i = 0; i < chartPanelList.size(); i++) {
            final ChartPanel chartPanel = chartPanelList.get(i);
            if (chart == chartPanel.getChart()) {
                chartPanelList.remove(i);
                refreshJPanel();
                break;
            }
        }
    }

    private void refreshJPanel() {
        final int nCharts = chartPanelList.size();

        this.removeAll();

        int nCols = (nCharts == 1) ? 1 : 2;
        ((GridLayout) this.getLayout()).setColumns(nCols);

        for (int i = 0; i < nCharts; i++) {
            this.add(chartPanelList.get(i));
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.GridLayout(0, 2));
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    /**
     * Export the chart component as a PDF document.
     * @param action export action to perform the export action
     */
    @Override
    public void performAction(final ExportDocumentAction action) {
        if (!chartPanelList.isEmpty()) {
            action.process(this);
        }
    }

    /**
     * Return the PDF default file name
     *
     * @param fileExtension  document's file extension
     * @return PDF default file name
     */
    @Override
    public String getDefaultFileName(final String fileExtension) {
        return null;
    }

    /**
     * Prepare the page layout before doing the export:
     * Performs layout and modifies the given options
     * @param options document options used to prepare the document
     */
    @Override
    public void prepareExport(final DocumentOptions options) {
        options.setNormalDefaults();
    }

    /**
     * Return the page to export given its page index
     * @param pageIndex page index (1..n)
     * @return Drawable array to export on this page
     */
    @Override
    public Drawable[] preparePage(final int pageIndex) {
        final int nCharts = chartPanelList.size();
        final JFreeChart[] charts = new JFreeChart[nCharts];

        for (int i = 0; i < nCharts; i++) {
            charts[i] = chartPanelList.get(i).getChart();
        }
        return charts;
    }

    /**
     * Callback indicating the PDF document is done to reset the component's state
     */
    @Override
    public void postExport() {
        // no-op
    }
}
