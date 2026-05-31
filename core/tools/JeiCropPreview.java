import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * JEI crop preview tool -- iterates through all machines.
 *
 * Usage: ./gradlew :core:runCropPreview
 * Press LEFT/RIGHT arrow keys to switch machines.
 */
public class JeiCropPreview {

    // ===== Machine configs =====
    static final MachineConfig[] MACHINES = {
        new MachineConfig("Compressor",      "guicompressor.png",          30,5,  118,78,  new int[][]{{28,10},{86,29},{86,47}},  new int[][]{{58,15},{116,34},{58,56}}),
        new MachineConfig("Macerator",       "scrapboxrecipes.png",        26,5,  124,78,  new int[][]{{16,10},{90,30}},          new int[][]{{42,15},{116,35},{42,54}}),
        new MachineConfig("Extractor",       "guiextractor.png",           33,5,  112,78,  new int[][]{{29,11},{83,30}},          new int[][]{{62,16},{116,35},{62,55}}),
        new MachineConfig("Recycler",        "guirecycler.png",            30,5,  120,78,  new int[][]{{22,11},{81,30}},          new int[][]{{52,16},{111,35},{52,54}}),
        new MachineConfig("BlockCutter",     "guiblockcuttingmachine.png", 10,5,  130,78,  new int[][]{{16,12},{106,30}},         new int[][]{{26,17},{116,35},{26,53},{70,53}}),
        new MachineConfig("Centrifuge",      "guicentrifuge.png",          0,11,  150,72,  new int[][]{{11,10},{124,7},{124,25},{124,43}}, new int[][]{{11,21},{124,18},{124,36},{124,54},{11,60}}),
        new MachineConfig("MetalFormer",     "guimetalformer.png",         4,5,   146,78,  new int[][]{{13,12},{112,30}},         new int[][]{{17,17},{116,35},{17,53}}),
        new MachineConfig("OreWashing",      "guiorewashingplant.png",     4,6,   144,75,  new int[][]{{100,10},{82,55},{100,55},{118,55}}, new int[][]{{104,16},{86,61},{104,61},{122,61}}),
        new MachineConfig("BlastFurnace",    "guiblockcutter.png",         0,0,   170,80,  new int[][]{{35,33},{134,56},{152,56}},  new int[][]{{35,33},{134,56},{152,56},{9,63}}),
        new MachineConfig("SolidCanner",     "guisolidcanner.png",         25,21, 112,45,  new int[][]{{4,14},{36,14},{92,14}},    new int[][]{{29,35},{61,35},{117,35},{8,62}}),
        new MachineConfig("CannerMixing",    "guicanner.png",              33,8,  104,90,  new int[][]{{8,8},{47,35},{86,8}},      new int[][]{{41,16},{80,43},{119,16},{43,46},{121,46}}),
    };

    static final String TEX_DIR = "core/src/main/resources/assets/ic2/textures/gui/";
    static final int SLOT_SIZE = 18;

    static class MachineConfig {
        final String name, texFile;
        final int cu, cv, cw, ch;
        final int[][] jeiSlots;  // crop coords
        final int[][] machineSlots;  // tex coords
        MachineConfig(String name, String texFile, int cu, int cv, int cw, int ch, int[][] jeiSlots, int[][] machineSlots) {
            this.name = name; this.texFile = texFile; this.cu=cu; this.cv=cv; this.cw=cw; this.ch=ch;
            this.jeiSlots = jeiSlots; this.machineSlots = machineSlots;
        }
    }

