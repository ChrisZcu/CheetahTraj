package swing;

import app.SharedObject;
import draw.TrajDrawManager;
import model.BlockType;
import model.TrajBlock;
import util.GBC;
import util.PSC;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Objects;

/**
 * The dialog that popup to select the data for specific map view.
 */
public class SelectDataDialog extends JDialog {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    private JLabel infoLabel;
    private JLabel typeLabel, rateLabel, deltaLabel;
    private JComboBox<BlockType> typeComboBox;
    private JComboBox<Double> rateComboBox;
    private JComboBox<Integer> deltaComboBox;
    private JButton cancelBtn, okBtn;

    private int optMapIdx = -1;

    public SelectDataDialog(Frame owner) {
        super(owner, "Select Data Set", false);

        setResizable(false);
        setAlwaysOnTop(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(owner);

        /* interface */

        JPanel mainPanel = new JPanel(new BorderLayout());
        initTopPart(mainPanel);
        initMainPart(mainPanel);
        initBtmPart(mainPanel);

        setContentPane(mainPanel);

        /* logic */

        initLogic();
    }

    /**
     * Design of the top part interface
     */
    private void initTopPart(JPanel mainPanel) {
        infoLabel = new JLabel();
        infoLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        mainPanel.add(infoLabel, BorderLayout.NORTH);
    }


    /**
     * Design of the center main part interface
     */
    private void initMainPart(JPanel mainPanel) {
        GridBagLayout gbLayout = new GridBagLayout();
        JPanel centerPanel = new JPanel(gbLayout);

        int cellWidth = (WIDTH - 100) / 2;
        int cellHeight = 30;

        typeLabel = new JLabel("Data Type: ");

        typeComboBox = new JComboBox<>(BlockType.values());
        centerPanel.add(typeLabel, new GBC(0, 0).setAnchor(GBC.WEST).setInsets(5, 10, 5, 5));
        centerPanel.add(typeComboBox, new GBC(1, 0).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        rateLabel = new JLabel("Sample Rate: ");
        rateComboBox = new JComboBox<>(Arrays.stream(PSC.RATE_LIST)
                .boxed().toArray(Double[]::new));
        centerPanel.add(rateLabel, new GBC(0, 1).setAnchor(GBC.WEST).setInsets(5, 10, 5, 5));
        centerPanel.add(rateComboBox, new GBC(1, 1).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        deltaLabel = new JLabel("Delta: ");
        deltaComboBox = new JComboBox<>(Arrays.stream(PSC.DELTA_LIST)
                .boxed().toArray(Integer[]::new));
        centerPanel.add(deltaLabel, new GBC(0, 2).setAnchor(GBC.WEST).setInsets(5, 10, 5, 5));
        centerPanel.add(deltaComboBox, new GBC(1, 2).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        // set one is ok for first col (label col)
        Dimension cellDim = new Dimension(cellWidth, cellHeight);
        typeLabel.setPreferredSize(cellDim);
        typeComboBox.setPreferredSize(cellDim);
        rateComboBox.setPreferredSize(cellDim);
        deltaComboBox.setPreferredSize(cellDim);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Design of the bottom part interface
     */
    private void initBtmPart(JPanel mainPanel) {
        Dimension btnDim = new Dimension(100, 30);

        Box bottomBox = Box.createHorizontalBox();
        cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(btnDim);
        okBtn = new JButton("OK");
        okBtn.setPreferredSize(btnDim);

        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(cancelBtn);
        bottomBox.add(Box.createHorizontalStrut(20));
        bottomBox.add(okBtn);
        bottomBox.add(Box.createHorizontalGlue());

        bottomBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(bottomBox, BorderLayout.SOUTH);
    }

    /**
     * Set the logic of the components.
     */
    private void initLogic() {
        // change visible settings when type is changed.
        typeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                BlockType toType = (BlockType) typeComboBox.getSelectedItem();
                setComponentVisible(toType);
            }
        });

        cancelBtn.addActionListener(e -> dispose());
        okBtn.addActionListener(e -> {
            updateBlockIfChanged();
            dispose();
        });
    }

    /**
     * Call this to get the dialog that shown the traj block data of current map.
     * But not to call {@link #setVisible(boolean)} directly.
     */
    public void showDialogFor(int mapIdx) {
        setDataInfo(mapIdx);
        setVisible(true);
    }

    /**
     * Set the current data to a specific {@link model.TrajBlock}.
     * Will be called when open it.
     *
     * @param mapIdx the idx to get the trajBlock.
     */
    public void setDataInfo(int mapIdx) {
        optMapIdx = mapIdx;
        infoLabel.setText("Choose the base data set for map " + mapIdx);

        TrajBlock tb = SharedObject.getInstance().getBlockList()[optMapIdx];
        BlockType toType = tb.getBlockType();
        typeComboBox.setSelectedItem(toType);
        setComponentVisible(toType);
    }

    /**
     * Set the visible property according to the block type
     * selected in the {@link #typeComboBox} now.
     *
     * @param blockType the selected blockType
     */
    private void setComponentVisible(BlockType blockType) {
        switch (Objects.requireNonNull(blockType)) {
            case NONE:
            case FULL:
                rateLabel.setEnabled(false);
                rateComboBox.setEnabled(false);
                deltaLabel.setEnabled(false);
                deltaComboBox.setEnabled(false);
                break;
            case VFGS:
                rateLabel.setEnabled(true);
                rateComboBox.setEnabled(true);
                deltaLabel.setEnabled(true);
                deltaComboBox.setEnabled(true);
                break;
            case RAND:
                rateLabel.setEnabled(true);
                rateComboBox.setEnabled(true);
                deltaLabel.setEnabled(false);
                deltaComboBox.setEnabled(false);
                break;
            default:
                break;
        }
    }

    private void updateBlockIfChanged() {
        TrajBlock tb = SharedObject.getInstance().getBlockList()[optMapIdx];
        BlockType newType = (BlockType) Objects.requireNonNull(typeComboBox.getSelectedItem());
        int newRIdx = rateComboBox.getSelectedIndex();
        int newDIdx = deltaComboBox.getSelectedIndex();
        if (tb.getBlockType().equals(newType)
                && tb.getRIdx() == newRIdx
                && tb.getDIdx() == newDIdx) {
            // not changed, do nothing.
            return;
        }
        SharedObject.getInstance().setBlockAt(optMapIdx, newType, newRIdx, newDIdx);

        boolean[] viewVisibleList = SharedObject.getInstance().getViewVisibleList();
        if (!viewVisibleList[optMapIdx]) {
            // no need to draw it right now
            return;
        }
        TrajDrawManager trajDrawManager = SharedObject.getInstance().getTrajDrawManager();
        trajDrawManager.cleanImgFor(optMapIdx, TrajDrawManager.MAIN);
        trajDrawManager.startNewRenderTaskFor(optMapIdx, TrajDrawManager.MAIN);
    }
}
