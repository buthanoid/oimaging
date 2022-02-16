/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oimaging.gui;

import fr.jmmc.jmcs.gui.component.GenericListModel;
import static fr.jmmc.jmcs.gui.util.ResourceImage.DOWN_ARROW;
import static fr.jmmc.jmcs.gui.util.ResourceImage.UP_ARROW;
import fr.jmmc.jmcs.util.SpecialChars;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.oimaging.services.software.MiraInputParam;
import fr.jmmc.oimaging.services.software.SparcoInputParam;
import fr.jmmc.oitools.fits.FitsTable;
import fr.jmmc.oitools.image.FitsUnit;
import fr.jmmc.oitools.meta.KeywordMeta;
import static fr.jmmc.oitools.meta.Types.TYPE_CHAR;
import static fr.jmmc.oitools.meta.Types.TYPE_DBL;
import static fr.jmmc.oitools.meta.Types.TYPE_INT;
import static fr.jmmc.oitools.meta.Types.TYPE_LOGICAL;
import fr.jmmc.oitools.meta.Units;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.SwingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class displays and edit table params using swing widgets.
 */
public final class TableKeywordsEditor extends javax.swing.JPanel implements ActionListener, PropertyChangeListener {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(TableKeywordsEditor.class.getName());

    private static final long serialVersionUID = 1L;

    private final static Insets INSETS = new Insets(2, 2, 2, 2);
    private final static Insets VERT_SPACE_INSETS = new Insets(20, 2, 2, 2);

    // members
    private SoftwareSettingsPanel notifiedParent = null;
    private FitsTable fitsTable = null;
    /** editor conversions */
    private final HashMap<String, FitsUnit> unitKeywords = new HashMap<String, FitsUnit>();
    private final HashMap<String, FitsUnit> unitFields = new HashMap<String, FitsUnit>();

    /**
     * state of the button show/hide for models (SPARCO only)
     */
    private final List<ToggleButtonModel> modelsShown = new ArrayList<>();
    /**
     * keywords components grouped by models (SPARCO only)
     */
    private final List<List<JComponent>> modelsComps = new ArrayList<>();

    /** Creates new form TableEditor */
    public TableKeywordsEditor() {
        initComponents();
    }

    public SoftwareSettingsPanel getNotifiedParent() {
        return notifiedParent;
    }

    public void setNotifiedParent(final SoftwareSettingsPanel notifiedParent) {
        this.notifiedParent = notifiedParent;
    }

    void setModel(final FitsTable fitsTable) {
        setModel(fitsTable, null);
    }