    record JeiSlot(String name, int cx, int cy) {}

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("JEI Crop Preview");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setIconImage(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB));

            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);
            JScrollPane scroll = new JScrollPane(label);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            frame.add(scroll);

            JLabel info = new JLabel(" ", SwingConstants.CENTER);
            info.setFont(new Font("Monospaced", Font.PLAIN, 12));
            frame.add(info, BorderLayout.SOUTH);

            int[] idx = {0};

            Runnable show = () -> {
                MachineConfig m = MACHINES[idx[0]];
                try {
                    BufferedImage result = renderMachine(m);
                    label.setIcon(new ImageIcon(result));
                    Dimension d = new Dimension(Math.min(result.getWidth()+16, 1200), Math.min(result.getHeight()+40, 900));
                    frame.setPreferredSize(d);
                    frame.pack();
                    frame.setLocationRelativeTo(null);

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%s — Crop(%d,%d) %dx%d  |  JEI:", m.name, m.cu, m.cv, m.cw, m.ch));
                    for (int[] s : m.jeiSlots) sb.append(String.format(" (%d,%d)", s[0], s[1]));
                    info.setText(sb.toString());
                } catch (Exception e) {
                    info.setText("Error: " + e.getMessage());
                }
            };

            frame.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        idx[0] = (idx[0] - 1 + MACHINES.length) % MACHINES.length;
                        show.run();
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        idx[0] = (idx[0] + 1) % MACHINES.length;
                        show.run();
                    }
                }
            });
            frame.setFocusable(true);

            show.run();
            frame.setVisible(true);

            System.out.println("Arrow LEFT/RIGHT to switch machines");
        });
    }

    static BufferedImage renderMachine(MachineConfig m) throws Exception {
        File f = new File(TEX_DIR + m.texFile);
        if (!f.exists()) throw new RuntimeException("Missing: " + f);
        BufferedImage tex = ImageIO.read(f);
        int texW = tex.getWidth(), texH = tex.getHeight();

        // Panel A: full texture with annotations
        BufferedImage full = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = full.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(tex, 0, 0, null);

        // Dim outside
        g.setColor(new Color(0,0,0,100));
        g.fillRect(0,0,m.cu,texH); g.fillRect(m.cu+m.cw,0,texW-m.cu-m.cw,texH);
        g.fillRect(m.cu,0,m.cw,m.cv); g.fillRect(m.cu,m.cv+m.ch,m.cw,texH-m.cv-m.ch);

        // Crop box
        g.setColor(new Color(255,30,30)); g.setStroke(new BasicStroke(2));
        g.drawRect(m.cu, m.cv, m.cw, m.ch);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        drawLabel(g, String.format("Crop (%d,%d) %dx%d", m.cu,m.cv,m.cw,m.ch), m.cu+4, m.cv-6, Color.RED);

        // Machine slot outlines (blue dotted)
        g.setColor(new Color(60,120,255,160));
        g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{4},0));
        for (int[] s : m.machineSlots) g.drawRect(s[0]-1, s[1]-1, SLOT_SIZE, SLOT_SIZE);

        // JEI slot markers
        g.setStroke(new BasicStroke(1));
        for (int[] s : m.jeiSlots) {
            int tx = m.cu + s[0] + 8, ty = m.cv + s[1] + 8;
            g.setColor(new Color(0,200,50,100)); g.fillOval(tx-6,ty-6,12,12);
            g.setColor(new Color(0,220,60)); g.setStroke(new BasicStroke(2)); g.drawOval(tx-6,ty-6,12,12);
        }
        g.dispose();

        // Panel B: cropped view
        BufferedImage crop = tex.getSubimage(m.cu, m.cv, m.cw, m.ch);
        BufferedImage cropV = new BufferedImage(m.cw, m.ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = cropV.createGraphics();
        cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cg.drawImage(crop, 0, 0, null);
        for (int[] s : m.jeiSlots) {
            int cx = s[0]+8, cy = s[1]+8;
            cg.setColor(new Color(0,200,50,120)); cg.fillOval(cx-6,cy-6,12,12);
            cg.setColor(new Color(0,220,60)); cg.setStroke(new BasicStroke(2)); cg.drawOval(cx-6,cy-6,12,12);
        }
        cg.dispose();

        // Composite
        int gap = 20;
        BufferedImage result = new BufferedImage(Math.max(texW, m.cw), texH+gap+m.ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fg = result.createGraphics();
        fg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fg.setColor(new Color(45,45,45)); fg.fillRect(0,0,result.getWidth(),result.getHeight());
        fg.drawImage(full, (result.getWidth()-texW)/2, 0, null);
        fg.drawImage(cropV, (result.getWidth()-m.cw)/2, texH+gap, null);
        fg.setColor(Color.WHITE); fg.setFont(new Font("Monospaced", Font.BOLD, 13));
        fg.drawString("[ Full texture ]", (result.getWidth()-texW)/2, texH+14);
        fg.drawString("[ JEI cropped view ]", (result.getWidth()-m.cw)/2, texH+gap+m.ch+14);
        fg.dispose();
        return result;
    }

    static void drawLabel(Graphics2D g, String t, int x, int y, Color c) {
        g.setColor(new Color(40,40,40,200));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(t);
        g.fillRect(x-2, y-fm.getAscent()+2, tw+4, fm.getHeight());
        g.setColor(c); g.drawString(t, x, y);
    }
}