    void setModel(final FitsTable fitsTable, final Set<String> keywordNames) {
        this.fitsTable = fitsTable;

        removeAll();
        updateUI(); // to resolve paint glitch on macos

        // reset converters:
        this.unitKeywords.clear();
        this.unitFields.clear();

        if (fitsTable == null) {
            return;
        }

        int gridy = 0;
        setLayout(new GridBagLayout());

        // separating the sparco models keywords from the others
        final List<String> filteredKeywordNames = new ArrayList<>(keywordNames.size());
        final List<String> sparcoModelsKeywords = new ArrayList<>();
        for (String keywordName : keywordNames) {
            if ((keywordName.startsWith(SparcoInputParam.KEYWORD_MOD))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_FLU))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_SPEC))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_IDX))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_TEM))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_PAR))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_DEX))
                    || (keywordName.startsWith(SparcoInputParam.KEYWORD_DEY))) {
                sparcoModelsKeywords.add(keywordName);
            } else {
                filteredKeywordNames.add(keywordName);
            }
        }

        // treating the keywords (except the sparco models keywords)
        for (final String name : filteredKeywordNames) {

            // just before SMEAR_FN field, add a label line (mira & sparco)
            if (MiraInputParam.KEYWORD_SMEAR_FN.equals(name)) {
                addFormLabel("Bandwith smearing:", gridy, VERT_SPACE_INSETS);
                gridy++;
            }

            // insert vertical space above SWAVE0 field (sparco)
            Insets insets = SparcoInputParam.KEYWORD_SWAVE0.equals(name) ? VERT_SPACE_INSETS : INSETS;

            addFormKeyword(name, getLabel(name), gridy, insets);
            gridy++;
        }

        // ===== treating the sparco models keywords =====

        // group the keywords names by model
        List<List<String>> modelsKeywords = new ArrayList<>();
        {
            int numModel = -1; // set to -1 so first keyword will trigger a model creation in the loop
            String numModelStr = Integer.toString(numModel);
            for (String keywordName : sparcoModelsKeywords) {
                // new model
                // keywords are assumed ordered in increasing model number, with no gaps
                if (!keywordName.endsWith(numModelStr)) {
                    numModel++;
                    numModelStr = Integer.toString(numModel);
                    modelsKeywords.add(new ArrayList<>());
                }
                modelsKeywords.get(numModel).add(keywordName);
            }
        }

        // create components for each keyword
        modelsComps.clear();
        // for each model
        for (int numModel = 0, s = modelsKeywords.size(); numModel < s; numModel++) {

            modelsComps.add(new ArrayList<JComponent>());

            // create a ToggleButtonModel if there is no one existing yet, for each model
            if (numModel >= modelsShown.size()) {
                modelsShown.add(new ToggleButtonModel());
                boolean selected = (numModel == 0); // first model is always shown
                modelsShown.get(numModel).setPressed(selected);
                modelsShown.get(numModel).setSelected(selected);
            }

            // for each keyword, add the components, and register them in modelsComps
            for (String keywordName : modelsKeywords.get(numModel)) {

                boolean isMODKeyword = keywordName.startsWith(SparcoInputParam.KEYWORD_MOD);
                boolean isSPEC0Keyword = keywordName.equals(SparcoInputParam.KEYWORD_SPEC + "0");

                // add the keyword to the panel
                // MODn keywords and SEPC0 have vertical space above
                Insets insets = (isMODKeyword || isSPEC0Keyword) ? VERT_SPACE_INSETS : INSETS;
                List<JComponent> keywordComps = addFormKeyword(
                        keywordName, getLabel(keywordName), gridy, insets);
                gridy++;

                // only after the keyword MOD, we insert the button to show/hide the other keywords
                // except for the first model which does not have a button (its keywords are always shown)
                if (isMODKeyword && (numModel != 0)) {
                    addGroupButton(numModel, gridy);
                    gridy++;
                }

                // we register the components in modelsComps
                // except for keyword MOD's components, which are always shown
                if (!isMODKeyword) {
                    for (JComponent keywordComp : keywordComps) {
                        modelsComps.get(numModel).add(keywordComp);
                        keywordComp.setVisible(modelsShown.get(numModel).isSelected());
                    }
                }
            }
        }
    }

    /**
     * add a label line in the form at coord gridy.
     *
     * @param label label text
     * @param gridy y coord in the form where to add the label
     * @param insets insets for gridbagconstraints for jlabel
     */
    private void addFormLabel(final String label, final int gridy, final Insets insets) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = insets;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;

        final JLabel jLabel = new JLabel(label);

        add(jLabel, gridBagConstraints);
    }

    /**
     * add to the panel a button toggling show/hide for the keywords components of a model. (SPARCO only)
     * @param numModel the number of the model (index for this.modelsComps and this.modelsShown)
     * @param gridy the next gridbaglayout y coord available
     */
    private void addGroupButton(final int numModel, final int gridy) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = INSETS;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;

        final JToggleButton groupButton = new JToggleButton();

        groupButton.setModel(modelsShown.get(numModel));

        groupButton.setOpaque(false);
        groupButton.setContentAreaFilled(false);
        groupButton.setHorizontalAlignment(SwingConstants.LEFT);

        groupButton.setText(groupButton.isSelected() ? "hide keywords" : "show keywords");
        groupButton.setIcon(groupButton.isSelected() ? DOWN_ARROW.icon() : UP_ARROW.icon());

        groupButton.addItemListener((ItemEvent e) -> {
            modelsComps.get(numModel).forEach(jc -> jc.setVisible(groupButton.isSelected()));
            groupButton.setText(groupButton.isSelected() ? "hide keywords" : "show keywords");
            groupButton.setIcon(groupButton.isSelected() ? DOWN_ARROW.icon() : UP_ARROW.icon());
        });

        add(groupButton, gridBagConstraints);
    }

    /**
     * Add to the panel a field
     * @param name name of the field
     * @param gridy the next gridbaglayout y coord available
     * @param insets swing insets
     * @return
     */
    private List<JComponent> addFormKeyword(final String name, String label, final int gridy, Insets insets) {

        final KeywordMeta meta = fitsTable.getKeywordsDesc(name);
        final Object value = fitsTable.getKeywordValue(name);

        final JComponent component;
        boolean supportedKeyword = true;

        switch (meta.getDataType()) {
            case TYPE_CHAR:
                if (meta.getStringAcceptedValues() == null) {
                    component = new JTextField((value == null) ? "" : value.toString());
                } else {
                    JComboBox comboBox = new JComboBox(new GenericListModel(Arrays.asList(meta.getStringAcceptedValues()), true));
                    comboBox.setPrototypeDisplayValue("XXXX");
                    if (value != null) {
                        comboBox.setSelectedItem(value);
                    }
                    comboBox.setRenderer(new LabelListCellRenderer());
                    component = comboBox;
                }
                break;
            case TYPE_DBL:
                component = createFormattedTextField(SoftwareSettingsPanel.getDecimalFormatterFactory(),
                        convertValueToField(name, meta.getUnits(), value));
                break;
            case TYPE_INT:
                component = createFormattedTextField(SoftwareSettingsPanel.getIntegerFormatterFactory(), value);
                break;
            case TYPE_LOGICAL:
                final JCheckBox checkbox = new JCheckBox();
                checkbox.setSelected(Boolean.TRUE.equals(value));
                component = checkbox;
                break;
            default:
                component = new JTextField(meta.getDataType() + " UNSUPPORTED");
                supportedKeyword = false;
        }

        // show description in tooltips:
        String description = meta.getDescription();

        FitsUnit unit = this.unitFields.get(name);
        if (unit != null) {
            description = "<html>" + description + "<br/><b>Editor unit is '" + unit.getRepresentation() + "'</b></html>";
        } else {
            unit = this.unitKeywords.get(name);
        }

        // define label and optionally the unit:
        if (unit != null) {
            label += " [" + ((unit == FitsUnit.WAVELENGTH_MICRO_METER)
                    ? SpecialChars.UNIT_MICRO_METER : unit.getStandardRepresentation()) + ']';
        }
        final JLabel jLabel = new JLabel(label);
        jLabel.setToolTipText(description);
        component.setToolTipText(description);

        if (supportedKeyword) {
            // store name to retieve back on edit
            component.setName(name);
            component.addPropertyChangeListener("value", this);

            if (component instanceof JTextField) {
                ((JTextField) component).addActionListener(this);
            } else if (component instanceof JComboBox) {
                ((JComboBox) component).addActionListener(this);
            } else if (component instanceof JCheckBox) {
                ((JCheckBox) component).addActionListener(this);
            }
        } else {
            ((JTextField) component).setEditable(false);
        }

        GridBagConstraints gridBagConstraints;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = insets;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        add(jLabel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = insets;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;

        add(component, gridBagConstraints);

        return Arrays.asList(jLabel, component);
    }

    /**
     * return a label associated to a keyword name.
     * The labels are specific to this form: they are chosen to be short, and clear in this context.
     *
     * @param keywordName the keyword name for which we want a label.
     * @return the label. return the keywordName if no label found.
     */
    private static String getLabel(final String keywordName) {
        switch (keywordName) {
            // === mira parameters ===
            case MiraInputParam.KEYWORD_SMEAR_FN:
                return "Function";
            case MiraInputParam.KEYWORD_SMEAR_FC:
                return "Factor";
            // === sparco parameters ===
            case SparcoInputParam.KEYWORD_SWAVE0:
                return "Reference wavelength";
            case SparcoInputParam.KEYWORD_SNMODS:
                return "Number of models";
            // === sparco list items ===
            case SparcoInputParam.KEYWORD_SPEC_POW:
                return "power";
            case SparcoInputParam.KEYWORD_SPEC_BB:
                return "black body";
            case SparcoInputParam.KEYWORD_MODEL_STAR:
                return "star";
            case SparcoInputParam.KEYWORD_MODEL_UD:
                return "uniform disc";
            case SparcoInputParam.KEYWORD_MODEL_BG:
                return "background";
            default:
                break;
        }
        // in case of Sparco Model keyword, we must first remove the integer at the end of the keyword :
        {
            int startInteger = keywordName.length();
            while (startInteger > 0 && Character.isDigit(keywordName.charAt(startInteger - 1))) {
                startInteger--;
            }
            if (startInteger < keywordName.length()) {
                switch (keywordName.substring(0, startInteger)) {
                    case SparcoInputParam.KEYWORD_SPEC:
                        if ("0".equals(keywordName.substring(startInteger))) {
                            return "Image spectrum"; // special rule for keyword name SPEC0
                        } else {
                            return "Model spectrum";
                        }
                    case SparcoInputParam.KEYWORD_IDX:
                        return "Spectral index";
                    case SparcoInputParam.KEYWORD_TEM:
                        return "Temperature";
                    case SparcoInputParam.KEYWORD_MOD:
                        return "Model n°" + keywordName.substring(startInteger);
                    case SparcoInputParam.KEYWORD_PAR:
                        return "UD diameter";
                    case SparcoInputParam.KEYWORD_FLU:
                        return "Flux ratio";
                    case SparcoInputParam.KEYWORD_DEX:
                        return "RA shift";
                    case SparcoInputParam.KEYWORD_DEY:
                        return "DEC shift";
                }
            }
        }
        // else, return the keywordName as label
        return keywordName;
    }

    /**
     * Render labels instead of raw values.
     */
    private static class LabelListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList jList, Object value, int index, boolean selected, boolean focused
        ) {
            super.getListCellRendererComponent(jList, value, index, selected, focused);
            if (value instanceof String) {
                this.setText(getLabel((String) value));
            }
            return this;
        }
    }

    private static JFormattedTextField createFormattedTextField(final JFormattedTextField.AbstractFormatterFactory formatterFactory, final Object value) {
        final JFormattedTextField jFormattedTextField = new JFormattedTextField(value);
        jFormattedTextField.setFormatterFactory(formatterFactory);
        return jFormattedTextField;
    }

    /** 
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBorder(javax.swing.BorderFactory.createTitledBorder("Specific params"));
        setLayout(new java.awt.GridBagLayout());
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void actionPerformed(final ActionEvent ae) {
        final JComponent component = (JComponent) ae.getSource();
        if (component != null) {
            final String name = component.getName();
            String value = null;
            if (component instanceof JTextField) {
                value = ((JTextField) component).getText();
            } else if (component instanceof JCheckBox) {
                value = Boolean.toString(((JCheckBox) component).isSelected());
            } else if (component instanceof JComboBox) {
                value = (String) ((JComboBox) component).getSelectedItem();
            }
            update(name, value);
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent pce) {
        final JComponent component = (JComponent) pce.getSource();
        if (component != null) {
            final String name = component.getName();
            if (pce.getNewValue() != null) {
                update(name, pce.getNewValue().toString());
            }
        }
    }

    private void update(final String name, final String value) {
        // Store content as a string even for every types
        if (name != null && value != null) {
            // handle unit conversion:
            final FitsUnit unitField = this.unitFields.get(name);

            if (unitField != null) {
                fitsTable.setKeywordValue(name,
                        convertFieldToValue(name, unitField, value));
            } else {
                fitsTable.updateKeyword(name, value);
            }
            notifiedParent.updateModel(true);
        }
    }

    private Object convertValueToField(final String name, final Units unit, final Object value) {
        Object output = value;

        if (unit != Units.NO_UNIT) {
            try {
                // parse unit:
                final FitsUnit unitKeyword = FitsUnit.parseUnit(unit.getStandardRepresentation());

                if (unitKeyword != FitsUnit.NO_UNIT) {
                    this.unitKeywords.put(name, unitKeyword);

                    final FitsUnit unitField;
                    switch (unitKeyword) {
                        case WAVELENGTH_METER:
                            // implictely suppose wavelength argument:
                            unitField = FitsUnit.WAVELENGTH_MICRO_METER;
                            break;
                        case ANGLE_DEG:
                            unitField = FitsUnit.ANGLE_MILLI_ARCSEC;
                            break;
                        default:
                            // no conversion
                            unitField = null;
                    }

                    if (unitField != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Keyword[{}] unitKeyword: {}, unitField: {}", name, unitKeyword, unitField);
                        }
                        this.unitFields.put(name, unitField);

                        // perform conversion:
                        if (value instanceof Double) {
                            final double val = (Double) value;
                            final double converted = unitKeyword.convert(val, unitField);

                            if (logger.isDebugEnabled()) {
                                logger.debug("Keyword[{}] val: {}, converted: {}", name, val, converted);
                            }
                            output = Double.valueOf(converted);
                        }
                    }
                }
            } catch (IllegalArgumentException iae) {
                logger.info("convertValueToField: failure:", iae);
            }
        }
        return output;
    }

    private Double convertFieldToValue(final String name, final FitsUnit unitField, final String value) {
        Double output = null;

        final FitsUnit unitKeyword = this.unitKeywords.get(name);

        if (unitKeyword != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Keyword[{}] unitKeyword: {}, unitField: {}", name, unitKeyword, unitField);
            }

            if (!StringUtils.isEmpty(value)) {
                // perform conversion:
                final double val = Double.valueOf(value);
                final double converted = unitField.convert(val, unitKeyword);

                if (logger.isDebugEnabled()) {
                    logger.debug("Keyword[{}] val: {}, converted: {}", name, val, converted);
                }
                output = Double.valueOf(converted);
            }
        }
        return output;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
